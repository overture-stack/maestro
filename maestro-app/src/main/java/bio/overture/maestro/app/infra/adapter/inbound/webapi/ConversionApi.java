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

package bio.overture.maestro.app.infra.adapter.inbound.webapi;

import bio.overture.maestro.domain.api.Converter;
import bio.overture.maestro.domain.api.message.*;
import bio.overture.maestro.domain.entities.indexing.FileCentricDocument;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
public class ConversionApi {
  private final Converter converter;

  @Inject
  public ConversionApi(Converter converter) {
    this.converter = converter;
  }

  @PostMapping("/convert")
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<Map<String, List<FileCentricDocument>>> convertAnalyses(
      @RequestBody ConvertAnalysisCommand convertAnalysisCommand) {
    return converter.convertAnalysesToFileDocuments(convertAnalysisCommand);
  }
}
