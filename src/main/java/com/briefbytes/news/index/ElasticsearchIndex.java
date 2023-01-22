package com.briefbytes.news.index;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Conflicts;
import co.elastic.clients.elasticsearch._types.FieldSort;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.transport.ElasticsearchTransport;
import com.briefbytes.news.model.News;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ElasticsearchIndex implements Index {

    private String indexName;
    private ElasticsearchTransport transport;
    private ElasticsearchClient client;

    public ElasticsearchIndex(String indexName, ElasticsearchTransport transport, ElasticsearchClient client) {
        if (indexName == null || transport == null || client == null) {
            throw new NullPointerException("ElasticsearchIndex arguments cannot be null");
        }
        this.indexName = indexName;
        this.transport = transport;
        this.client = client;
    }

    @Override
    public List<News> latestNews(String seed, int count, Date start, Date end) throws IOException {
        if (seed == null || start == null || end == null || count <= 0) {
            return new ArrayList<>();
        }
        var response = client.search(s -> s
                        .index(indexName)
                        .query(q -> q
                                .bool(b -> b
                                        .filter(f -> f
                                                .range(r -> r
                                                        .field(FIELD_DATE)
                                                        .gte(JsonData.of(start))
                                                        .lte(JsonData.of(end))
                                                )
                                        )
                                        .filter(f -> f
                                                .match(m -> m
                                                        .field(FIELD_SEED)
                                                        .query(seed)
                                                )
                                        )
                                )
                        )
                        .sort(SortOptions.of(o -> o.field(FieldSort.of(f -> f.field(FIELD_DATE).order(SortOrder.Desc)))))
                        .size(count),
                News.class

        );
        return response.hits().hits().stream().map(h -> h.source()).toList();
    }

    @Override
    public List<NewsSearchResult> similarNews(List<String> newsIds, int count, Date start, Date end) throws IOException {
        if (newsIds.isEmpty() || start == null || end == null || count <= 0) {
            return new ArrayList<>();
        }
        var likes = newsIds.stream().map(id -> {
            var likeDocument = new LikeDocument.Builder().index(indexName).id(id).build();
            return new Like.Builder().document(likeDocument).build();
        }).toList();
        var response = client.search(s -> s
                        .index(indexName)
                        .query(q -> q
                                .bool(b -> b
                                        .filter(f -> f
                                                .range(r -> r
                                                        .field(FIELD_DATE)
                                                        .gte(JsonData.of(start))
                                                        .lte(JsonData.of(end))
                                                )
                                        )
                                        .must(f -> f
                                                .moreLikeThis(m -> m
                                                        .fields(List.of(FIELDS_SEARCH))
                                                        .like(likes)
                                                )
                                        )
                                )
                        )
                        .size(count),
                News.class

        );
        return response.hits().hits().stream().map(this::toRankedNews).toList();
    }

    private String getBoost(String field, float boost) {
        return field + "^" + boost;
    }

    private NewsSearchResult toRankedNews(Hit<News> hit) {
        return new NewsSearchResult(hit.source(), hit.score());
    }

    @Override
    public List<NewsSearchResult> searchNews(String query, int count, Date start, Date end) throws IOException {
        if (query == null || start == null || end == null || count <= 0) {
            return new ArrayList<>();
        }
        var response = client.search(s -> s
                        .index(indexName)
                        .query(q -> q
                                .bool(b -> b
                                        .filter(f -> f
                                                .range(r -> r
                                                        .field(FIELD_DATE)
                                                        .gte(JsonData.of(start))
                                                        .lte(JsonData.of(end))
                                                )
                                        )
                                        .must(f -> f
                                                .multiMatch(m -> m
                                                        .fields(
                                                                getBoost(FIELD_TITLE, FIELD_BOOST_TITLE),
                                                                getBoost(FIELD_CONTENT, FIELD_BOOST_CONTENT)
                                                        )
                                                        .query(query)
                                                )
                                        )
                                )
                        )
                        .size(count),
                News.class

        );
        return response.hits().hits().stream().map(this::toRankedNews).toList();
    }

    @Override
    public News getNewsById(String id) throws IOException {
        if (id == null) {
            return null;
        }
        return client.get(g -> g
                        .index(indexName)
                        .id(id),
                News.class
        ).source();
    }

    @Override
    public String getContent(String storyUrl) throws IOException {
        if (storyUrl == null) {
            return null;
        }
        var response = client.search(s -> s
                        .index(indexName)
                        .query(q -> q
                                .bool(b -> b
                                        .filter(f -> f
                                                .match(m -> m
                                                        .field(FIELD_STORY_URL)
                                                        .query(storyUrl)))
                                )
                        )
                        .size(1),
                News.class

        );
        return response.hits().hits().stream().findFirst().map(h -> h.source().getContent()).orElse(null);
    }

    @Override
    public void saveNews(News news) throws IOException {
        if (news != null) {
            client.index(i -> i
                    .index(indexName)
                    .id(news.getId())
                    .document(news)
            );
        }
    }

    @Override
    public void deleteNews(String id) throws IOException {
        if (id != null) {
            client.delete(d -> d
                    .index(indexName)
                    .id(id)
            );
        }
    }

    @Override
    public void deleteOldNews(Date date) throws IOException {
        // may get a conflict if index asynchronously with old docs (could set .refresh on save)
        client.deleteByQuery(d -> d
                .index(indexName)
                .conflicts(Conflicts.Proceed)
                .query(q -> q
                        .range(r -> r
                                .field(FIELD_DATE)
                                .lte(JsonData.of(date))
                        )));
    }

    @Override
    public long countNews() throws IOException {
        return client.count(c -> c.index(indexName)).count();
    }

    @Override
    public void close() throws IOException {
        transport.close();
    }
}
