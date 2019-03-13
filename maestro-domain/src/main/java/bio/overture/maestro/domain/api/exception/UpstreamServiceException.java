package bio.overture.maestro.domain.api.exception;

public class UpstreamServiceException extends IndexerException {
    public UpstreamServiceException(String message, Throwable e) {
        super(message, e);
    }
}
