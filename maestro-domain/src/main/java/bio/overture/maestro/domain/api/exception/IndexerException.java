package bio.overture.maestro.domain.api.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class IndexerException extends RuntimeException {
    public IndexerException(String message) {
        super(message);
    }

    public IndexerException(Throwable cause) {
        super(cause);
    }

    public IndexerException(String message, Throwable cause) {
        super(message, cause);
    }
}
