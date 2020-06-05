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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import bio.overture.maestro.app.infra.config.properties.PropertiesConfig;
import bio.overture.maestro.domain.api.exception.FailureData;
import bio.overture.maestro.domain.api.message.IndexResult;
import bio.overture.maestro.domain.entities.indexing.FileCentricDocument;
import bio.overture.maestro.domain.port.outbound.indexing.BatchIndexFilesCommand;
import bio.overture.masestro.test.Fixture;
import bio.overture.masestro.test.TestCategory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

@Slf4j
@Tag(TestCategory.INT_TEST)
@SpringBootTest(properties = {"embedded.elasticsearch.enabled=false"})
@ContextConfiguration(classes = {FileCentricElasticSearchAdapterUnavaibilityTest.Config.class})
class FileCentricElasticSearchAdapterUnavaibilityTest {

  @SpyBean private RestHighLevelClient client;

  @Autowired private FileCentricElasticSearchAdapter adapter;

  @Test
  void shouldRetryUpsertOnIOException() throws IOException {
    // given
    val files =
        Arrays.asList(
            Fixture.loadJsonFixtureSnakeCase(
                this.getClass(), "PEME-CA.files.json", FileCentricDocument[].class));

    val expectedResult =
        IndexResult.builder()
            .indexName("file_centric")
            .failureData(
                FailureData.builder()
                    .failingIds(Map.of("analysisId", Set.of("EGAZ00001254368")))
                    .build())
            .successful(false)
            .build();

    // when
    val result =
        adapter.batchUpsertFileRepositories(BatchIndexFilesCommand.builder().files(files).build());

    // then
    StepVerifier.create(result).expectNext(expectedResult).verifyComplete();

    // since this is a final method I had to add the mockito-extensions directory to test resources
    // see why.md there for more info.
    verify(client, times(3)).bulk(any(BulkRequest.class), any(RequestOptions.class));
  }

  @Import({
    FileCentricElasticSearchAdapter.class,
    SnakeCaseJacksonSearchResultMapper.class,
    PropertiesConfig.class
  })
  @Configuration
  static class Config {

    @Bean
    WebClient webClient() {
      return WebClient.builder().build();
    }

    @Bean
    RestHighLevelClient mockClient() {
      // this will trigger an IO exception
      val restClient = RestClient.builder(new HttpHost("nonexisting", 9201, "http"));
      return new RestHighLevelClient(restClient);
    }

    @Bean(name = ELASTIC_SEARCH_DOCUMENT_JSON_MAPPER)
    ObjectMapper documentObjectMapper() {
      val mapper = new ObjectMapper();
      mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
      return mapper;
    }
  }
}
