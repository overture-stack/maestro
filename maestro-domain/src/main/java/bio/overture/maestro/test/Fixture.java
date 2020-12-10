/*
 *  Copyright (c) 2019. Ontario Institute for Cancer Research
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as
 *   published by the Free Software Foundation, either version 3 of the
 *   License, or (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package bio.overture.maestro.test;

import static bio.overture.maestro.domain.utility.StringUtilities.inputStreamToString;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import lombok.*;
import lombok.experimental.UtilityClass;

/**
 * Helper to load test fixtures from resources. This class is only for testing purposes not to be
 * used for non test code.
 */
@UtilityClass
public class Fixture {

  private static final String FIXTURE_PATH = "src/test/resources/";
  private static final String BASE_PATH = "fixtures" + File.separator;
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @SneakyThrows
  public static <T> T loadJsonFixture(Class clazz, String fileName, Class<T> targetClass) {
    return loadJsonFixture(clazz, fileName, targetClass, MAPPER);
  }

  public static <T> T loadConverterTestFixture(String fileName, Class<T> targetClass) {
    return readConverterFixture(fileName, targetClass, MAPPER);
  }

  @SneakyThrows
  public static <T> T loadJsonFixtureSnakeCase(Class clazz, String fileName, Class<T> targetClass) {
    ObjectMapper mapper =
        new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
    return loadJsonFixture(clazz, fileName, targetClass, mapper);
  }

  /** Use this overload for generics */
  @SneakyThrows
  public static <T> T loadJsonFixture(Class clazz, String fileName, TypeReference<T> type) {
    return loadJsonFixture(clazz, fileName, type, MAPPER);
  }
  /**
   * this overload can be used to load json files and convert it to the target class using a custom
   * mapper
   *
   * @param clazz will be used to obtain a class loader to get the resources for.
   * @param fileName the fixture files we want to load
   * @param targetClass the target java type we want to convert the json to
   * @param customMapper in case you want to pre configure a mapper (property name case for example)
   * @param <T> type parameter of the target class
   * @param templateParams parameters map to be replaced in the json files use this if you have
   *     dynamic values that change each test run the placeholder should be ##key## and will be
   *     replaced with the value : templateParams.get(key)
   * @return the converted json files as java type
   */
  @SneakyThrows
  public static <T> T loadJsonFixture(
      Class clazz,
      String fileName,
      Class<T> targetClass,
      ObjectMapper customMapper,
      Map<String, String> templateParams) {
    String json = loadJsonString(clazz, fileName);
    TemplateResult replaceResult = new TemplateResult();
    replaceResult.setResult(json);
    templateParams.forEach(
        (name, value) ->
            replaceResult.setResult(
                replaceResult.getResult().replaceAll(Pattern.quote("##" + name + "##"), value)));

    return customMapper.readValue(replaceResult.getResult(), targetClass);
  }

  /**
   * this overload can be used to load json files and convert it to the target class using a custom
   * mapper
   *
   * @param clazz will be used to obtain a class loader to get the resources for.
   * @param fileName the fixture files we want to load
   * @param targetClass the target java type we want to convert the json to
   * @param customMapper in case you want to pre configure a mapper (property name case for example)
   * @param <T> type parameter of the target class
   * @return the converted json files as java type
   */
  @SneakyThrows
  public static <T> T loadJsonFixture(
      Class clazz, String fileName, Class<T> targetClass, ObjectMapper customMapper) {
    String json = loadJsonString(clazz, fileName);
    return customMapper.readValue(json, targetClass);
  }

  @SneakyThrows
  public static <T> T loadJsonFixture(
      Class clazz, String fileName, TypeReference<T> targetClass, ObjectMapper customMapper) {
    String json = loadJsonString(clazz, fileName);
    return customMapper.readValue(json, targetClass);
  }

  @SneakyThrows
  public static <T> T readConverterFixture(
      String fileName, Class<T> targetClass, ObjectMapper customMapper) {
    return customMapper.readValue(
        new File(FIXTURE_PATH + BASE_PATH + "DocumentConverterTest" + File.separator + fileName),
        targetClass);
  }

  public static String loadJsonString(Class clazz, String fileName) throws IOException {
    val path = BASE_PATH + clazz.getSimpleName() + File.separator + fileName;
    return inputStreamToString(
        Optional.ofNullable(clazz.getClassLoader().getResource(path))
            .orElseThrow(
                () ->
                    new RuntimeException(
                        "fixture not found in path "
                            + path
                            + ". make sure you created the correct "
                            + "folder if this is a new class or if you renamed the class"))
            .openStream());
  }

  @Getter
  @Setter
  @NoArgsConstructor
  private static class TemplateResult {
    private String result;
  }
}
