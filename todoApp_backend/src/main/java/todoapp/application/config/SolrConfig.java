package todoapp.application.config;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SolrConfig {
    @SuppressWarnings("deprecation")
    @Bean
    public SolrClient solrClient(
        @Value("${app.search.solr.base-url}") String baseUrl 
    ) {
         return new HttpSolrClient.Builder(baseUrl)
                .withConnectionTimeout(10_000)
                .withSocketTimeout(10_000)
                .build();
    }
}
