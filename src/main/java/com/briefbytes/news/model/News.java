package com.briefbytes.news.model;

import java.util.Date;
import java.util.Objects;

public class News {

    private String id;
    private String seed;
    private String title;
    private String commentsUrl;
    private String storyUrl;
    private int commentsCount;
    private int score;
    private Date date;
    private String content;
    private ContentFormat contentFormat;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        Objects.requireNonNull(id, "News id is required");
        this.id = id;
    }

    public String getSeed() {
        return seed;
    }

    public void setSeed(String seed) {
        Objects.requireNonNull(seed, "News seed is required");
        this.seed = seed;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        Objects.requireNonNull(title, "News title is required");
        this.title = title;
    }

    public String getCommentsUrl() {
        return commentsUrl;
    }

    public void setCommentsUrl(String commentsUrl) {
        this.commentsUrl = commentsUrl;
    }

    public String getStoryUrl() {
        return storyUrl;
    }

    public void setStoryUrl(String storyUrl) {
        Objects.requireNonNull(storyUrl, "News storyUrl is required");
        this.storyUrl = storyUrl;
    }

    public int getCommentsCount() {
        return commentsCount;
    }

    public void setCommentsCount(int commentsCount) {
        this.commentsCount = commentsCount;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        Objects.requireNonNull(date, "News date is required");
        this.date = date;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public ContentFormat getContentFormat() {
        return contentFormat;
    }

    public void setContentFormat(ContentFormat contentFormat) {
        Objects.requireNonNull(contentFormat, "News contentFormat is required");
        this.contentFormat = contentFormat;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        News news = (News) o;
        return id.equals(news.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "News{" +
                "id='" + id + '\'' +
                ", seed='" + seed + '\'' +
                ", title='" + title + '\'' +
                ", commentsUrl='" + commentsUrl + '\'' +
                ", storyUrl='" + storyUrl + '\'' +
                ", commentsCount=" + commentsCount +
                ", score=" + score +
                ", date=" + date +
                ", contentFormat=" + contentFormat +
                '}';
    }


    public static final class NewsBuilder {
        private String id;
        private String seed;
        private String title;
        private String commentsUrl;
        private String storyUrl;
        private int commentsCount;
        private int score;
        private Date date;
        private String content;
        private ContentFormat contentFormat;

        public NewsBuilder() {
        }

        public NewsBuilder withId(String id) {
            this.id = id;
            return this;
        }

        public NewsBuilder withSeed(String seed) {
            this.seed = seed;
            return this;
        }

        public NewsBuilder withTitle(String title) {
            this.title = title;
            return this;
        }

        public NewsBuilder withCommentsUrl(String commentsUrl) {
            this.commentsUrl = commentsUrl;
            return this;
        }

        public NewsBuilder withStoryUrl(String storyUrl) {
            this.storyUrl = storyUrl;
            return this;
        }

        public NewsBuilder withCommentsCount(int commentsCount) {
            this.commentsCount = commentsCount;
            return this;
        }

        public NewsBuilder withScore(int score) {
            this.score = score;
            return this;
        }

        public NewsBuilder withDate(Date date) {
            this.date = date;
            return this;
        }

        public NewsBuilder withContent(String content) {
            this.content = content;
            return this;
        }

        public NewsBuilder withContentFormat(ContentFormat contentFormat) {
            this.contentFormat = contentFormat;
            return this;
        }

        public News build() {
            News news = new News();
            news.setId(id);
            news.setSeed(seed);
            news.setTitle(title);
            news.setCommentsUrl(commentsUrl);
            news.setStoryUrl(storyUrl);
            news.setCommentsCount(commentsCount);
            news.setScore(score);
            news.setDate(date);
            news.setContent(content);
            news.setContentFormat(contentFormat);
            return news;
        }
    }
}
