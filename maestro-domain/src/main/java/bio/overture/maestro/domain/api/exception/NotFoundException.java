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

package bio.overture.maestro.domain.api.exception;

import lombok.NoArgsConstructor;
import lombok.NonNull;

import static java.lang.String.format;

/**
 * Indicates a required / requested resource / data is missing.
 */
@NoArgsConstructor
public class NotFoundException extends IndexerException {
    public NotFoundException(String message) {
        super(message);
    }

    public static NotFoundException buildNotFoundException(
            @NonNull String formattedMessage, Object... args) {
        return new NotFoundException(format(formattedMessage, args));
    }

    public static void checkNotFound(
            boolean expression, @NonNull String formattedMessage, @NonNull Object... args) {
        if (!expression) {
            throw new NotFoundException(format(formattedMessage, args));
        }
    }
}
