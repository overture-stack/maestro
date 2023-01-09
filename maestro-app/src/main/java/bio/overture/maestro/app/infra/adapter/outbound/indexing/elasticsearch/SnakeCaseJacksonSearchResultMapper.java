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

import bio.overture.maestro.app.infra.config.RootConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.LinkedList;
import lombok.SneakyThrows;
import lombok.val;
import org.elasticsearch.action.get.MultiGetResponse;
import org.springframework.beans.factory.annotation.Qualifier;

class SnakeCaseJacksonSearchResultMapper {

  private ObjectMapper objectMapper;

  public SnakeCaseJacksonSearchResultMapper(
      @Qualifier(RootConfiguration.ELASTIC_SEARCH_DOCUMENT_JSON_MAPPER) ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @SneakyThrows
  <T> LinkedList<T> mapResults(MultiGetResponse responses, Class<T> clazz) {
    val list = new LinkedList<T>();
    Arrays.stream(responses.getResponses())
        .filter((response) -> !response.isFailed() && response.getResponse().isExists())
        .forEach(
            (response) -> {
              T result = convertSourceToObject(clazz, response.getResponse().getSourceAsString());
              list.add(result);
            });
    return list;
  }

  @SneakyThrows
  private <T> T convertSourceToObject(Class<T> clazz, String source) {
    return objectMapper.readValue(source, clazz);
  }
}
