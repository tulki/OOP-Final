package Models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ResearchProfile implements Serializable {
    private static final long serialVersionUID = 1L;

    private int hIndex;
    private String school;
    private List<ResearchPaper> papers;
    private List<String> projectIds;

    public ResearchProfile(int hIndex, String school) {
        setHIndex(hIndex);
        this.school = school;
        this.papers = new ArrayList<>();
        this.projectIds = new ArrayList<>();
    }

    public int getHIndex() { return hIndex; }
    public String getSchool() { return school; }
    public List<ResearchPaper> getPapers() { return papers; }
    public List<String> getProjectIds() { return projectIds; }

    public void setHIndex(int hIndex) {
        if (hIndex < 0) throw new IllegalArgumentException("h-index cannot be negative");
        this.hIndex = hIndex;
    }

    public void setSchool(String school) { this.school = school; }

    public void addPaper(ResearchPaper paper) {
        if (paper != null && !papers.contains(paper)) papers.add(paper);
    }

    public void addProjectId(String projectId) {
        if (projectId != null && !projectIds.contains(projectId)) projectIds.add(projectId);
    }

    public void printPapers(Comparator<ResearchPaper> comparator) {
        List<ResearchPaper> sorted = new ArrayList<>(papers);
        sorted.sort(comparator);
        if (sorted.isEmpty()) {
            System.out.println("No papers found.");
            return;
        }
        sorted.forEach(System.out::println);
    }

    @Override
    public String toString() {
        return String.format("ResearchProfile{hIndex=%d, school='%s', papers=%d, projects=%d}",
                hIndex, school, papers.size(), projectIds.size());
    }
}

