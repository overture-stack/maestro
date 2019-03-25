package bio.overture.maestro.domain.entities.indexer;

import lombok.*;

@Builder
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class File {
    private String name;
    private String format;
    private String md5sum;
    private Long size;
    private Long lastModified;
    private IndexFile indexFile;
}
