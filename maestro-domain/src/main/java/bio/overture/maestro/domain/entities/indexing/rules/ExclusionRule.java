package bio.overture.maestro.domain.entities.indexing.rules;

/**
 * A generic extendable rule to indicate if a rule applies to an instance.
 */
public abstract class ExclusionRule {
    abstract public boolean applies(Object instance);
}
