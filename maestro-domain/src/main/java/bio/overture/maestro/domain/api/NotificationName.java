package bio.overture.maestro.domain.api;

import lombok.Getter;

@Getter
public enum NotificationName {
    STUDY_ANALYSES_FETCH_FAILED(NotificationCategory.ERROR),
    FETCH_REPO_STUDIES_FAILED(NotificationCategory.ERROR),
    INDEX_REQ_FAILED(NotificationCategory.ERROR),
    CONVERT_ANALYSIS_TO_FILE_DOCS_FAILED(NotificationCategory.ERROR),
    INDEX_FILE_CONFLICT(NotificationCategory.WARN),
    ALL(null),
    UNHANDLED_ERROR(NotificationCategory.ERROR),
    FAILED_TO_FETCH_ANALYSIS(NotificationCategory.ERROR),
    FAILED_TO_FETCH_REPOSITORY(NotificationCategory.ERROR),
    FAILED_TO_REMOVE_ANALYSIS(NotificationCategory.ERROR);

    private final NotificationCategory category;

    NotificationName(NotificationCategory category) {
        this.category = category;
    }

}
