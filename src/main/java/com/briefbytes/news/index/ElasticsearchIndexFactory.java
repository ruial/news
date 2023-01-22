package com.briefbytes.news.index;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import co.elastic.clients.elasticsearch.indices.get_mapping.IndexMappingRecord;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ElasticsearchIndexFactory implements IndexFactory {

    private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchIndexFactory.class);
    private static final String INDEX_NAME = "news";

    private String elasticHost;

    public ElasticsearchIndexFactory(String elasticHost) {
        this.elasticHost = elasticHost;
    }

    @Override
    public Index createIndex() throws IOException {
        RestClient restClient = RestClient.builder(HttpHost.create(elasticHost)).build();
        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        ElasticsearchClient client = new ElasticsearchClient(transport);

        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        var settings = new IndexSettings.Builder()
                .withJson(classloader.getResourceAsStream("index-settings.json"))
                .build();
        var mappings = new IndexMappingRecord.Builder()
                .withJson(classloader.getResourceAsStream("index-mappings.json"))
                .build().mappings();
        try {
            client.indices().create(new CreateIndexRequest.Builder()
                    .settings(settings)
                    .mappings(mappings)
                    .index(INDEX_NAME)
                    .build()
            );
        } catch (ElasticsearchException e) {
            if (e.getMessage().contains("already exists")) {
                LOG.info("News index already exists");
            } else {
                throw e;
            }
        }
        return new ElasticsearchIndex(INDEX_NAME, transport, client);
    }
}
