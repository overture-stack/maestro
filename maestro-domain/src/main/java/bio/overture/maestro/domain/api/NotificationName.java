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

import lombok.Getter;

@Getter
public enum NotificationName {
  STUDY_ANALYSES_FETCH_FAILED(NotificationCategory.ERROR),
  FETCH_REPO_STUDIES_FAILED(NotificationCategory.ERROR),
  INDEX_REQ_FAILED(NotificationCategory.ERROR),
  CONVERT_ANALYSIS_TO_FILE_DOCS_FAILED(NotificationCategory.ERROR),
  CONVERT_ANALYSIS_TO_ANALYSIS_DOCS_FAILED(NotificationCategory.ERROR),
  INDEX_FILE_CONFLICT(NotificationCategory.WARN),
  ALL(null),
  UNHANDLED_ERROR(NotificationCategory.ERROR),
  FAILED_TO_FETCH_ANALYSIS(NotificationCategory.ERROR),
  FAILED_TO_FETCH_REPOSITORY(NotificationCategory.ERROR),
  FAILED_TO_REMOVE_ANALYSIS(NotificationCategory.ERROR);

  private final NotificationCategory category;

  NotificationName(NotificationCategory category) {
    this.category = category;
  }
}
