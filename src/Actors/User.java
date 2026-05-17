package Actors;

import Models.Message;
import Models.News;
import Models.ResearchPaper;
import Models.ResearchProfile;
import Models.ResearchProject;
import Interfaces.IResearcher;
import Services.MenuHelper;
import Services.Services;

import java.time.LocalDate;
import java.util.Arrays;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;


/**
 * Abstract base class for every account in the university system.
 *
 * <p>Encapsulates identity (username, full name) and credentials (SHA-256
 * password hash). Subclasses represent concrete roles: {@link Admin},
 * {@link Student}, {@link Teacher}, {@link Manager}, {@link Dean},
 * {@link Rector}.
 *
 * <p>The {@link #showMenu(java.util.Scanner, Services.Services)} method
 * implements the <em>Template Method</em> pattern: it drives the common
 * menu loop and delegates role-specific items to the abstract methods
 * {@link #printRoleSpecificMenu()} and
 * {@link #handleRoleSpecificChoice(String, java.util.Scanner, Services.Services)}.
 *
 * <p>Common functionality available to all users regardless of role:
 * profile management, news viewing, researcher directory, research paper
 * search, peer-to-peer messaging, and full-text public search.
 *
 * <p>Research operations ({@link #manageResearch}, publish paper, create/join
 * project, view projects, update h-index) are also implemented here so that
 * both {@link Student} and {@link Employee} subtypes share one code path
 * without duplication. Methods cast {@code this} to {@link Interfaces.IResearcher}
 * at runtime; this is always safe because they are only reachable when
 * {@link Interfaces.IResearcher#isResearcher()} returns {@code true}.
 * Researcher status is granted and revoked exclusively by {@link Admin}.
 *
 * <p>Equality and hashing are based solely on {@code username}.
 */
public abstract class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String username;
    private String passwordHash;
    private String fullName;

    public User(String username, String passwordHash, String fullName) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
    }

    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public boolean verifyPassword(String plainInput) {
        return hash(plainInput).equals(passwordHash);
    }

    public void changePassword(String newPlain) {
        this.passwordHash = hash(newPlain);
    }

    private String hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public final void showMenu(Scanner in, Services services) {
        while (true) {
            int unread = services.getMessageService().unreadCount(username);
            String unreadTag = unread > 0 ? " (" + unread + " unread)" : "";

            System.out.println("\n=== Menu [" + getClass().getSimpleName() + ": " + username + "] ===");
            System.out.println("  p  - Manage profile");
            System.out.println("  n  - View news");
            System.out.println("  r  - View researchers");
            System.out.println("  rp - View research papers");
            System.out.println("  s  - Search public information");
            System.out.println("  m  - Manage messages" + unreadTag);
            System.out.println("  0  - Logout");
            printRoleSpecificMenu();
            System.out.print("> ");

            String choice = in.nextLine().trim().toLowerCase();

            switch (choice) {
                case "p" -> manageProfile(in, services);
                case "n" -> viewNews(services);
                case "r" -> viewResearchers(in, services);
                case "rp" -> viewResearchPapers(in, services);
                case "s" -> searchPublicInfo(in, services);
                case "m" -> manageMessages(in, services);
                case "0" -> { return; }
                default -> {
                    boolean handled = handleRoleSpecificChoice(choice, in, services);
                    if (!handled) System.out.println("Unknown option. Try again.");
                }
            }
        }
    }

    protected void manageProfile(Scanner in, Services services) {
        while (true) {
            System.out.println("\n=== Profile ===");
            System.out.println("Username: " + username);
            System.out.println("Full name: " + fullName);
            if (this instanceof IResearcher researcher) {
                System.out.println("Researcher: " + (researcher.isResearcher() ? "yes" : "no"));
                if (researcher.isResearcher()) {
                    System.out.println("h-index: " + researcher.getHIndex());
                    System.out.println("School: " + researcher.getResearchSchool());
                }
            }
            System.out.println("  1 - Change full name");
            System.out.println("  2 - Change password");
            System.out.println("  0  - Back");
            System.out.print("> ");

            String choice = in.nextLine().trim();
            switch (choice) {
                case "1" -> {
                    System.out.print("New full name: ");
                    setFullName(in.nextLine().trim());
                    services.saveAll();
                    System.out.println("Profile updated.");
                }
                case "2" -> {
                    System.out.print("Old password: ");
                    if (!verifyPassword(in.nextLine().trim())) {
                        System.out.println("Wrong password.");
                        break;
                    }
                    System.out.print("New password: ");
                    String p1 = in.nextLine().trim();
                    System.out.print("Repeat new password: ");
                    String p2 = in.nextLine().trim();
                    if (!p1.equals(p2)) {
                        System.out.println("Passwords do not match.");
                        break;
                    }
                    changePassword(p1);
                    services.saveAll();
                    System.out.println("Password changed.");
                }
                case "0" -> { return; }
                default -> System.out.println("Unknown option.");
            }
        }
    }

    protected void viewNews(Services services) {
        List<News> news = services.getNewsService().getAll();
        if (news.isEmpty()) {
            System.out.println("No news found.");
            return;
        }
        for (News item : news) {
            System.out.println(item);
            System.out.println("  " + item.getContent());
        }
    }

    protected void viewResearchers(Scanner in, Services services) {
        System.out.println("  1  - List all researchers");
        System.out.println("  2  - Top cited by school");
        System.out.println("  3  - Top cited by year");
        System.out.print("> ");
        String choice = in.nextLine().trim();

        switch (choice) {
            case "1" -> {
                List<IResearcher> researchers = services.getResearchService().getAllResearchers();
                if (researchers.isEmpty()) { System.out.println("No researchers found."); return; }
                for (int i = 0; i < researchers.size(); i++) {
                    IResearcher r = researchers.get(i);
                    System.out.printf("%d. %s | h-index: %d | school: %s | papers: %d%n",
                            i + 1, researcherLabel(r), r.getHIndex(), r.getResearchSchool(), r.getPapers().size());
                }
            }
            case "2" -> {
                Enums.School school = MenuHelper.select(in, "School number: ", Enums.School.values());
                services.getResearchService().printTopCitedResearcherBySchool(school.getDisplayName());
            }
            case "3" -> {
                System.out.print("Year (e.g. 2024): ");
                try {
                    int year = Integer.parseInt(in.nextLine().trim());
                    services.getResearchService().printTopCitedResearcherOfYear(year);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid year.");
                }
            }
            default -> System.out.println("Unknown option.");
        }
    }

    protected void viewResearchPapers(Scanner in, Services services) {
        System.out.println("Sort by: 1=citations, 2=date, 3=pages");
        System.out.print("> ");
        String choice = in.nextLine().trim();

        Comparator<ResearchPaper> comparator = switch (choice) {
            case "2" -> Comparator.comparing(ResearchPaper::getDatePublished,
                    Comparator.nullsLast(Comparator.naturalOrder()));
            case "3" -> Comparator.comparingInt(ResearchPaper::getPages).reversed();
            default ->Comparator.comparingInt(ResearchPaper::getCitations).reversed();
        };
        services.getResearchService().printAllPapersSorted(comparator);
    }

    protected void searchPublicInfo(Scanner in, Services services) {
        System.out.print("Keyword: ");
        String keyword = in.nextLine().trim().toLowerCase();
        if (keyword.isBlank()) {
            System.out.println("Keyword cannot be empty.");
            return;
        }

        boolean found = false;
        for (News item : services.getNewsService().getAll()) {
            if (contains(item.getTitle(), keyword) || contains(item.getContent(), keyword)) {
                System.out.println("NEWS: " + item);
                found = true;
            }
        }
        for (IResearcher researcher : services.getResearchService().getAllResearchers()) {
            if (contains(researcherLabel(researcher), keyword)
                    || contains(researcher.getResearchSchool(), keyword)) {
                System.out.println("RESEARCHER: " + researcherLabel(researcher));
                found = true;
            }
        }
        for (ResearchPaper paper : services.getResearchService().getAllPapers()) {
            if (contains(paper.getTitle(), keyword)
                    || contains(paper.getJournal(), keyword)
                    || contains(paper.getDoi(), keyword)
                    || contains(paper.getField(), keyword)
                    || contains(String.join(" ", paper.getAuthors()), keyword)) {
                System.out.println("PAPER: " + paper);
                found = true;
            }
        }
        if (!found) System.out.println("No public information found.");
    }

    protected void manageMessages(Scanner in, Services services) {
        while (true) {
            List<Message> inbox = services.getMessageService().getInbox(username);
            long unread = inbox.stream().filter(m -> !m.isRead()).count();
            System.out.println("\n=== Messages [inbox: " + inbox.size() + ", unread: " + unread + "] ===");
            System.out.println("  1 - View inbox");
            System.out.println("  2 - Send message");
            System.out.println("  0  - Back");
            System.out.print("> ");

            switch (in.nextLine().trim()) {
                case "1" -> viewInbox(in, services, inbox);
                case "2" -> sendMessage(in, services);
                case "0" -> { return; }
                default ->System.out.println("Unknown option.");
            }
        }
    }

    private void viewInbox(Scanner in, Services services, List<Message> inbox) {
        if (inbox.isEmpty()) {
            System.out.println("Inbox is empty.");
            return;
        }
        for (int i = 0; i < inbox.size(); i++) {
            System.out.printf("%d. %s%n", i + 1, inbox.get(i));
        }
        System.out.print("Message number to read (0=back): ");
        int idx = readIndex(in, inbox.size());
        if (idx < 0) return;

        Message msg = inbox.get(idx);
        System.out.println("\n--- Message ---");
        System.out.println("From:    " + msg.getFromUsername());
        System.out.println("Subject: " + msg.getSubject());
        System.out.println("Sent:    " + msg.getSentAt());
        System.out.println();
        System.out.println(msg.getContent());
        System.out.println("---------------");

        if (!msg.isRead()) {
            services.getMessageService().markAsRead(username, msg.getMessageId());
            services.saveAll();
        }
    }

    private void sendMessage(Scanner in, Services services) {
        System.out.print("Recipient username: ");
        String to = in.nextLine().trim();
        if (services.getUserRepository().findByUsername(to).isEmpty()) {
            System.out.println("User '" + to + "' not found.");
            return;
        }
        System.out.print("Subject: ");
        String subject = in.nextLine().trim();
        if (subject.isBlank()) {
            System.out.println("Subject cannot be empty.");
            return;
        }
        System.out.print("Message: ");
        String content = in.nextLine().trim();
        if (content.isBlank()) {
            System.out.println("Message cannot be empty.");
            return;
        }
        services.getMessageService().send(username, to, subject, content);
        services.saveAll();
        System.out.println("Message sent to '" + to + "'.");
    }

    /** Shared index reader used by subclasses: prompts nothing, returns -1 on cancel/error. */
    protected int readIndex(Scanner in, int size) {
        try {
            int index = Integer.parseInt(in.nextLine().trim()) - 1;
            if (index < 0 || index >= size) {
                if (index == -1) return -1; // user typed 0
                System.out.println("Invalid number.");
                return -1;
            }
            return index;
        } catch (NumberFormatException e) {
            System.out.println("Invalid number.");
            return -1;
        }
    }

    private boolean contains(String source, String keyword) {
        return source != null && source.toLowerCase().contains(keyword);
    }

    private String researcherLabel(IResearcher researcher) {
        if (researcher instanceof User user) {
            return user.getFullName() + " (" + user.getUsername() + ")";
        }
        return researcher.toString();
    }

    protected void manageResearch(Scanner in, Services services) {
        while (true) {
            System.out.println("\n=== Research ===");
            System.out.println("  l - List my papers");
            System.out.println("  p - Publish paper");
            System.out.println("  c - Create project");
            System.out.println("  j - Join project");
            System.out.println("  v - View my projects");
            System.out.println("  a - View all projects");
            System.out.println("  h - Update h-index/school");
            System.out.println("  b - Back");
            System.out.print("> ");

            String choice = in.nextLine().trim().toLowerCase();
            switch (choice) {
                case "l" -> printOwnPapers(in);
                case "p" -> publishPaper(in, services);
                case "c" -> createProject(in, services);
                case "j" -> joinProject(in, services);
                case "v" -> viewProjects(services, true);
                case "a" -> viewProjects(services, false);
                case "h" -> updateResearchProfile(in, services);
                case "b" -> { return; }
                default -> System.out.println("Unknown option.");
            }
        }
    }

    private void printOwnPapers(Scanner in) {
        System.out.println("Sort by: 1=citations, 2=date, 3=pages");
        String choice = in.nextLine().trim();
        Comparator<ResearchPaper> comparator = switch (choice) {
            case "2" -> Comparator.comparing(ResearchPaper::getDatePublished,
                    Comparator.nullsLast(Comparator.naturalOrder()));
            case "3" -> Comparator.comparingInt(ResearchPaper::getPages).reversed();
            default  -> Comparator.comparingInt(ResearchPaper::getCitations).reversed();
        };
        ((IResearcher) this).printPapers(comparator);
    }

    private void publishPaper(Scanner in, Services services) {
        IResearcher me = (IResearcher) this;
        try {
            System.out.print("Title: ");
            String title = in.nextLine().trim();
            System.out.print("Authors (comma-separated): ");
            List<String> authors = Arrays.stream(in.nextLine().split(","))
                    .map(String::trim).filter(s -> !s.isBlank()).toList();
            System.out.print("Journal: ");
            String journal = in.nextLine().trim();
            System.out.print("Pages: ");
            int pages = Integer.parseInt(in.nextLine().trim());
            System.out.print("Date published (YYYY-MM-DD): ");
            LocalDate date = LocalDate.parse(in.nextLine().trim());
            System.out.print("DOI: ");
            String doi = in.nextLine().trim();
            System.out.print("Citations: ");
            int citations = Integer.parseInt(in.nextLine().trim());
            System.out.print("Field: ");
            String field = in.nextLine().trim();
            System.out.print("Abstract (Enter to skip): ");
            String abstractText = in.nextLine().trim();
            System.out.print("Keywords (Enter to skip): ");
            String keywords = in.nextLine().trim();

            ResearchPaper paper = new ResearchPaper(title, authors, journal, pages, date, doi, citations, field);
            if (!abstractText.isBlank()) paper.setAbstractText(abstractText);
            if (!keywords.isBlank()) paper.setKeywords(keywords);
            me.addResearchPaper(paper);

            System.out.println("Add co-authors by username (empty line to finish):");
            while (true) {
                System.out.print("Co-author username: ");
                String coUsername = in.nextLine().trim();
                if (coUsername.isBlank()) break;
                if (coUsername.equals(getUsername())) { System.out.println("  That is you."); continue; }
                User coUser = services.getUserRepository().findByUsername(coUsername).orElse(null);
                if (!(coUser instanceof IResearcher coR) || !coR.isResearcher()) {
                    System.out.println("  Not a researcher: " + coUsername);
                    continue;
                }
                coR.addResearchPaper(paper);
                System.out.println("  Paper added to " + coUsername + "'s profile.");
            }

            List<ResearchProject> myProjects = services.getResearchService().getAllProjects().stream()
                    .filter(p -> p.getParticipantUsernames().contains(getUsername()))
                    .toList();
            if (!myProjects.isEmpty()) {
                System.out.println("Your projects:");
                for (int i = 0; i < myProjects.size(); i++)
                    System.out.printf("  %d. %s%n", i + 1, myProjects.get(i));
                System.out.print("Add to project number (Enter to skip): ");
                String projectInput = in.nextLine().trim();
                if (!projectInput.isBlank()) {
                    try {
                        int idx = Integer.parseInt(projectInput) - 1;
                        if (idx >= 0 && idx < myProjects.size()) {
                            myProjects.get(idx).addPaper(paper);
                            System.out.println("Paper linked to project.");
                        } else {
                            System.out.println("Invalid number.");
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid input.");
                    }
                }
            }
            services.saveAll();
            System.out.println("Paper published.");
        } catch (RuntimeException e) {
            System.out.println("Could not publish paper: " + e.getMessage());
        }
    }

    private void createProject(Scanner in, Services services) {
        try {
            System.out.print("Project ID: ");
            String id = in.nextLine().trim();
            System.out.print("Topic: ");
            ResearchProject project = new ResearchProject(id, in.nextLine().trim());
            services.getResearchService().addProject(project);
            services.getResearchService().joinProject(id, getUsername());
            services.saveAll();
            System.out.println("Project created.");
        } catch (RuntimeException e) {
            System.out.println("Could not create project: " + e.getMessage());
        }
    }

    private void joinProject(Scanner in, Services services) {
        viewProjects(services, false);
        try {
            System.out.print("Project ID: ");
            services.getResearchService().joinProject(in.nextLine().trim(), getUsername());
            services.saveAll();
            System.out.println("Joined project.");
        } catch (RuntimeException e) {
            System.out.println("Could not join project: " + e.getMessage());
        }
    }

    private void viewProjects(Services services, boolean onlyMine) {
        List<ResearchProject> projects = services.getResearchService().getAllProjects();
        if (onlyMine)
            projects = projects.stream()
                    .filter(p -> p.getParticipantUsernames().contains(getUsername()))
                    .toList();
        if (projects.isEmpty()) {
            System.out.println(onlyMine ? "You are not in any research projects." : "No research projects found.");
            return;
        }
        for (ResearchProject p : projects) {
            System.out.println(p);
            System.out.println("  Participants: " + p.getParticipantUsernames());
            for (ResearchPaper paper : p.getPublishedPapers())
                System.out.println("    - " + paper.getTitle() + " (" + paper.getCitations() + " citations)");
        }
    }

    protected void updateResearchProfile(Scanner in, Services services) {
        IResearcher me = (IResearcher) this;
        try {
            System.out.print("h-index: ");
            me.getResearchProfile().setHIndex(Integer.parseInt(in.nextLine().trim()));
            System.out.print("Research school: ");
            me.getResearchProfile().setSchool(in.nextLine().trim());
            services.saveAll();
            System.out.println("Research profile updated.");
        } catch (RuntimeException e) {
            System.out.println("Could not update profile: " + e.getMessage());
        }
    }

    protected abstract void printRoleSpecificMenu();

    protected abstract boolean handleRoleSpecificChoice(String choice, Scanner in, Services services);

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        return username.equals(((User) o).username);
    }

    @Override
    public int hashCode() { return username.hashCode(); }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + username + ", " + fullName + "}";
    }
}
