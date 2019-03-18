package bio.overture.maestro.domain.entities.metadata.study;

import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * This corresponds to the analysis entity in the file metadata repository.
 *
 */
@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Analysis {
    @NonNull
    private String analysisId;

    /**
     * Method used in this analysis (variantCall, sequenceRead)
     */
    @NonNull
    private String analysisType;

    /**
     * the status of the analysis (published or other values)
     */
    @NonNull
    private String analysisState;

    /**
     * the study Id that this analysis belongs to.
     */
    @NonNull
    private String study;

    /**
     * map of extra attributes that belong to this analysis
     */
    private Map<String, Object> info;

    /**
     * multiple files belong to an analysis, files can be related (bam, bai, xml)
     */
    @NonNull
    private List<File> file;

    /**
     * An analysis can have one or more samples
     */
    @NonNull
    private List<Sample> sample;

    /**
     * extra information about the analysis type.
     * this will contain attributes that change by the analysis type.
     *
     * see the source repository api for more info if needed.
     */
    private Map<String, Object> experiment;
}
