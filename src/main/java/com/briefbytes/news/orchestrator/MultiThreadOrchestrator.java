package com.briefbytes.news.orchestrator;

import com.briefbytes.news.downloader.Downloader;
import com.briefbytes.news.index.Index;
import com.briefbytes.news.model.News;
import com.briefbytes.news.seed.Seed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class MultiThreadOrchestrator extends Orchestrator {

    private static final Logger LOG = LoggerFactory.getLogger(MultiThreadOrchestrator.class);

    public MultiThreadOrchestrator(Index index, Downloader downloader, List<Seed> seeds, int retentionDays) {
        super(index, downloader, seeds, retentionDays);
    }

    @Override
    public void run() {
        LOG.info("Running multi-thread orchestrator");
        // parallel stream uses common ForkJoinPool, but could also create a custom pool to set parallelism
        // https://stackoverflow.com/questions/21163108/custom-thread-pool-in-java-8-parallel-stream
        // flatmap converts stream to sequential, so we have to create a new stream, can be checked with AtomicInteger
        // https://stackoverflow.com/questions/45038120/parallel-flatmap-always-sequential
        List<News> news = fetchSeedsNews();
        news.parallelStream().forEach(this::refreshNewsContent);
        deleteOldNews();
        try {
            index.commit();
        } catch (IOException e) {
            LOG.error("Unable to commit changes", e);
        }
        LOG.info("Multi-thread orchestrator finished");
    }

    @Override
    public void close() throws IOException {
        index.close();
    }

}
