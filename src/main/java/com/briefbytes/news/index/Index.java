package com.briefbytes.news.index;

import com.briefbytes.news.model.News;

import java.io.Closeable;
import java.io.IOException;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

public interface Index extends Closeable {
    List<News> latestNews(String seed, int count, Date start, Date end) throws IOException;
    List<NewsSearchResult> similarNews(List<String> newsIds, int count, Date start, Date end) throws IOException;
    List<NewsSearchResult> searchNews(String query, int count, Date start, Date end) throws IOException;
    News getNewsById(String id) throws IOException;
    String getContent(String storyUrl) throws IOException;
    void saveNews(News news) throws IOException;
    void deleteNews (String id) throws IOException;
    void deleteOldNews(Date date) throws IOException;
    long countNews() throws IOException;
    default void commit() throws IOException {}

    String FIELD_ID = "id";
    String FIELD_SEED = "seed";
    String FIELD_STORY_URL = "storyUrl";
    String FIELD_TITLE = "title";
    String FIELD_COMMENTS_URL = "commentsUrl";
    String FIELD_COMMENTS_COUNT = "commentsCount";
    String FIELD_SCORE = "score";
    String FIELD_DATE = "date";
    String FIELD_CONTENT_FORMAT = "contentFormat";
    String FIELD_CONTENT = "content";
    String[] FIELDS_SEARCH = new String[]{FIELD_TITLE, FIELD_CONTENT};
    float FIELD_BOOST_TITLE = 2.0f;
    float FIELD_BOOST_CONTENT = 1.0f;
}
