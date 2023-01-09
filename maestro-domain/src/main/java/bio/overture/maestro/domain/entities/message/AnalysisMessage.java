package bio.overture.maestro.domain.entities.message;

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

import bio.overture.maestro.domain.entities.metadata.study.Analysis;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Value
@Builder
@ToString
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnalysisMessage {
  @NonNull private final String analysisId;
  @NonNull private final String studyId;
  @NonNull private final String state;
  @NonNull private final String songServerId;
  @NonNull private final Analysis analysis;
}
