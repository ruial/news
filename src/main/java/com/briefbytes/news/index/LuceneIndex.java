package com.briefbytes.news.index;

import com.briefbytes.news.model.ContentFormat;
import com.briefbytes.news.model.News;
import com.briefbytes.news.utils.CloseableUtils;
import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.charfilter.HTMLStripCharFilter;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.en.EnglishPossessiveFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.mlt.MoreLikeThis;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.util.*;

public class LuceneIndex implements Index {

    private static final Logger LOG = LoggerFactory.getLogger(LuceneIndex.class);
    private final Analyzer analyzer;
    private final IndexWriter indexWriter;
    private final SearcherManager searcherManager;
    private final ControlledRealTimeReopenThread<IndexSearcher> reopenThread;

    public LuceneIndex(Analyzer analyzer, IndexWriter indexWriter, SearcherManager searcherManager,
                       ControlledRealTimeReopenThread<IndexSearcher> reopenThread) {
        if (analyzer == null || indexWriter == null || searcherManager == null || reopenThread == null) {
            throw new NullPointerException("LuceneIndex arguments cannot be null");
        }
        this.analyzer = analyzer;
        this.indexWriter = indexWriter;
        this.searcherManager = searcherManager;
        this.reopenThread = reopenThread;
    }

    private News docToNews(Document doc, boolean includeContent) {
        var builder = new News.NewsBuilder()
                .withId(doc.get(FIELD_ID))
                .withSeed(doc.get(FIELD_SEED))
                .withStoryUrl(doc.get(FIELD_STORY_URL))
                .withTitle(doc.get(FIELD_TITLE))
                .withCommentsUrl(doc.get(FIELD_COMMENTS_URL))
                .withCommentsCount(doc.getField(FIELD_COMMENTS_COUNT).numericValue().intValue())
                .withScore(doc.getField(FIELD_SCORE).numericValue().intValue())
                .withDate(new Date(doc.getField(FIELD_DATE).numericValue().longValue()))
                .withContentFormat(ContentFormat.fromInteger(doc.getField(FIELD_CONTENT_FORMAT).numericValue().intValue()));
        if (includeContent) builder.withContent(doc.get(FIELD_CONTENT));
        return builder.build();
    }

    private Document newsToDoc(News news) {
        Document doc = new Document();
        // For text:
        // StringField - keyword only
        // TextField - full text analysis
        // For numbers:
        // LongPoint - range queries
        // NumericDocValuesField - sorting
        // StoredField - store the actual value
        doc.add(new StringField(FIELD_ID, news.getId(), Field.Store.YES));
        doc.add(new StringField(FIELD_SEED, news.getSeed(), Field.Store.YES));
        doc.add(new StringField(FIELD_STORY_URL, news.getStoryUrl(), Field.Store.YES));
        doc.add(new TextField(FIELD_TITLE, news.getTitle(), Field.Store.YES));
        doc.add(new StoredField(FIELD_COMMENTS_COUNT, news.getCommentsCount()));
        doc.add(new StoredField(FIELD_SCORE, news.getScore()));
        long timestamp = news.getDate().getTime();
        doc.add(new LongPoint(FIELD_DATE, timestamp));
        doc.add(new NumericDocValuesField(FIELD_DATE, timestamp));
        doc.add(new StoredField(FIELD_DATE, timestamp));
        doc.add(new StoredField(FIELD_CONTENT_FORMAT, news.getContentFormat().ordinal()));
        if (news.getCommentsUrl() != null) doc.add(new StringField(FIELD_COMMENTS_URL, news.getCommentsUrl(), Field.Store.YES));
        if (news.getContent() != null) doc.add(new TextField(FIELD_CONTENT, news.getContent(), Field.Store.YES));
        return doc;
    }

    @Override
    public List<News> latestNews(String seed, int count, Date start, Date end) throws IOException {
        if (seed == null || start == null || end == null || count <= 0) {
            return new ArrayList<>();
        }
        IndexSearcher searcher = searcherManager.acquire();
        try {
            Sort sort = new Sort(new SortedNumericSortField(FIELD_DATE, SortField.Type.LONG, true));
            // Query all = new MatchAllDocsQuery();
            Query seedQuery = new TermQuery(new Term(FIELD_SEED, seed));
            Query dateRange = LongPoint.newRangeQuery(FIELD_DATE, start.getTime(), end.getTime());
            Query query = new BooleanQuery.Builder()
                    .add(seedQuery, BooleanClause.Occur.FILTER)
                    .add(dateRange, BooleanClause.Occur.FILTER)
                    .build();
            TopDocs docs = searcher.search(query, count, sort);
            List<News> results = new ArrayList<>(count);
            for (var scoreDoc : docs.scoreDocs) {
                results.add(docToNews(searcher.doc(scoreDoc.doc), false));
            }
            return results;
        } finally {
            searcherManager.release(searcher);
        }
    }

    @Override
    public List<NewsSearchResult> similarNews(List<String> newsIds, int count, Date start, Date end) throws IOException {
        if (newsIds.isEmpty() || start == null || end == null || count <= 0) {
            return new ArrayList<>();
        }
        IndexSearcher searcher = searcherManager.acquire();
        try {
            BooleanQuery.Builder queryBuilderDocs = new BooleanQuery.Builder();
            BooleanQuery.Builder queryBuilderSimilar = new BooleanQuery.Builder();
            for (String id : newsIds) {
                if (id != null) {
                    Query term = new TermQuery(new Term(FIELD_ID, id));
                    queryBuilderDocs.add(term, BooleanClause.Occur.SHOULD);
                    queryBuilderSimilar.add(term, BooleanClause.Occur.MUST_NOT);
                }
            }
            HashMap<String, Collection<Object>> fieldValues = new HashMap<>();
            fieldValues.put(FIELD_TITLE, new ArrayList<>(newsIds.size()));
            fieldValues.put(FIELD_CONTENT, new ArrayList<>(newsIds.size()));
            for (var scoreDoc : searcher.search(queryBuilderDocs.build(), newsIds.size()).scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                fieldValues.get(FIELD_TITLE).add(doc.get(FIELD_TITLE));
                fieldValues.get(FIELD_CONTENT).add(doc.get(FIELD_CONTENT));
            }
            MoreLikeThis mlt = new MoreLikeThis(searcher.getIndexReader());
            mlt.setFieldNames(FIELDS_SEARCH);
            mlt.setAnalyzer(analyzer);
            // expensive operation as it calculates term vectors on the fly for multiple documents (enable on FieldType)
            // could index them, but they take more disk space and only work on like query for a single document
            Query mltQuery = mlt.like(fieldValues);
            queryBuilderSimilar.add(mltQuery, BooleanClause.Occur.MUST);
            Query dateRange = LongPoint.newRangeQuery(FIELD_DATE, start.getTime(), end.getTime());
            queryBuilderSimilar.add(dateRange, BooleanClause.Occur.FILTER);

            // elasticsearch also does a queryBuilderSimilar.setMinimumNumberShouldMatch() to 30% by default
            TopDocs docs = searcher.search(queryBuilderSimilar.build(), count);
            List<NewsSearchResult> results = new ArrayList<>(count);
            for (var scoreDoc : docs.scoreDocs) {
                results.add(new NewsSearchResult(docToNews(searcher.doc(scoreDoc.doc), false), scoreDoc.score));
            }
            return results;
        } finally {
            searcherManager.release(searcher);
        }
    }

    @Override
    public List<NewsSearchResult> searchNews(String query, int count, Date start, Date end) throws IOException {
        if (query == null || start == null || end == null || count <= 0) {
            return new ArrayList<>();
        }
        IndexSearcher searcher = searcherManager.acquire();
        try {
            StandardQueryParser queryParser = new StandardQueryParser();
            queryParser.setAnalyzer(analyzer);
            queryParser.setMultiFields(FIELDS_SEARCH);
            queryParser.setFieldsBoost(Map.of(
                    FIELD_TITLE, FIELD_BOOST_TITLE,
                    FIELD_CONTENT, FIELD_BOOST_CONTENT
            ));
            // escaping special characters but still allowing operators AND OR NOT
            Query textQuery = queryParser.parse(QueryParserBase.escape(query), null);
            Query dateRange = LongPoint.newRangeQuery(FIELD_DATE, start.getTime(), end.getTime());
            Query booleanQuery = new BooleanQuery.Builder()
                    .add(textQuery, BooleanClause.Occur.MUST)
                    .add(dateRange, BooleanClause.Occur.FILTER)
                    .build();

            TopDocs docs = searcher.search(booleanQuery, count);
            List<NewsSearchResult> results = new ArrayList<>(count);
            for (var scoreDoc : docs.scoreDocs) {
                // Explanation explanation = searcher.explain(booleanQuery, scoreDoc.doc);
                results.add(new NewsSearchResult(docToNews(searcher.doc(scoreDoc.doc), false), scoreDoc.score));
            }
            return results;
        } catch (QueryNodeException e) {
            LOG.warn("Invalid query: " + query, e.getMessage());
        } finally {
            searcherManager.release(searcher);
        }
        return new ArrayList<>();
    }

    @Override
    public News getNewsById(String id) throws IOException {
        if (id == null) {
            return null;
        }
        Query query = new TermQuery(new Term(FIELD_ID, id));
        IndexSearcher searcher = searcherManager.acquire();
        try {
            TopDocs docs = searcher.search(query, 1);
            if (docs.totalHits.value > 0) {
                return docToNews(searcher.doc(docs.scoreDocs[0].doc), true);
            }
        } finally {
            searcherManager.release(searcher);
        }
        return null;
    }

    @Override
    public String getContent(String storyUrl) throws IOException {
        if (storyUrl == null) {
            return null;
        }
        Query query = new TermQuery(new Term(FIELD_STORY_URL, storyUrl));
        IndexSearcher searcher = searcherManager.acquire();
        try {
            TopDocs docs = searcher.search(query, 1);
            if (docs.totalHits.value > 0) {
                Document doc = searcher.doc(docs.scoreDocs[0].doc);
                return doc.get(FIELD_CONTENT);
            }
        } finally {
            searcherManager.release(searcher);
        }
        return null;
    }
    @Override
    public void saveNews(News news) throws IOException {
        if (news != null) {
            Document doc = newsToDoc(news);
            // update in lucene is like delete followed by insert
            indexWriter.updateDocument(new Term(FIELD_ID, news.getId()), doc);
            // if I was not using the re-opener thread, I could refresh manually so the reader can see the latest writes
            // searcherManager.maybeRefresh();
        }
    }

    @Override
    public void deleteNews(String id) throws IOException {
        if (id != null) {
            indexWriter.deleteDocuments(new Term(FIELD_ID, id));
        }
    }

    @Override
    public void deleteOldNews(Date date) throws IOException {
        Query query = LongPoint.newRangeQuery(FIELD_DATE, 0, date.getTime());
        indexWriter.deleteDocuments(query);
    }

    @Override
    public long countNews() throws IOException {
        IndexSearcher searcher = searcherManager.acquire();
        try {
            return searcher.getIndexReader().numDocs();
        }
        finally {
            searcherManager.release(searcher);
        }
    }

    @Override
    public void close() {
        // indexWriter close already calls commit by default, based on
        // https://github.com/Stratio/cassandra-lucene-index/blob/branch-3.0.14/plugin/src/main/scala/com/stratio/cassandra/lucene/index/FSIndex.scala
        CloseableUtils.closeQuietly(reopenThread, searcherManager, indexWriter, indexWriter.getDirectory());
        // could also collect prometheus stats similar to
        // https://github.com/apache/geode/blob/develop/geode-lucene/src/main/java/org/apache/geode/cache/lucene/internal/repository/IndexRepositoryImpl.java
    }

    @Override
    public void commit() throws IOException {
        // need to commit lucene updates to disk, expensive operation
        indexWriter.commit();
    }

    public void refresh() throws IOException {
        searcherManager.maybeRefresh();
    }

    public static class NewsAnalyzer extends StopwordAnalyzerBase {

        public static final CharArraySet STOP_WORDS_SET = EnglishAnalyzer.ENGLISH_STOP_WORDS_SET;

        public NewsAnalyzer() {
            this(STOP_WORDS_SET);
        }

        public NewsAnalyzer(CharArraySet stopWords) {
            super(stopWords);
        }


        @Override
        protected TokenStreamComponents createComponents(String fieldName) {
            final Tokenizer source = new StandardTokenizer();
            TokenStream result = new EnglishPossessiveFilter(source);
            result = new LowerCaseFilter(result);
            result = new StopFilter(result, stopwords);
            return new TokenStreamComponents(source, result);
        }

        @Override
        protected TokenStream normalize(String fieldName, TokenStream in) {
            return new LowerCaseFilter(in);
        }

        @Override
        protected Reader initReader(String fieldName, Reader reader) {
            if (fieldName.equals(FIELD_CONTENT)) {
                return new HTMLStripCharFilter(super.initReader(fieldName, reader));
            }
            return super.initReader(fieldName, reader);
        }
    }
}
