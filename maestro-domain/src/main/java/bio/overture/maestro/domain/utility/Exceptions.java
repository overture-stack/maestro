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

package bio.overture.maestro.domain.utility;

import static java.text.MessageFormat.format;

import bio.overture.maestro.domain.api.exception.BadDataException;
import bio.overture.maestro.domain.api.exception.FailureData;
import bio.overture.maestro.domain.api.exception.IndexerException;
import bio.overture.maestro.domain.api.exception.NotFoundException;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class Exceptions {

  public static Exception notFound(String msg, Object... args) {
    return new NotFoundException(format(msg, args));
  }

  public static Exception badData(String msg, Object... args) {
    return new BadDataException(format(msg, args));
  }

  public static IndexerException wrapWithIndexerException(
      Throwable e, String message, FailureData failureData) {
    if (e instanceof IndexerException) return (IndexerException) e;
    return new IndexerException(message, e, failureData);
  }
}
