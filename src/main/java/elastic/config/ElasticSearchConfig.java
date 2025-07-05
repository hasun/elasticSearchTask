package elastic.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.elasticsearch.support.HttpHeaders;

import javax.net.ssl.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Objects;

@Configuration
@ComponentScan(basePackages = { "elastic.search.service", "elastic.index.service"})
@EnableElasticsearchRepositories(basePackages = "elastic.search.repository")
public class ElasticSearchConfig extends ElasticsearchConfiguration {
    @Autowired
    ApplicationProperties applicationProperties;

    @Override
    public ClientConfiguration clientConfiguration() {
        return ClientConfiguration.builder()
                .connectedTo(applicationProperties.getHosts())
                .withConnectTimeout(100000000)
                .withSocketTimeout(100000000)
                .build();
    }
}
