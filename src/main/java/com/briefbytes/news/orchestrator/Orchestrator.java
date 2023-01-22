package com.briefbytes.news.orchestrator;

import com.briefbytes.news.downloader.Downloader;
import com.briefbytes.news.index.Index;
import com.briefbytes.news.model.ContentFormat;
import com.briefbytes.news.model.News;
import com.briefbytes.news.seed.Seed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public abstract class Orchestrator implements Runnable, Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(Orchestrator.class);

    protected Index index;
    protected Downloader downloader;
    protected List<Seed> seeds;
    protected int retentionDays;

    protected Orchestrator(Index index, Downloader downloader, List<Seed> seeds, int retentionDays) {
        this.index = index;
        this.downloader = downloader;
        this.seeds = seeds;
        this.retentionDays = retentionDays;
    }

    protected List<News> fetchSeedsNews() {
        return seeds.parallelStream()
                .flatMap(seed -> {
                    try {
                        LOG.info("Fetching seed: " + seed.getSeedName());
                        return seed.poll().parallelStream();
                    } catch (IOException e) {
                        LOG.error("Unable to fetch seed: " + seed.getSeedName(), e);
                        return Stream.empty();
                    }
                })
                .filter(Objects::nonNull)
                .toList();
    }

    protected void refreshNewsContent(News news) {
        if (news.getContent() == null && news.getContentFormat() == ContentFormat.HTML) {
            var newsUrl = news.getStoryUrl();
            try {
                var indexedContent = index.getContent(newsUrl);
                if (indexedContent == null) {
                    news.setContent(downloader.download(new URL(newsUrl), true));
                } else {
                    news.setContent(indexedContent);
                }
            } catch (IOException e) {
                LOG.error("Unable to fetch content for: " + newsUrl, e);
            }
        }
        try {
            index.saveNews(news);
        } catch (IOException e) {
            LOG.error("Unable to save news for: " + news.getId(), e);
        }
    }

    protected void deleteOldNews() {
        var then = LocalDate.now().minusDays(retentionDays);
        var date = Date.from(then.atStartOfDay(ZoneId.systemDefault()).toInstant());
        try {
            index.deleteOldNews(date);
        } catch (IOException e) {
            LOG.error("Unable to delete old news", e);
        }
    }

}
