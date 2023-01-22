package com.briefbytes.news.seed;

import com.briefbytes.news.downloader.Downloader;
import com.briefbytes.news.model.ContentFormat;
import com.briefbytes.news.model.News;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.*;

public class HackerNews extends Seed {

    private static final Logger LOG = LoggerFactory.getLogger(HackerNews.class);

    private String newsEndpoint;
    private Downloader downloader;
    private int maxItems;

    public HackerNews(Downloader downloader) {
        this("hn/hot", "https://news.ycombinator.com",
                "https://hacker-news.firebaseio.com/v0/topstories.json",
                downloader,
                MAX_ITEMS_DEFAULT);
    }
    public HackerNews(String seedName, String seedUrl, String newsEndpoint, Downloader downloader, int maxItems) {
        super(seedName, seedUrl);
        this.newsEndpoint = newsEndpoint;
        this.downloader = downloader;
        this.maxItems = maxItems;
    }

    private News getStory(long id) {
        try {
            var url = new URL("https://hacker-news.firebaseio.com/v0/item/" + id + ".json");
            var json = new ObjectMapper().readTree(downloader.download(url, false));

            // discard job ads
            if (!"story".equals(json.get("type").asText())) {
                return null;
            }

            var commentsUrl = "https://news.ycombinator.com/item?id=" + id;
            var newsUrl = json.get("url");
            var builder = new News.NewsBuilder()
                    .withId(getSeedName() + "-" + id)
                    .withSeed(getSeedName())
                    .withTitle(json.get("title").asText())
                    .withCommentsUrl(commentsUrl)
                    .withDate(new Date(json.get("time").asLong() * 1000))
                    .withScore(json.get("score").asInt())
                    .withCommentsCount(json.get("descendants").asInt())
                    .withStoryUrl(newsUrl == null ? commentsUrl : newsUrl.asText());

            var text = json.get("text");
            if (text == null) {
                builder.withContentFormat(ContentFormat.HTML);
            } else {
                builder.withContentFormat(ContentFormat.TEXT);
                builder.withContent(text.asText());
            }
            return builder.build();
        } catch (Exception e) {
            LOG.error("Unable to fetch hacker news story {}: {}", id, e);
            return null;
        }
    }

    @Override
    public List<News> poll() throws IOException {
        List<Long> items = new ObjectMapper().readValue(new URL(newsEndpoint), new TypeReference<>() {
        });
        // do not parallelize requests as API is fast and don't want to overload them
        return items.stream()
                .filter(Objects::nonNull)
                .limit(maxItems)
                .map(this::getStory)
                .toList();
    }
}
