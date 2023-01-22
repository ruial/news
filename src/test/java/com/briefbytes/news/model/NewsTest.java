package com.briefbytes.news.model;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class NewsTest {

    @Test
    void testBuildNews() {
        assertDoesNotThrow(() -> new News.NewsBuilder()
                .withId("id")
                .withDate(new Date())
                .withTitle("News Title")
                .withSeed("briefbytes")
                .withStoryUrl("https://briefbytes.com")
                .withContentFormat(ContentFormat.HTML)
                .build()
        );
    }

    @Test
    void testBuildNewsNull() {
        assertThrows(NullPointerException.class, () -> new News.NewsBuilder().build());
    }

}
