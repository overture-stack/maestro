package bio.overture.maestro.domain.entities.indexer;

import lombok.*;

import java.util.List;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class FileCentricDocument {
    private String objectId;
    private String access;
    private List<String> study;
    private FileCentricAnalysis analysis;
    private List<FileCopy> fileCopies;
    private List<FileCentricDonor> donors;
}
