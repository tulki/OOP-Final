package Services;

import Actors.User;
import Models.ResearchPaper;
import Models.ResearchProject;
import Exceptions.NotAResearcherException;
import Interfaces.IResearcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ResearchService implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final String DATA_FILE = "data/research.dat";

    private List<ResearchProject> projects;
    private List<IResearcher> registeredResearchers;
    private transient UserRepository userRepository;

    public ResearchService() {
        this(null);
    }

    public ResearchService(UserRepository userRepository) {
        this.projects = new ArrayList<>();
        this.registeredResearchers = new ArrayList<>();
        this.userRepository = userRepository;
    }

    public static ResearchService load(UserRepository userRepository) {
        File file = new File(DATA_FILE);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                ResearchService service = (ResearchService) ois.readObject();
                service.setUserRepository(userRepository);
                return service;
            } catch (Exception e) {
                System.err.println("Failed to load research.dat, starting fresh: " + e.getMessage());
            }
        }
        return new ResearchService(userRepository);
    }

    public void save() {
        new File("data").mkdirs();
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            oos.writeObject(this);
        } catch (IOException e) {
            System.err.println("Failed to save research.dat: " + e.getMessage());
        }
    }

    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
        if (projects == null) projects = new ArrayList<>();
        if (registeredResearchers == null) registeredResearchers = new ArrayList<>();
    }

    public void registerResearcher(IResearcher researcher) {
        if (researcher == null || !researcher.isResearcher()) {
            throw new NotAResearcherException("Only active researchers can be registered");
        }
        if (!registeredResearchers.contains(researcher)) registeredResearchers.add(researcher);
    }

    public void addProject(ResearchProject project) {
        if (project != null && findProjectById(project.getProjectId()) == null) projects.add(project);
    }

    public ResearchProject findProjectById(String projectId) {
        return projects.stream()
                .filter(p -> p.getProjectId().equalsIgnoreCase(projectId))
                .findFirst().orElse(null);
    }

    public void joinProject(String projectId, String username) {
        ResearchProject project = findProjectById(projectId);
        if (project == null) throw new IllegalArgumentException("Project not found: " + projectId);

        IResearcher researcher = findResearcherByUsername(username);
        if (researcher == null) {
            throw new NotAResearcherException(username + " is not an active researcher");
        }

        project.addParticipantUsername(username);
        researcher.addResearchProjectId(projectId);
    }

    public IResearcher findResearcherByUsername(String username) {
        if (username == null) return null;

        if (userRepository != null) {
            return userRepository.findByUsername(username)
                    .filter(user -> user instanceof IResearcher)
                    .map(user -> (IResearcher) user)
                    .filter(IResearcher::isResearcher)
                    .orElse(null);
        }

        for (IResearcher researcher : registeredResearchers) {
            if (researcher instanceof User user
                    && user.getUsername().equalsIgnoreCase(username)
                    && researcher.isResearcher()) {
                return researcher;
            }
        }
        return null;
    }

    public List<ResearchPaper> getAllPapers() {
        Map<String, ResearchPaper> unique = new LinkedHashMap<>();
        for (IResearcher researcher : getAllResearchers()) {
            for (ResearchPaper paper : researcher.getPapers()) {
                unique.putIfAbsent(paperKey(paper), paper);
            }
        }
        for (ResearchProject project : projects) {
            for (ResearchPaper paper : project.getPublishedPapers()) {
                unique.putIfAbsent(paperKey(paper), paper);
            }
        }
        return new ArrayList<>(unique.values());
    }

    public void printAllPapersSorted(Comparator<ResearchPaper> comparator) {
        List<ResearchPaper> all = getAllPapers();
        all.sort(comparator);
        if (all.isEmpty()) {
            System.out.println("No papers found.");
            return;
        }
        all.forEach(System.out::println);
    }

    public void printTopCitedResearcherBySchool(String school) {
        IResearcher top = null;
        int topCitations = -1;

        for (IResearcher researcher : getAllResearchers()) {
            String researcherSchool = researcher.getResearchSchool();
            if (school != null && !school.isBlank()
                    && (researcherSchool == null || !researcherSchool.equalsIgnoreCase(school))) {
                continue;
            }

            int citations = totalCitations(researcher.getPapers(), null);
            if (citations > topCitations) {
                top = researcher;
                topCitations = citations;
            }
        }

        if (top == null) {
            System.out.println("No researchers found for school: " + school);
            return;
        }
        System.out.printf("Top cited researcher: %s | citations: %d | h-index: %d%n",
                describeResearcher(top), topCitations, top.getHIndex());
    }

    public void printTopCitedResearcherOfYear(int year) {
        IResearcher top = null;
        int topCitations = -1;

        for (IResearcher researcher : getAllResearchers()) {
            int citations = totalCitations(researcher.getPapers(), year);
            if (citations > topCitations) {
                top = researcher;
                topCitations = citations;
            }
        }

        if (top == null || topCitations <= 0) {
            System.out.println("No cited researchers found for year: " + year);
            return;
        }
        System.out.printf("Top cited researcher of %d: %s | citations: %d | h-index: %d%n",
                year, describeResearcher(top), topCitations, top.getHIndex());
    }

    public List<IResearcher> getAllResearchers() {
        Map<String, IResearcher> unique = new LinkedHashMap<>();

        if (userRepository != null) {
            for (User user : userRepository.getAllUsers()) {
                if (user instanceof IResearcher researcher && researcher.isResearcher()) {
                    unique.put(user.getUsername().toLowerCase(), researcher);
                }
            }
        }

        for (IResearcher researcher : registeredResearchers) {
            if (researcher != null && researcher.isResearcher()) {
                unique.putIfAbsent(researcherKey(researcher), researcher);
            }
        }

        return new ArrayList<>(unique.values());
    }

    public List<ResearchProject> getAllProjects() {
        return new ArrayList<>(projects);
    }

    private String paperKey(ResearchPaper paper) {
        if (paper.getDoi() != null && !paper.getDoi().isBlank()) {
            return "doi:" + paper.getDoi().toLowerCase();
        }
        return "paper:" + paper.getTitle().toLowerCase() + ":" + paper.getDatePublished();
    }

    private int totalCitations(List<ResearchPaper> papers, Integer year) {
        int total = 0;
        for (ResearchPaper paper : papers) {
            if (year == null
                    || (paper.getDatePublished() != null && paper.getDatePublished().getYear() == year)) {
                total += paper.getCitations();
            }
        }
        return total;
    }

    private String researcherKey(IResearcher researcher) {
        if (researcher instanceof User user) return user.getUsername().toLowerCase();
        return "external:" + System.identityHashCode(researcher);
    }

    private String describeResearcher(IResearcher researcher) {
        if (researcher instanceof User user) {
            return user.getFullName() + " (" + user.getUsername() + ")";
        }
        return researcher.toString();
    }
}

