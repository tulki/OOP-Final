package Actors;

import Assets.Course;
import Assets.Mark;
import Assets.ResearchPaper;
import Assets.ResearchProfile;
import Assets.ResearchProject;
import Enums.TeacherTitle;
import Services.Services;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

public class Teacher extends Employee {
    private static final long serialVersionUID = 1L;

    private TeacherTitle title;
    private List<Course> courses;
    private double ratingSum;
    private int ratingCount;

    public Teacher(String username, String passwordHash, String fullName,
                   String department, TeacherTitle title) {
        super(username, passwordHash, fullName, department);
        this.title = title;
        this.courses = new ArrayList<>();
        if (isProfessor()) setResearchProfile(new ResearchProfile(0, department));
    }

    public TeacherTitle getTitle()  { return title; }
    public List<Course> getCourses() { return courses; }
    public boolean isProfessor()    { return title == TeacherTitle.PROFESSOR; }

    public double getRating() {
        return ratingCount == 0 ? 0.0 : ratingSum / ratingCount;
    }

    public void addCourse(Course course) {
        if (!courses.contains(course)) courses.add(course);
    }

    public void removeCourse(Course course) { courses.remove(course); }

    public void addRating(int rating) {
        if (rating < 1 || rating > 5) throw new IllegalArgumentException("Rating must be 1-5");
        ratingSum += rating;
        ratingCount++;
    }

    @Override
    protected void printRoleSpecificMenu() {
        System.out.println("--- Teacher (" + title + ") ---");
        System.out.println("  1  - Manage my courses");
        System.out.println("  2  - Send complaint");
        System.out.println("  3  - Manage my requests");
        System.out.println("  4  - Write recommendation letter");
        if (isResearcher())
            System.out.println("  5  - Research block");
    }

    @Override
    protected boolean handleRoleSpecificChoice(String choice, Scanner in, Services services) {
        switch (choice) {
            case "1" -> manageMyCourses(in, services);
            case "2" -> sendComplaint(in, services);
            case "3" -> manageOwnRequests(in, services);
            case "4" -> writeRecommendationLetter(in, services);
            case "5" -> { if (isResearcher()) manageResearch(in, services); else return false; }
            default  -> { return false; }
        }
        return true;
    }

    private void manageMyCourses(Scanner in, Services services) {
        if (courses.isEmpty()) {
            System.out.println("No assigned courses.");
            return;
        }
        List<Course> currentCourses = canonicalCourses(services);
        for (int i = 0; i < currentCourses.size(); i++) {
            System.out.printf("%d. %s%n", i + 1, currentCourses.get(i));
        }

        System.out.print("Course number: ");
        int index = readIndex(in, currentCourses.size());
        if (index < 0) return;
        Course course = currentCourses.get(index);

        System.out.println("  vs - View students");
        System.out.println("  mk - Manage marks");
        System.out.println("  st - View statistics");
        System.out.println("  b  - Back");
        System.out.print("> ");
        switch (in.nextLine().trim().toLowerCase()) {
            case "vs" -> printStudentsForCourse(course, services);
            case "mk" -> manageMarks(course, in, services);
            case "st" -> printCourseStatistics(course, services);
            case "b" -> { return; }
            default -> System.out.println("Unknown option.");
        }
    }

    private void writeRecommendationLetter(Scanner in, Services services) {
        System.out.print("Student username: ");
        String studentUsername = in.nextLine().trim();
        User user = services.getUserRepository().findByUsername(studentUsername).orElse(null);
        if (!(user instanceof Student student) || !teachesStudent(student, services)) {
            System.out.println("Student is not enrolled in your courses.");
            return;
        }

        System.out.println("Letter body:");
        String body = in.nextLine();
        System.out.println("\n=== Recommendation letter ===");
        System.out.println("Teacher: " + getFullName());
        System.out.println("Student: " + student.getFullName());
        System.out.println(body);
    }

    private void manageResearch(Scanner in, Services services) {
        while (true) {
            System.out.println("\n=== Research ===");
            System.out.println("  l - List my papers");
            System.out.println("  p - Publish paper");
            System.out.println("  c - Create project");
            System.out.println("  j - Join project");
            System.out.println("  v - View projects");
            System.out.println("  h - Update h-index/school");
            System.out.println("  b - Back");
            System.out.print("> ");

            String choice = in.nextLine().trim().toLowerCase();
            switch (choice) {
                case "l" -> printOwnPapers(in);
                case "p" -> publishPaper(in, services);
                case "c" -> createProject(in, services);
                case "j" -> joinProject(in, services);
                case "v" -> services.getResearchService().getAllProjects().forEach(System.out::println);
                case "h" -> updateResearchProfile(in, services);
                case "b" -> { return; }
                default -> System.out.println("Unknown option.");
            }
        }
    }

    private void printStudentsForCourse(Course course, Services services) {
        List<Student> students = studentsForCourse(course, services);
        if (students.isEmpty()) {
            System.out.println("No students enrolled.");
            return;
        }
        students.forEach(s -> System.out.printf("  %s | %s | GPA %.2f%n",
                s.getUsername(), s.getFullName(), services.getMarkService().getGpa(s.getUsername())));
    }

    private void manageMarks(Course course, Scanner in, Services services) {
        printStudentsForCourse(course, services);
        try {
            System.out.print("Student username: ");
            String studentUsername = in.nextLine().trim();
            System.out.print("First attestation (0-30): ");
            double first = Double.parseDouble(in.nextLine().trim());
            System.out.print("Second attestation (0-30): ");
            double second = Double.parseDouble(in.nextLine().trim());
            System.out.print("Final exam (0-40): ");
            double finalExam = Double.parseDouble(in.nextLine().trim());

            services.getMarkService().setMark(studentUsername, course.getCourseId(),
                    new Mark(first, second, finalExam));
            services.saveAll();
            System.out.println("Mark saved.");
        } catch (RuntimeException e) {
            System.out.println("Could not save mark: " + e.getMessage());
        }
    }

    private void printCourseStatistics(Course course, Services services) {
        List<Student> students = studentsForCourse(course, services);
        if (students.isEmpty()) {
            System.out.println("No students enrolled.");
            return;
        }
        double total = 0.0;
        int count = 0;
        int passed = 0;
        for (Student student : students) {
            Mark mark = services.getMarkService().getMark(student.getUsername(), course.getCourseId());
            if (mark == null) continue;
            total += mark.getTotal();
            count++;
            if (mark.isPassed()) passed++;
        }
        if (count == 0) {
            System.out.println("No marks recorded.");
            return;
        }
        System.out.printf("Average total: %.2f | Pass rate: %.1f%%%n",
                total / count, passed * 100.0 / count);
    }

    private void printOwnPapers(Scanner in) {
        System.out.println("Sort by: 1=citations, 2=date, 3=pages");
        String choice = in.nextLine().trim();
        Comparator<ResearchPaper> comparator = switch (choice) {
            case "2" -> Comparator.comparing(ResearchPaper::getDatePublished,
                    Comparator.nullsLast(Comparator.naturalOrder()));
            case "3" -> Comparator.comparingInt(ResearchPaper::getPages).reversed();
            default -> Comparator.comparingInt(ResearchPaper::getCitations).reversed();
        };
        printPapers(comparator);
    }

    private void publishPaper(Scanner in, Services services) {
        try {
            System.out.print("Title: ");
            String title = in.nextLine().trim();
            System.out.print("Authors (comma-separated): ");
            List<String> authors = Arrays.stream(in.nextLine().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .toList();
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

            addResearchPaper(new ResearchPaper(title, authors, journal, pages, date, doi, citations, field));
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
        services.getResearchService().getAllProjects().forEach(System.out::println);
        try {
            System.out.print("Project ID: ");
            services.getResearchService().joinProject(in.nextLine().trim(), getUsername());
            services.saveAll();
            System.out.println("Joined project.");
        } catch (RuntimeException e) {
            System.out.println("Could not join project: " + e.getMessage());
        }
    }

    private void updateResearchProfile(Scanner in, Services services) {
        if (!isResearcher()) setResearchProfile(new ResearchProfile(0, getDepartment()));
        try {
            System.out.print("h-index: ");
            getResearchProfile().setHIndex(Integer.parseInt(in.nextLine().trim()));
            System.out.print("School: ");
            getResearchProfile().setSchool(in.nextLine().trim());
            services.saveAll();
            System.out.println("Research profile updated.");
        } catch (RuntimeException e) {
            System.out.println("Could not update profile: " + e.getMessage());
        }
    }

    private List<Course> canonicalCourses(Services services) {
        List<Course> result = new ArrayList<>();
        for (Course course : courses) {
            Course current = services.getCourseService().findById(course.getCourseId());
            result.add(current != null ? current : course);
        }
        return result;
    }

    private List<Student> studentsForCourse(Course course, Services services) {
        return services.getUserRepository().getAllUsers().stream()
                .filter(u -> u instanceof Student)
                .map(u -> (Student) u)
                .filter(s -> s.getCourses().contains(course))
                .toList();
    }

    private boolean teachesStudent(Student student, Services services) {
        for (Course teacherCourse : canonicalCourses(services)) {
            if (student.getCourses().contains(teacherCourse)) return true;
        }
        return false;
    }

    private int readIndex(Scanner in, int size) {
        try {
            int index = Integer.parseInt(in.nextLine().trim()) - 1;
            if (index < 0 || index >= size) {
                System.out.println("Invalid number.");
                return -1;
            }
            return index;
        } catch (NumberFormatException e) {
            System.out.println("Invalid number.");
            return -1;
        }
    }
}
