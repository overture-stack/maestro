package bio.overture.maestro.domain.entities.indexing.rules;

public abstract class ExclusionRule {
    abstract public <T> boolean applies(T instance);
}
