package bio.overture.maestro.domain.entities.indexing;

import lombok.*;

import java.util.Map;

@Builder
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class File {
    @NonNull
    private String name;
    @NonNull
    private String format;
    @NonNull
    private String md5sum;
    private Long size;
    private Long lastModified;
    private IndexFile indexFile;
    private Map<String, Object> info;
}
