package bio.overture.maestro.domain.api;

import lombok.Getter;

@Getter
public enum NotificationName {
    STUDY_ANALYSES_FETCH_FAILED(NotificationCategory.ERROR),
    FETCH_REPO_STUDIES_FAILED(NotificationCategory.ERROR),
    INDEX_REQ_FAILED(NotificationCategory.ERROR),
    CONVERT_ANALYSIS_TO_FILE_DOCS_FAILED(NotificationCategory.ERROR),
    ALL(null),
    ;

    private NotificationCategory category;

    NotificationName(NotificationCategory category) {
        this.category = category;
    }

}
