package org.entropy.search.config;

import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Config;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(SearchConfigProperties.class)
public class SearchConfiguration {

    @Bean
    @ConditionalOnMissingBean(Client.class)
    public Client searchClient(final SearchConfigProperties searchConfigProperties) {
        return new Client(
                new Config(searchConfigProperties.getHostUrl(),
                        searchConfigProperties.getApiKey()));
    }
}
