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

package bio.overture.maestro.domain.entities.metadata.study;

import bio.overture.maestro.domain.entities.indexing.rules.ExclusionId;
import lombok.*;

import java.util.Map;

/**
 * Many samples can belong to an Analysis, a samples represents
 * a donor and a specimen composition.
 */
@Getter
@Builder
@ToString
@NoArgsConstructor
@EqualsAndHashCode
@AllArgsConstructor
public class Sample {
    @ExclusionId
    private String sampleId;
    private String specimenId;
    private String submitterSampleId;
    private String matchedNormalSubmitterSampleId;
    private String sampleType;
    private Donor donor;
    private Specimen specimen;
    private Map<String, Object> info;
}
