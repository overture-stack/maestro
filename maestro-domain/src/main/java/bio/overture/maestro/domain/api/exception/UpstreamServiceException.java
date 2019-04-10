package bio.overture.maestro.domain.api.exception;

/**
 * This means that a service that this indexer depends on have thrown an error
 */
public class UpstreamServiceException extends IndexerException {
    public UpstreamServiceException(String message, Throwable e, FailureData failureData) {
        super(message, e, failureData);
    }
}
