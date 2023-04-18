package com.briefbytes.news;

import com.briefbytes.news.downloader.Downloader;
import com.briefbytes.news.downloader.DynamicDownloader;
import com.briefbytes.news.downloader.StaticDownloader;
import com.briefbytes.news.index.ElasticsearchIndexFactory;
import com.briefbytes.news.index.Index;
import com.briefbytes.news.index.LuceneIndexFactory;
import com.briefbytes.news.orchestrator.DistributedOrchestratorFactory;
import com.briefbytes.news.orchestrator.MultiThreadOrchestratorFactory;
import com.briefbytes.news.orchestrator.Orchestrator;
import com.briefbytes.news.orchestrator.OrchestratorFactory;
import com.briefbytes.news.seed.Seed;
import com.briefbytes.news.seed.SeedList;
import com.briefbytes.news.ui.JavalinApp;
import com.briefbytes.news.utils.CloseableUtils;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);
    private static final String DEFAULT_SEEDS_FILE = "seeds.json";
    private static final Duration DEFAULT_TIMEOUT_SECONDS = Duration.ofSeconds(5);
    private static final Duration DEFAULT_POLL_INTERVAL = Duration.ofMinutes(60);
    private static final int DEFAULT_PORT = 8080;
    private static final int DEFAULT_RETENTION_DAYS = 90;
    private static final String OPT_ROLES = "roles";
    private static final String OPT_DISTRIBUTED = "distributed";
    private static final String OPT_SEEDS_FILE = "seeds";
    private static final String OPT_LUCENE_DIR = "lucene-dir";
    private static final String OPT_ELASTIC_HOST = "elastic-host";
    private static final String OPT_ZOOKEEPER_HOST = "zookeeper-host";
    private static final String OPT_KAFKA_HOSTS = "kafka-hosts";
    private static final String OPT_TIMEOUT = "timeout";
    private static final String OPT_DYNAMIC_DOWNLOADER = "dynamic-downloader";
    private static final String OPT_USER_AGENT = "user-agent";
    private static final String OPT_THREADS = "threads";
    private static final String OPT_POLL_INTERVAL = "poll-interval";
    private static final String OPT_PORT = "port";
    private static final String OPT_RETENTION = "retention";

    private static void exit(String message, Exception exception) {
        LOG.error(message, exception);
        System.exit(1);
    }

    public static void main(String[] args) {
        Options options = new Options();
        options.addOption(Option.builder().longOpt(OPT_ROLES).hasArgs().required().build());
        options.addOption(Option.builder().longOpt(OPT_DISTRIBUTED).hasArg(false).build());
        options.addOption(Option.builder().longOpt(OPT_SEEDS_FILE).hasArg().build());
        options.addOption(Option.builder().longOpt(OPT_LUCENE_DIR).hasArg().build());
        options.addOption(Option.builder().longOpt(OPT_ELASTIC_HOST).hasArg().build());
        options.addOption(Option.builder().longOpt(OPT_ZOOKEEPER_HOST).hasArg().build());
        options.addOption(Option.builder().longOpt(OPT_KAFKA_HOSTS).hasArgs().build());
        options.addOption(Option.builder().longOpt(OPT_TIMEOUT).hasArg().build());
        options.addOption(Option.builder().longOpt(OPT_THREADS).hasArg().build());
        options.addOption(Option.builder().longOpt(OPT_POLL_INTERVAL).hasArg().build());
        options.addOption(Option.builder().longOpt(OPT_DYNAMIC_DOWNLOADER).hasArg(false).build());
        options.addOption(Option.builder().longOpt(OPT_USER_AGENT).hasArg().build());
        options.addOption(Option.builder().longOpt(OPT_PORT).hasArg().build());
        options.addOption(Option.builder().longOpt(OPT_RETENTION).hasArg().build());
        CommandLineParser parser = new DefaultParser();

        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException | NumberFormatException e) {
            exit("Unable to parse command line arguments", e);
            return;
        }

        Duration timeout;
        try {
            timeout = Duration.ofSeconds(Long.parseLong(cmd.getOptionValue(OPT_TIMEOUT)));
        } catch (NumberFormatException e) {
            timeout = DEFAULT_TIMEOUT_SECONDS;
        }

        Duration pollInterval;
        try {
            pollInterval = Duration.ofMinutes(Long.parseLong(cmd.getOptionValue(OPT_POLL_INTERVAL)));
        } catch (NumberFormatException e) {
            pollInterval = DEFAULT_POLL_INTERVAL;
        }

        int port;
        try {
            port = Integer.parseInt(cmd.getOptionValue(OPT_PORT));
        } catch (NumberFormatException e) {
            port = DEFAULT_PORT;
        }

        int retentionDays;
        try {
            retentionDays = Integer.parseInt(cmd.getOptionValue(OPT_RETENTION));
        } catch (NumberFormatException e) {
            retentionDays = DEFAULT_RETENTION_DAYS;
        }

        boolean distributed = cmd.hasOption(OPT_DISTRIBUTED);
        List<String> roles = List.of(cmd.getOptionValues(OPT_ROLES));
        String seedsFile = cmd.getOptionValue(OPT_SEEDS_FILE, DEFAULT_SEEDS_FILE);
        String luceneDir = cmd.getOptionValue(OPT_LUCENE_DIR);
        String elasticHost = cmd.getOptionValue(OPT_ELASTIC_HOST);
        String zookeeperHost = cmd.getOptionValue(OPT_ZOOKEEPER_HOST);
        String kafkaHosts = cmd.getOptionValue(OPT_KAFKA_HOSTS);
        String userAgent = cmd.getOptionValue(OPT_USER_AGENT);
        String threads = cmd.getOptionValue(OPT_THREADS);

        if (threads != null && threads.chars().allMatch(Character::isDigit)) {
            // to configure concurrent requests made by using parallelStream with the default pool
            System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", threads);
        }

        Downloader staticDownloader = new StaticDownloader(timeout, userAgent);
        Downloader downloader = staticDownloader;

        // items to be closed on clean shutdown
        List<Closeable> closeables = new ArrayList<>();

        if (cmd.hasOption(OPT_DYNAMIC_DOWNLOADER)) {
            var dynamicDownloader = new DynamicDownloader(timeout);
            downloader = dynamicDownloader;
            closeables.add(dynamicDownloader);
        }

        // Load seeds from configuration file, always use the static http client to fetch data from APIs and rss feeds
        List<Seed> seeds = null;
        try {
            seeds = SeedList.fromFile(seedsFile).getSeeds(staticDownloader);
        } catch (IOException | IllegalArgumentException e) {
            exit("Unable to parse seeds file", e);
            return;
        }

        // Use factories when creation of object is somewhat complex, some would prefer dependency injection
        final Index index;
        try {
            if (distributed) {
                LOG.info("Starting application in distributed mode");
                index = new ElasticsearchIndexFactory(elasticHost).createIndex();
            } else {
                LOG.info("Starting application in standalone mode");
                index = new LuceneIndexFactory(luceneDir).createIndex();
            }
        } catch (IOException e) {
            exit("Unable to create index", e);
            return;
        }

        if (roles.contains("orchestrator")) {
            LOG.info("Starting orchestrator");
            OrchestratorFactory orchestratorFactory = distributed ?
                    new DistributedOrchestratorFactory(index, downloader, seeds, zookeeperHost, kafkaHosts, retentionDays) :
                    new MultiThreadOrchestratorFactory(index, downloader, seeds, retentionDays);
            Orchestrator orchestrator = null;
            try {
                orchestrator = orchestratorFactory.createOrchestrator();
            } catch (Exception e) {
                exit("Unable to create orchestrator", e);
                return;
            }
            var scheduler = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "OrchestratorThread"));
            var future = scheduler.scheduleAtFixedRate(orchestrator, 0, pollInterval.toMinutes(), TimeUnit.MINUTES);
            closeables.add(() -> {
                // would need to check if thread was interrupted inside the orchestrator
                future.cancel(true);
                scheduler.shutdown();
            });
            closeables.add(orchestrator);
        }

        if (roles.contains("webserver")) {
            var server = new JavalinApp(seeds, index, port, timeout);
            server.start();
            // web server should be closed first
            closeables.add(0, server);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOG.info("Shutting down...");
            CloseableUtils.closeQuietly(closeables.toArray(Closeable[]::new));
            /*
                To check running threads
                Thread.getAllStackTraces().keySet().forEach(t -> {
                    System.out.println(t.getName() + " : " + t.isDaemon() + " : " + t.isAlive());
                });
             */
            LOG.info("Goodbye");
        }));

    }
}
