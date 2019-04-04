package bio.overture.maestro.domain.entities.indexing.rules;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A metadata annotation to indicate a field annotated with this
 * qualifies to be processed by {@link IDExclusionRule}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface ExclusionId { }
