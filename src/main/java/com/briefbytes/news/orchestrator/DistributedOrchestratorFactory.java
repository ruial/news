package com.briefbytes.news.orchestrator;

import com.briefbytes.news.downloader.Downloader;
import com.briefbytes.news.index.Index;
import com.briefbytes.news.orchestrator.kafka.NewsConsumer;
import com.briefbytes.news.orchestrator.kafka.NewsProducer;
import com.briefbytes.news.seed.Seed;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.retry.ExponentialBackoffRetry;

import java.util.List;

public class DistributedOrchestratorFactory implements OrchestratorFactory {

    private static final RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 5);
    private static final String KAFKA_TOPIC = "news";
    private static final String LATCH_PATH = "/news/leader";

    private Index index;
    private Downloader downloader;
    private List<Seed> seeds;
    private String zookeeperHost;
    private String kafkaHosts;
    private int retentionDays;

    public DistributedOrchestratorFactory(Index index, Downloader downloader, List<Seed> seeds,
                                          String zookeeperHost, String kafkaHosts, int retentionDays) {
        this.index = index;
        this.downloader = downloader;
        this.seeds = seeds;
        this.zookeeperHost = zookeeperHost;
        this.kafkaHosts = kafkaHosts;
        this.retentionDays = retentionDays;
    }

    @Override
    public Orchestrator createOrchestrator() throws Exception {
        CuratorFramework curator = CuratorFrameworkFactory.newClient(zookeeperHost, retryPolicy);
        curator.start();
        LeaderLatch leaderLatch = new LeaderLatch(curator, LATCH_PATH);
        leaderLatch.start();
        NewsProducer producer = new NewsProducer(kafkaHosts, KAFKA_TOPIC);
        var orchestrator = new DistributedOrchestrator(index, downloader, seeds, curator, leaderLatch, producer, retentionDays);
        NewsConsumer consumer = new NewsConsumer(kafkaHosts, KAFKA_TOPIC, orchestrator);
        new Thread(consumer, "KafkaNewsConsumer").start();
        // so the orchestrator can gracefully close the kafka consumer
        orchestrator.setConsumer(consumer);
        return orchestrator;
    }
}
