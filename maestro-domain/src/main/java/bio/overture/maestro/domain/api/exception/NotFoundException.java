package bio.overture.maestro.domain.api.exception;

import lombok.NoArgsConstructor;

/**
 * Indicates a required / requested resource / data is missing.
 */
@NoArgsConstructor
public class NotFoundException extends IndexerException {
    public NotFoundException(String message) {
        super(message);
    }
}
