package com.briefbytes.news.seed;

import com.briefbytes.news.downloader.Downloader;
import com.briefbytes.news.model.ContentFormat;
import com.briefbytes.news.model.News;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Reddit extends Seed {

    private Downloader downloader;
    private int maxItems;

    public Reddit(String subreddit, Downloader downloader) {
        this(subreddit, downloader, MAX_ITEMS_DEFAULT);
    }

    public Reddit(String subreddit, Downloader downloader, int maxItems) {
        super("r/" + subreddit, "https://reddit.com/r/" + subreddit);
        this.downloader = downloader;
        this.maxItems = maxItems;
    }

    @Override
    public List<News> poll() throws IOException {
        var url = new URL(getSeedUrl() + ".json?limit=" + maxItems);
        var it = new ObjectMapper().readTree(downloader.download(url, false))
                .get("data")
                .get("children")
                .elements();
        List<News> news = new ArrayList<>();
        while(it.hasNext()) {
            var node = it.next().get("data");
            if (!node.get("stickied").asBoolean()) {
                var builder = new News.NewsBuilder()
                        .withId(getSeedName() + "-" + node.get("id").asText())
                        .withSeed(getSeedName())
                        .withTitle(node.get("title").asText())
                        .withCommentsUrl("https://reddit.com" + node.get("permalink").asText())
                        .withDate(new Date(node.get("created_utc").asLong() * 1000))
                        .withScore(node.get("score").asInt())
                        .withCommentsCount(node.get("num_comments").asInt())
                        .withStoryUrl(node.get("url").asText());
                // unlike hackernews api, the text field always exists and can be null, which is parsed as empty string
                var text = node.get("selftext").asText();
                if (text.isEmpty()) {
                    builder.withContentFormat(ContentFormat.HTML);
                } else {
                    builder.withContentFormat(ContentFormat.TEXT);
                    builder.withContent(text);
                }
                news.add(builder.build());
            }
        }
        return news;
    }
}
