package bio.overture.maestro.domain.utility;

import bio.overture.maestro.domain.api.exception.IndexerException;
import lombok.experimental.UtilityClass;
import org.slf4j.Logger;

@UtilityClass
public class ErrorHandlers {
    public static Throwable logAndReturn(Throwable e, String message, Logger logger) {
        logger.error(message, e);
        if (e instanceof IndexerException) return e;
        return new IndexerException(message, e);
    }
}
