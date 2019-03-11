package bio.overture.maestro.domain.entities;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class FileCentricDocument {
    private String id;
    private String objectId;
    private String access;
    private List<String> study;
    private FileCentricAnalysis analysis;
    private List<FileCopy> fileCopies;
    private List<FileCentricDonor> donors;
}
