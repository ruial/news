package com.briefbytes.news.downloader;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;

public class StaticDownloader implements Downloader {

    private static final String ACCEPT_HEADERS = "application/json, application/xhtml+xml, application/xml, text/html, text/plain";
    private static final int MAX_BODY_SIZE = 1024 * 512; // 0.5MB

    private Duration timeout;
    private String userAgent;

    public StaticDownloader(Duration timeout, String userAgent) {
        this.timeout = timeout;
        this.userAgent = userAgent;
    }
    @Override
    public String download(URL url, boolean clean) throws IOException {
        var connection = Jsoup.connect(url.toString())
                .timeout(Math.toIntExact(timeout.toMillis()))
                .maxBodySize(MAX_BODY_SIZE)
                .ignoreContentType(true)
                .header("accept", ACCEPT_HEADERS);
        if (userAgent != null) {
            connection.userAgent(userAgent);
        }
        var response = connection.execute();
        if (!clean) {
            return response.body();
        }
        var baseUri = url.getProtocol() + "://" + url.getAuthority();
        return clean(response.parse().body().html(), baseUri);
    }
}
