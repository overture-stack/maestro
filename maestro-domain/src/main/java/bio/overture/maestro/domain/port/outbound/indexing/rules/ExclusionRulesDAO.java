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

package bio.overture.maestro.domain.port.outbound.indexing.rules;

import bio.overture.maestro.domain.entities.indexing.rules.ExclusionRule;
import java.util.List;
import java.util.Map;
import reactor.core.publisher.Mono;

public interface ExclusionRulesDAO {
  Mono<Map<Class<?>, List<? extends ExclusionRule>>> getExclusionRules();
}
