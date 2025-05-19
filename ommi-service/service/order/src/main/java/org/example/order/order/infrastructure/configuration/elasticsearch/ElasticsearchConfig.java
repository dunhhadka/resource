package org.example.order.order.infrastructure.configuration.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class ElasticsearchConfig {

    @Bean
    public ElasticsearchClient restHighLevelClient(@Value("${es.hosts}") List<String> esHosts) {
        var httpHosts = esHosts.stream()
                .map(address -> {
                    var hostAndPort = address.split(":");
                    return new HttpHost(hostAndPort[0], Integer.valueOf(hostAndPort[1]));
                })
                .toArray(HttpHost[]::new);
        var restClient = RestClient.builder(httpHosts).build();

        var elasticsearchTransport = new RestClientTransport(restClient, new JacksonJsonpMapper());

        return new ElasticsearchClient(elasticsearchTransport);
    }
}
