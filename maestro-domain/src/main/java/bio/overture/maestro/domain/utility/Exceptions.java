package bio.overture.maestro.domain.utility;

import bio.overture.maestro.domain.api.exception.BadDataException;
import bio.overture.maestro.domain.api.exception.IndexerException;
import bio.overture.maestro.domain.api.exception.NotFoundException;
import lombok.experimental.UtilityClass;

import static java.text.MessageFormat.format;

@UtilityClass
public class Exceptions {


    public static Exception notFound(String msg, Object ...args) {
        return new NotFoundException(format(msg, args));
    }

    public static Exception badData(String msg, Object ...args) {
        return new BadDataException(format(msg, args));
    }

    public static Throwable wrapWithIndexerException(Throwable e, String message) {
        if (e instanceof IndexerException) return e;
        return new IndexerException(message, e);
    }
}
