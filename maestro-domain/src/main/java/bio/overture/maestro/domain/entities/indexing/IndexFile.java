package bio.overture.maestro.domain.entities.indexing;

import lombok.*;

@Builder
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class IndexFile {
    private String objectId;
    private String name;
    private String format;
    private String md5sum;
    private Long size;
}
