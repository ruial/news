package com.briefbytes.news.downloader;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.Set;

public class DynamicDownloader implements Downloader, Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(DynamicDownloader.class);
    private static final Set<String> BLOCKED_RESOURCES = Set.of("image", "stylesheet", "media", "font", "other");

    private Playwright playwright;
    private Browser browser;
    private Duration timeout;

    public DynamicDownloader(Duration timeout) {
        playwright = Playwright.create();
        browser = playwright.chromium().launch();
        this.timeout = timeout;
    }

    @Override
    public String download(URL url, boolean clean) throws IOException {
        String content;
        String baseUri = url.toString();
        // playwright is not thread safe, could create an instance per thread/process
        synchronized (this) {
            Page page = null;
            try {
                page = browser.newPage();
                page.route("**/*", route -> {
                    if (BLOCKED_RESOURCES.contains(route.request().resourceType())) {
                        route.abort();
                    } else {
                        route.resume();
                    }
                });
                page.setDefaultTimeout(timeout.toMillis());
                page.navigate(baseUri);
                content = page.innerHTML("body");
            } catch (Exception e) {
                throw new IOException(e);
            } finally {
                if (page != null) page.close();
            }
        }
        return clean ? clean(content, baseUri) : content;
    }

    @Override
    public synchronized void close() {
        try {
            LOG.info("Closing playwright browser");
            browser.close();
        } catch(Exception e) {
            LOG.error("Error closing browser", e);
        } finally {
            try {
                LOG.info("Closing playwright");
                playwright.close();
                LOG.info("Playwright closed");
            } catch (Exception e) {
                LOG.error("Error closing playwright", e);
            }
        }
    }
}
