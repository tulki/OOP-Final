package Interfaces;

import Models.ResearchPaper;
import Models.ResearchProfile;
import Exceptions.NotAResearcherException;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Marks an entity as capable of having a research profile.
 *
 * <p>Implemented by {@link Actors.Student}, {@link Actors.Employee} (and all its
 * subclasses). A {@code null} {@link Models.ResearchProfile} means the entity is
 * <em>not</em> an active researcher; setting a non-null profile activates research
 * capabilities. Default methods provide safe delegating accessors that return
 * neutral values when the profile is absent, so callers never need null-checks.
 *
 * <p>Design note: chosen as an interface (rather than an abstract class or
 * Decorator) to allow orthogonal mix-in across the existing inheritance hierarchy
 * — both {@code Student} and {@code Employee} subtypes can be researchers without
 * forced single-inheritance constraints.
 */
public interface IResearcher {

    /** Returns this entity's research profile, or {@code null} if not a researcher. */
    ResearchProfile getResearchProfile();

    /** Activates (non-null) or deactivates (null) researcher status. */
    void setResearchProfile(ResearchProfile researchProfile);

    /** Returns {@code true} if this entity has an active research profile. */
    default boolean isResearcher() {
        return getResearchProfile() != null;
    }

    /**
     * Asserts that this entity is an active researcher.
     *
     * @throws NotAResearcherException if {@link #isResearcher()} is {@code false}
     */
    default void requireResearcher() {
        if (!isResearcher()) {
            if (this instanceof Actors.User user) throw new NotAResearcherException(user.getUsername());
            throw new NotAResearcherException();
        }
    }

    /** Returns the list of published papers, or an empty list if not a researcher. */
    default List<ResearchPaper> getPapers() {
        return isResearcher() ? getResearchProfile().getPapers() : Collections.emptyList();
    }

    /** Returns the h-index, or {@code 0} if not a researcher. */
    default int getHIndex() {
        return isResearcher() ? getResearchProfile().getHIndex() : 0;
    }

    /** Returns the affiliated school/department, or {@code null} if not a researcher. */
    default String getResearchSchool() {
        return isResearcher() ? getResearchProfile().getSchool() : null;
    }

    /** Returns IDs of research projects this entity participates in. */
    default List<String> getProjectIds() {
        return isResearcher() ? getResearchProfile().getProjectIds() : Collections.emptyList();
    }

    /**
     * Adds a paper to this researcher's publication list.
     *
     * @throws NotAResearcherException if not an active researcher
     */
    default void addResearchPaper(ResearchPaper paper) {
        requireResearcher();
        getResearchProfile().addPaper(paper);
    }

    /**
     * Records participation in a research project by its ID.
     *
     * @throws NotAResearcherException if not an active researcher
     */
    default void addResearchProjectId(String projectId) {
        requireResearcher();
        getResearchProfile().addProjectId(projectId);
    }

    /**
     * Prints all papers sorted by the given comparator.
     *
     * @param comparator sort order (e.g. by citations, date, or page count)
     * @throws NotAResearcherException if not an active researcher
     */
    default void printPapers(Comparator<ResearchPaper> comparator) {
        requireResearcher();
        getResearchProfile().printPapers(comparator);
    }
}

