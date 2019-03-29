package bio.overture.maestro.app.infra.config.properties;

public interface PropertiesFileRepository {
    String getName();

    String getCode();

    String getUrl();

    String getDataPath();

    String getMetadataPath();

    String getOrganization();

    String getCountry();

    bio.overture.maestro.domain.entities.indexing.StorageType getStorageType();
}
