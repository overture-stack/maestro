package bio.overture.masestro.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.val;

import java.io.File;
import java.util.Optional;

import static bio.overture.maestro.domain.utility.StringUtilities.inputStreamToString;

@UtilityClass
public class Fixture {

    private final static String BASE_PATH = "fixtures" + File.separator;
    private final static ObjectMapper MAPPER = new ObjectMapper();

    @SneakyThrows
    public static <T> T loadJsonFixture(Class clazz, String fileName, Class<T> targetClass) {
        val json = inputStreamToString(
            Optional.ofNullable(clazz.getClassLoader()
                .getResource(BASE_PATH + clazz.getSimpleName() + File.separator + fileName)
            )
            .orElseThrow()
            .openStream()
        );
        return MAPPER.readValue(json, targetClass);
    }

}
