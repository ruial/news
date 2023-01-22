package com.briefbytes.news.orchestrator;

import com.briefbytes.news.downloader.Downloader;
import com.briefbytes.news.index.Index;
import com.briefbytes.news.model.News;
import com.briefbytes.news.orchestrator.kafka.ConsumerAction;
import com.briefbytes.news.orchestrator.kafka.NewsConsumer;
import com.briefbytes.news.orchestrator.kafka.NewsProducer;
import com.briefbytes.news.seed.Seed;
import com.briefbytes.news.utils.CloseableUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DistributedOrchestrator extends Orchestrator implements ConsumerAction<News> {

    private static final Logger LOG = LoggerFactory.getLogger(DistributedOrchestrator.class);
    private static final Duration LEADER_TIMEOUT = Duration.ofSeconds(60);
    private CuratorFramework curator;
    private LeaderLatch leaderLatch;
    private NewsProducer producer;
    private NewsConsumer consumer;


    public DistributedOrchestrator(Index index, Downloader downloader, List<Seed> seeds,
                                   CuratorFramework curator, LeaderLatch leaderLatch,
                                   NewsProducer producer, int retentionDays) {
        super(index, downloader, seeds, retentionDays);
        this.curator = curator;
        this.leaderLatch = leaderLatch;
        this.producer = producer;
    }

    public void setConsumer(NewsConsumer consumer) {
        this.consumer = consumer;
    }

    @Override
    public void run() {
        LOG.info("Running distributed orchestrator");
        try {
            leaderLatch.await(LEADER_TIMEOUT.getSeconds(), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOG.error("Leader checking interrupted", e);
            Thread.currentThread().interrupt();
            return;
        }
        if (leaderLatch.hasLeadership()) {
            LOG.info("I am the leader");
            // The leader has the additional task of fetching all news from the seeds
            // and every instance is responsible for fetching content and saving to the index
            List<News> news = fetchSeedsNews();
            for(News n : news) {
                try {
                    producer.send(n);
                } catch (Exception e) {
                    LOG.error("Unable to send news to kafka", e);
                }
            }
            deleteOldNews();
        } else {
            LOG.info("I am not the leader");
        }
        LOG.info("Distributed orchestrator finished");
    }

    @Override
    public void close() {
        CloseableUtils.closeQuietly(index, leaderLatch, curator, producer, consumer);
    }

    @Override
    public void consume(News record) {
        try {
            refreshNewsContent(record);
        } catch (Exception e) {
            // may get unchecked CancellationException on close, but it's ok
            LOG.error("Error fetching and saving news content", e);
        }
    }
}
