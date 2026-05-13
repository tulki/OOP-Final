package Interfaces;

import Assets.ResearchPaper;
import Assets.ResearchProfile;
import Exceptions.NotAResearcherException;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public interface IResearcher {
    ResearchProfile getResearchProfile();
    void setResearchProfile(ResearchProfile researchProfile);

    default boolean isResearcher() {
        return getResearchProfile() != null;
    }

    default void requireResearcher() {
        if (!isResearcher()) {
            throw new NotAResearcherException(this + " is not an active researcher");
        }
    }

    default List<ResearchPaper> getPapers() {
        return isResearcher() ? getResearchProfile().getPapers() : Collections.emptyList();
    }

    default int getHIndex() {
        return isResearcher() ? getResearchProfile().getHIndex() : 0;
    }

    default String getResearchSchool() {
        return isResearcher() ? getResearchProfile().getSchool() : null;
    }

    default List<String> getProjectIds() {
        return isResearcher() ? getResearchProfile().getProjectIds() : Collections.emptyList();
    }

    default void addResearchPaper(ResearchPaper paper) {
        requireResearcher();
        getResearchProfile().addPaper(paper);
    }

    default void addResearchProjectId(String projectId) {
        requireResearcher();
        getResearchProfile().addProjectId(projectId);
    }

    default void printPapers(Comparator<ResearchPaper> comparator) {
        requireResearcher();
        getResearchProfile().printPapers(comparator);
    }
}
