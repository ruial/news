package com.briefbytes.news.orchestrator.kafka;

import com.briefbytes.news.model.News;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

public class NewsConsumer implements Runnable, Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(NewsConsumer.class);
    private static final String CONSUMER_GROUP = "news-group";
    private static final String AUTO_OFFSET_RESET = "earliest";
    private static final String ENABLE_AUTO_COMMIT = "false";
    private static final String MAX_POLL_RECORDS = "10";
    private static final Duration POLL_DURATION = Duration.ofSeconds(10);

    private KafkaConsumer<String, News> consumer;
    private String topic;
    private ConsumerAction<News> callback;
    private AtomicBoolean closed = new AtomicBoolean(false);

    public NewsConsumer(String bootstrapServers, String topic, ConsumerAction<News> callback) {
        Properties kafkaProps = new Properties();
        kafkaProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        kafkaProps.put(ConsumerConfig.GROUP_ID_CONFIG, CONSUMER_GROUP);
        kafkaProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, AUTO_OFFSET_RESET);
        kafkaProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, ENABLE_AUTO_COMMIT);
        kafkaProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, MAX_POLL_RECORDS);
        consumer = new KafkaConsumer<>(kafkaProps, new StringDeserializer(), new JsonDeserializer<>(News.class));
        this.topic = topic;
        this.callback = callback;
    }

    @Override
    public void run() {
        LOG.info("Kafka news consumer started");
        try {
            consumer.subscribe(Collections.singletonList(topic));
            while (!closed.get()) {
                var records = consumer.poll(POLL_DURATION);
                for(ConsumerRecord<String, News> record : records) {
                    callback.consume(record.value());
                }
                consumer.commitAsync();
            }
        } catch (WakeupException e) {
            // Ignore exception if closing
            if (!closed.get()) throw e;
        } catch (Exception e) {
            LOG.error("Unexpected kafka consumer error", e);
        } finally {
            try {
                consumer.commitSync();
            } catch (Exception e) {
                LOG.error("Unable to commit before closing kafka consumer: ", e);
            } finally {
                consumer.close();
            }
        }
    }

    @Override
    public void close() {
        // kafka consumer, unlike the producer, is not thread safe except for the wakeup method
        closed.set(true);
        consumer.wakeup();
    }

}
