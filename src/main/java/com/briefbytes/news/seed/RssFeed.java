package com.briefbytes.news.seed;

import com.briefbytes.news.downloader.Downloader;
import com.briefbytes.news.model.ContentFormat;
import com.briefbytes.news.model.News;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RssFeed extends Seed {

    private static final String ISO_8601 = "yyyy-MM-dd'T'HH:mm";
    private static final String RFC_822 = "EEE, dd MMM yyyy HH:mm:ss Z";

    private String feedUrl;
    private Downloader downloader;
    private boolean fetchContent;

    public RssFeed(String seedName, String seedUrl, String feedUrl, Downloader downloader, boolean fetchContent) {
        super(seedName, seedUrl);
        this.feedUrl = feedUrl;
        this.downloader = downloader;
        this.fetchContent = fetchContent;
    }

    private String getText(Element element, String query) {
        Element match = element.selectFirst(query);
        return match == null ? "" : match.text();
    }

    private String getLink(Element element) {
        String link = getText(element, "link");
        if (!link.equals("")) return link;
        Element match = element.selectFirst("link");
        return match == null ? "" : match.attr("href");
    }

    private Date getDate(Element element) {
        String text = getText(element, "published,pubDate");
        try {
            return new SimpleDateFormat(ISO_8601).parse(text.substring(0, ISO_8601.length() + 1));
        } catch (ParseException | IndexOutOfBoundsException e) {
            try {
                return new SimpleDateFormat(RFC_822, Locale.US).parse(text);
            } catch (ParseException ex) {
                // fallback to current date
                return new Date();
            }
        }
    }

    @Override
    public List<News> poll() throws IOException {
        String content = downloader.download(new URL(feedUrl), false);
        // try to parse both rss and atom format
        return Jsoup.parse(content, Parser.xmlParser())
                .select("entry,item")
                .stream()
                .map(element -> {
                    String story = getLink(element);
                    String comments = getText(element, "comments");
                    var builder = new News.NewsBuilder()
                            .withSeed(getSeedName())
                            .withId(getSeedName() + "-" + story)
                            .withTitle(getText(element, "title"))
                            .withStoryUrl(story)
                            .withCommentsUrl(comments.isEmpty() ? story : comments)
                            .withDate(getDate(element))
                            .withContentFormat(ContentFormat.HTML);
                    if (!fetchContent) {
                        // if we don't want to fetch content, can grab description from the feed directly, although sometimes it is trimmed
                        builder.withContent(downloader.clean(getText(element, "content,description"), story));
                    }
                    return builder.build();
                })
                .filter(n -> !n.getStoryUrl().isEmpty())
                .toList();
    }

}
