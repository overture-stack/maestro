package bio.overture.maestro.domain.message.out;


import bio.overture.maestro.domain.message.in.Donor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class FileDocument {
    private String id;
    private String objectId;
    private String access;
    private List<String> study;
    private DataCategorization dataCategorization;
    private DataBundle dataBundle;
    private List<FileCopy> fileCopies;
    private List<Donor> donors;
    private ReferenceGenome referenceGenome;
    private AnalysisMethod analysisMethod;
}
