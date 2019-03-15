package bio.overture.maestro.domain.api.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class NotFoundException extends IndexerException {
    public NotFoundException(String message) {
        super(message);
    }
}
