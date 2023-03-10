package com.briefbytes.news.orchestrator.kafka;

import com.briefbytes.news.model.News;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.Closeable;
import java.io.IOException;
import java.util.Properties;

public class NewsProducer implements Closeable {

    // https://docs.confluent.io/platform/current/installation/configuration/producer-configs.html#enable-idempotence
    private static final String IDEMPOTENCE = "true";
    // if idempotence is disabled, MAX_REQUESTS should be 1 to avoid incorrect message ordering
    // due to retries (enabled by default), but it would reduce throughput
    private static final String MAX_REQUESTS = "5";
    private static final String REQUIRED_ACKS = "all";

    private KafkaProducer<String, News> producer;
    private String topic;

    public NewsProducer(String bootstrapServers, String topic) {
        Properties kafkaProps = new Properties();
        kafkaProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        kafkaProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, IDEMPOTENCE);
        kafkaProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, MAX_REQUESTS);
        kafkaProps.put(ProducerConfig.ACKS_CONFIG, REQUIRED_ACKS);
        producer = new KafkaProducer<>(kafkaProps, new StringSerializer(), new JsonSerializer<>());
        this.topic = topic;
    }

    public void send(News news) throws Exception {
        producer.send(new ProducerRecord<>(topic, news.getId(), news)).get();
    }

    @Override
    public void close() {
        try {
            producer.flush();
        } finally {
            producer.close();
        }
    }
}
