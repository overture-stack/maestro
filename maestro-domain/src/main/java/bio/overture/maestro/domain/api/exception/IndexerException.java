package bio.overture.maestro.domain.api.exception;

import lombok.*;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
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
