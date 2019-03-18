package bio.overture.maestro.domain.utility;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.val;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

@UtilityClass
public final class StringUtilities {

    /**
     * loads a string out of input stream.
     */
    @SneakyThrows
    public static String inputStreamToString(InputStream inputStream) {
        try (ByteArrayOutputStream result = new ByteArrayOutputStream()) {
            val buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            return result.toString(UTF_8);
        }
    }

}
