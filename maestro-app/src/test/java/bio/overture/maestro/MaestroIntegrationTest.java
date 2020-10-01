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

package bio.overture.maestro;

import bio.overture.maestro.app.Maestro;
import bio.overture.maestro.app.infra.config.properties.ApplicationProperties;
import bio.overture.masestro.test.TestCategory;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ContextConfiguration;

/**
 * Base class for full integration tests. it will create an elasticsearch container (using test
 * containers).
 *
 * <p>if you need a more light weight faster tests, avoid using this class, this is meant for full
 * end to end.
 */
@Slf4j
@Tag(TestCategory.INT_TEST)
@ContextConfiguration(classes = {Maestro.class})
@AutoConfigureWireMock(port = 0) // we bind the port in the application.yml ${wiremock.server.port}
@SpringBootTest(
    classes = {Maestro.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"embedded.elasticsearch.enabled=true"})
public abstract class MaestroIntegrationTest {

  /** wait time for elastic search to update */
  @Value("${maestro.test.elasticsearch.sleep_millis:2500}")
  protected int sleepMillis;

  @Autowired private ApplicationProperties properties;

  @Autowired private RestHighLevelClient client;

  @AfterEach
  @SneakyThrows
  void tearDown() {
    // clean indexes after each test to keep tests isolated
    DeleteByQueryRequest deleteByQueryRequest = new DeleteByQueryRequest();

    if (properties.isFileCentricIndexEnabled()) {
      deleteByQueryRequest.indices(properties.fileCentricAlias());
    }

    if (properties.isAnalysisCentricIndexEnabled()) {
      deleteByQueryRequest.indices(properties.analysisCentricAlias());
    }

    deleteByQueryRequest.setQuery(QueryBuilders.matchAllQuery());
    client.deleteByQuery(deleteByQueryRequest, RequestOptions.DEFAULT);
    Thread.sleep(sleepMillis);
  }
}
