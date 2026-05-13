package Actors;

import Assets.News;
import Assets.ResearchPaper;
import Interfaces.IResearcher;
import Services.Services;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

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
            System.out.println("\n=== Menu [" + getClass().getSimpleName() + ": " + username + "] ===");
            System.out.println("  p  - Manage profile");
            System.out.println("  n  - View news");
            System.out.println("  r  - View researchers");
            System.out.println("  rp - View research papers");
            System.out.println("  s  - Search public information");
            System.out.println("  m  - Manage messages");
            System.out.println("  0  - Logout");
            printRoleSpecificMenu();
            System.out.print("> ");

            String choice = in.nextLine().trim().toLowerCase();

            switch (choice) {
                case "p"  -> manageProfile(in, services);
                case "n"  -> viewNews(services);
                case "r"  -> viewResearchers(services);
                case "rp" -> viewResearchPapers(in, services);
                case "s"  -> searchPublicInfo(in, services);
                case "m"  -> manageMessages(in, services);
                case "0"  -> { return; }
                default   -> {
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
            System.out.println("  0 - Back");
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

    protected void viewResearchers(Services services) {
        List<IResearcher> researchers = services.getResearchService().getAllResearchers();
        if (researchers.isEmpty()) {
            System.out.println("No researchers found.");
            return;
        }
        for (int i = 0; i < researchers.size(); i++) {
            IResearcher r = researchers.get(i);
            System.out.printf("%d. %s | h-index: %d | school: %s | papers: %d%n",
                    i + 1, researcherLabel(r), r.getHIndex(), r.getResearchSchool(), r.getPapers().size());
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
            default -> Comparator.comparingInt(ResearchPaper::getCitations).reversed();
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
        System.out.println("No saved messages.");
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
