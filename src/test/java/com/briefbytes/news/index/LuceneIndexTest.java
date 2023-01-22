package com.briefbytes.news.index;

import com.briefbytes.news.model.ContentFormat;
import com.briefbytes.news.model.News;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LuceneIndexTest {

    private LuceneIndex index;
    private News news;

    @BeforeEach
    void init() throws IOException {
        index = (LuceneIndex) new LuceneIndexFactory(null).createIndex();
        news = new News.NewsBuilder()
                .withId("id")
                .withDate(new Date())
                .withTitle("News Title")
                .withSeed("briefbytes")
                .withStoryUrl("https://briefbytes.com")
                .withContentFormat(ContentFormat.HTML)
                .build();
        index.saveNews(news);
        index.refresh();
    }

    @AfterEach
    void teardown() {
        index.close();
    }

    @Test
    void testUpsert() throws IOException {
        news.setSeed("briefbytes.com");
        index.saveNews(news);
        index.refresh();
        assertEquals(news.getSeed(), index.getNewsById(news.getId()).getSeed());
    }

    @Test
    void testSearch() throws IOException {
        var date = news.getDate();
        assertEquals(0, index.searchNews("notfound", 1, date, date).size());
        assertEquals(1, index.searchNews("title", 1, date, date).size());
        assertEquals(0, index.searchNews("title", 1, new Date(date.getTime() + 1), date).size());
        assertEquals(0, index.searchNews("title", 1, date, new Date(date.getTime() - 1)).size());
    }

    @Test
    void testDeleteNews() throws IOException {
        index.deleteNews("NA");
        index.refresh();
        assertEquals(1, index.countNews());
        index.deleteNews(news.getId());
        index.refresh();
        assertEquals(0, index.countNews());
    }

    @Test
    void testDeleteOldNews() throws IOException {
        var calendar = Calendar.getInstance();
        calendar.set(2023, Calendar.JANUARY, 2);
        var oldNews = new News.NewsBuilder()
                .withId("old")
                .withDate(calendar.getTime())
                .withTitle("News Title")
                .withSeed("briefbytes")
                .withStoryUrl("https://briefbytes.com")
                .withContentFormat(ContentFormat.HTML)
                .build();
        index.saveNews(oldNews);
        calendar.set(Calendar.DATE, 1);
        index.deleteOldNews(calendar.getTime());
        index.refresh();
        assertEquals(2, index.countNews());
        calendar.set(Calendar.DATE, 3);
        index.deleteOldNews(calendar.getTime());
        index.refresh();
        assertEquals(1, index.countNews());
    }

}
