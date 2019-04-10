package bio.overture.maestro.domain.api.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class IndexerException extends RuntimeException {
    protected FailureData failureData;
    public IndexerException(String message) {
        super(message);
    }
    public IndexerException(String message, Throwable cause, FailureData failureData) {
        super(message, cause);
        this.failureData = failureData;
    }
}
