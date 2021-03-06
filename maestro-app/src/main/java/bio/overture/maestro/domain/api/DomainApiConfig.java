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

package bio.overture.maestro.domain.api;

import bio.overture.maestro.app.infra.config.properties.ApplicationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
  IndexerConfig.class,
  Converter.class,
  Notifier.class,
})
public class DomainApiConfig {

  @Bean
  IndexProperties indexEnabled(@Autowired ApplicationProperties applicationProperties) {
    return new IndexPropertiesImpl.IndexPropertiesImplBuilder()
        .isAnalysisCentricEnabled(applicationProperties.isAnalysisCentricIndexEnabled())
        .isFileCentricEnabled(applicationProperties.isFileCentricIndexEnabled())
        .analysisCentricIndexName(applicationProperties.analysisCentricIndexName())
        .fileCentricIndexName(applicationProperties.fileCentricIndexName())
        .build();
  }
}

@ConditionalOnProperty(name = "maestro.disableIndexing", havingValue = "false")
@Configuration
@Import({
  DefaultIndexer.class,
})
class IndexerConfig {}
