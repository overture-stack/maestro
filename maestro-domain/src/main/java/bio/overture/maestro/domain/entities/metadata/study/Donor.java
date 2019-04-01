package bio.overture.maestro.domain.entities.metadata.study;

import bio.overture.maestro.domain.entities.indexing.rules.ExclusionId;
import lombok.*;

import java.util.Map;

/**
 * This represents the sample donor
 * A donor is part of the {@link Sample} entity.
 */
@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Donor {
    /**
     * The id of this donor
     */
    @NonNull
    @ExclusionId
    private String donorId;

    /**
     * the id as submitted by the analysis creator
     */
    @NonNull
    private String donorSubmitterId;

    /**
     * the study which this donor belongs to
     */
    @NonNull
    private String studyId;

    /**
     * can be Female / Male
     */
    @NonNull
    private String donorGender;

    /**
     * for extra information if any.
     */
    private Map<String, Object> info;
}
