package bio.overture.maestro.domain.api.exception;

/**
 * This exception is to indicate that a data processing issue happened.
 */
public class BadDataException extends IndexerException {
    public BadDataException(String message) {
        super(message);
    }
}
