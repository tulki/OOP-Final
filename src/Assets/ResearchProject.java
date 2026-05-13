package Assets;

import Actors.User;
import Exceptions.NotAResearcherException;
import Interfaces.IResearcher;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ResearchProject implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String projectId;
    private String topic;
    private List<ResearchPaper> publishedPapers;
    private List<String> participantUsernames;

    public ResearchProject(String projectId, String topic) {
        this.projectId = projectId;
        this.topic = topic;
        this.publishedPapers = new ArrayList<>();
        this.participantUsernames = new ArrayList<>();
    }

    public void addParticipant(Object person) {
        if (!(person instanceof IResearcher researcher) || !researcher.isResearcher()) {
            throw new NotAResearcherException(
                    person + " cannot join ResearchProject: not a Researcher");
        }
        if (!(person instanceof User user)) {
            throw new NotAResearcherException(
                    person + " cannot join ResearchProject: not a system user");
        }
        addParticipantUsername(user.getUsername());
        researcher.addResearchProjectId(projectId);
    }

    public void addParticipantUsername(String username) {
        if (username != null && !participantUsernames.contains(username)) {
            participantUsernames.add(username);
        }
    }

    public void addPaper(ResearchPaper paper) { publishedPapers.add(paper); }

    public String getProjectId() { return projectId; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public List<ResearchPaper> getPublishedPapers() { return publishedPapers; }
    public List<String> getParticipantUsernames() { return participantUsernames; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResearchProject)) return false;
        return projectId.equals(((ResearchProject) o).projectId);
    }

    @Override
    public int hashCode() { return projectId.hashCode(); }

    @Override
    public String toString() {
        return String.format("[%s] %s - %d participant(s), %d paper(s)",
                projectId, topic, participantUsernames.size(), publishedPapers.size());
    }
}
