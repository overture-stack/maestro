/*
 *  Copyright (c) 2019. Ontario Institute for Cancer Research
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as
 *   published by the Free Software Foundation, either version 3 of the
 *   License, or (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package bio.overture.maestro.app.infra.adapter.outbound.indexing.elasticsearch;

import static bio.overture.maestro.app.infra.config.RootConfiguration.ELASTIC_SEARCH_DOCUMENT_JSON_MAPPER;

import bio.overture.maestro.app.infra.config.properties.ApplicationProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.stream.Collectors;
import lombok.val;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.ssl.SSLContextBuilder;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Elasticsearch related configuration this allows us to keep the beans package private to avoid
 * other packages using them instead of the interface, and be more explicit about configuration
 * scope.
 */
@ConditionalOnProperty(name = "maestro.disableIndexing", havingValue = "false")
@Configuration
@Import({
  FileCentricElasticSearchAdapter.class,
  AnalysisCentricElasticSearchAdapter.class,
  SnakeCaseJacksonSearchResultMapper.class
})
public class ElasticSearchConfig {

  /**
   * this bean executes when the application starts it's used to initialize the indexes in elastic
   * search server, can be extended as needed.
   */
  @Bean
  CommandLineRunner elasticsearchBootstrapper(FileCentricElasticSearchAdapter adapter) {
    return (args) -> adapter.initialize();
  }

  @Bean
  CommandLineRunner analysisElasticsearchBootstrapper(AnalysisCentricElasticSearchAdapter adapter) {
    return (args) -> adapter.initialize();
  }

  @Bean("ES_CLIENT")
  RestHighLevelClient client(ApplicationProperties properties) {
    val httpHostArrayList =
        new ArrayList<HttpHost>(
            properties.elasticSearchClusterNodes().stream()
                .map(HttpHost::create)
                .collect(Collectors.toUnmodifiableList()));
    val builder = RestClient.builder(httpHostArrayList.toArray(new HttpHost[] {}));

    builder.setHttpClientConfigCallback(
        (httpAsyncClientBuilder) -> {
          val connectTimeout = properties.elasticSearchClientConnectionTimeoutMillis();
          val timeout = properties.elasticSearchClientSocketTimeoutMillis();
          val requestConfigBuilder = RequestConfig.custom();

          if (connectTimeout > 0) {
            requestConfigBuilder.setConnectTimeout(connectTimeout);
            requestConfigBuilder.setConnectionRequestTimeout(connectTimeout);
          }
          if (timeout > 0) {
            requestConfigBuilder.setSocketTimeout(timeout);
          }
          httpAsyncClientBuilder.setDefaultRequestConfig(requestConfigBuilder.build());

          if (properties.elasticSearchTlsTrustSelfSigned()) {
            SSLContextBuilder sslCtxBuilder = new SSLContextBuilder();
            try {
              sslCtxBuilder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
              httpAsyncClientBuilder.setSSLContext(sslCtxBuilder.build());
            } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
              throw new RuntimeException("failed to build Elastic rest client");
            }
          }

          // set the credentials provider for auth
          if (properties.elasticSearchBasicAuthEnabled()) {
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(
                AuthScope.ANY,
                new UsernamePasswordCredentials(
                    properties.elasticSearchAuthUser(), properties.elasticSearchAuthPassword()));
            httpAsyncClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
          }
          return httpAsyncClientBuilder;
        });
    return new RestHighLevelClient(builder);
  }

  @Bean(name = ELASTIC_SEARCH_DOCUMENT_JSON_MAPPER)
  ObjectMapper documentObjectMapper() {
    val mapper = new ObjectMapper();
    mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
    return mapper;
  }
}
