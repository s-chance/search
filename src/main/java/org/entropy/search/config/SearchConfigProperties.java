package org.entropy.search.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("meili.search")
public class SearchConfigProperties {
    private String hostUrl;
    private String apiKey;
}
