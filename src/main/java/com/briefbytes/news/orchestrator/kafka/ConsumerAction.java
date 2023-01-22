package com.briefbytes.news.orchestrator.kafka;

public interface ConsumerAction<T> {
    void consume(T record);
}
