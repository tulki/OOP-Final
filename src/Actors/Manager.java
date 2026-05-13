package Actors;

import Assets.Course;
import Assets.News;
import Enums.CourseStatus;
import Enums.ManagerType;
import Services.Services;

import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

public class Manager extends Employee {
    private static final long serialVersionUID = 1L;

    private ManagerType managerType;

    public Manager(String username, String passwordHash, String fullName,
                   String department, ManagerType managerType) {
        super(username, passwordHash, fullName, department);
        this.managerType = managerType;
    }

    public ManagerType getManagerType() { return managerType; }

    @Override
    protected void printRoleSpecificMenu() {
        System.out.println("--- Manager (" + managerType + ") ---");
        System.out.println("  1  - Manage course catalog");
        System.out.println("  2  - Approve course registrations");
        System.out.println("  3  - Assign course to teacher");
        System.out.println("  4  - View student & teacher info");
        System.out.println("  5  - Generate academic performance report");
        System.out.println("  6  - Manage news");
        System.out.println("  7  - Process employee requests");
        System.out.println("  8  - Process employee complaints");
    }

    @Override
    protected boolean handleRoleSpecificChoice(String choice, Scanner in, Services services) {
        switch (choice) {
            case "1" -> manageCourseCatalog(in, services);
            case "2" -> approveCourseRegistrations(in, services);
            case "3" -> assignCourseToTeacher(in, services);
            case "4" -> viewStudentAndTeacherInfo(in, services);
            case "5" -> services.getReportService().generateAcademicPerformanceReport();
            case "6" -> manageNews(in, services);
            case "7" -> processEmployeeRequests(in, services);
            case "8" -> processEmployeeComplaints(in, services);
            default  -> { return false; }
        }
        return true;
    }

    private void manageCourseCatalog(Scanner in, Services services) {
        while (true) {
            System.out.println("\n=== Course catalog ===");
            System.out.println("  l - List courses");
            System.out.println("  a - Add course");
            System.out.println("  e - Edit course");
            System.out.println("  b - Back");
            System.out.print("> ");

            String choice = in.nextLine().trim().toLowerCase();
            switch (choice) {
                case "l" -> listCourses(services);
                case "a" -> addCourse(in, services);
                case "e" -> editCourse(in, services);
                case "b" -> { return; }
                default -> System.out.println("Unknown option.");
            }
        }
    }

    private void addCourse(Scanner in, Services services) {
        try {
            System.out.print("Course ID: ");
            String courseId = in.nextLine().trim();
            System.out.print("Name: ");
            String name = in.nextLine().trim();
            System.out.print("Major: ");
            String major = in.nextLine().trim();
            System.out.print("Year level: ");
            int year = Integer.parseInt(in.nextLine().trim());
            System.out.print("Credits: ");
            int credits = Integer.parseInt(in.nextLine().trim());

            services.getCourseService().addCourse(new Course(courseId, name, major, year, credits));
            services.saveAll();
            System.out.println("Course added.");
        } catch (RuntimeException e) {
            System.out.println("Could not add course: " + e.getMessage());
        }
    }

    private void editCourse(Scanner in, Services services) {
        System.out.print("Course ID: ");
        Course course = services.getCourseService().findById(in.nextLine().trim());
        if (course == null) {
            System.out.println("Course not found.");
            return;
        }

        try {
            System.out.println("  n - Set name");
            System.out.println("  m - Set major");
            System.out.println("  y - Set year level");
            System.out.println("  c - Set credits");
            System.out.println("  s - Set status");
            System.out.print("> ");
            String choice = in.nextLine().trim().toLowerCase();
            switch (choice) {
                case "n" -> {
                    System.out.print("New name: ");
                    course.setName(in.nextLine().trim());
                }
                case "m" -> {
                    System.out.print("New major: ");
                    course.setMajor(in.nextLine().trim());
                }
                case "y" -> {
                    System.out.print("New year: ");
                    course.setYearLevel(Integer.parseInt(in.nextLine().trim()));
                }
                case "c" -> {
                    System.out.print("New credits: ");
                    course.setCredits(Integer.parseInt(in.nextLine().trim()));
                }
                case "s" -> {
                    System.out.print("Status (OPEN/CLOSED/ARCHIVED): ");
                    course.setStatus(CourseStatus.valueOf(in.nextLine().trim().toUpperCase()));
                }
                default -> {
                    System.out.println("Unknown option.");
                    return;
                }
            }
            services.saveAll();
            System.out.println("Course updated.");
        } catch (RuntimeException e) {
            System.out.println("Could not update course: " + e.getMessage());
        }
    }

    private void approveCourseRegistrations(Scanner in, Services services) {
        List<String[]> pending = services.getCourseService().getPendingRegistrations();
        if (pending.isEmpty()) {
            System.out.println("No pending registrations.");
            return;
        }
        for (int i = 0; i < pending.size(); i++) {
            String[] request = pending.get(i);
            System.out.printf("%d. %s -> %s%n", i + 1, request[0], request[1]);
        }

        try {
            System.out.print("Request number: ");
            int index = Integer.parseInt(in.nextLine().trim()) - 1;
            if (index < 0 || index >= pending.size()) {
                System.out.println("Invalid request number.");
                return;
            }
            String[] request = pending.get(index);
            System.out.print("a=approve, r=reject: ");
            String action = in.nextLine().trim().toLowerCase();
            if ("a".equals(action)) {
                services.getCourseService().approveRegistration(request[0], request[1]);
            } else if ("r".equals(action)) {
                services.getCourseService().rejectRegistration(request[0], request[1]);
            } else {
                System.out.println("Unknown action.");
                return;
            }
            services.saveAll();
        } catch (RuntimeException e) {
            System.out.println("Could not process request: " + e.getMessage());
        }
    }

    private void assignCourseToTeacher(Scanner in, Services services) {
        listCourses(services);
        System.out.print("Course ID: ");
        String courseId = in.nextLine().trim();

        List<Teacher> teachers = services.getUserRepository().getAllUsers().stream()
                .filter(u -> u instanceof Teacher)
                .map(u -> (Teacher) u)
                .sorted(Comparator.comparing(User::getFullName))
                .toList();
        if (teachers.isEmpty()) {
            System.out.println("No teachers found.");
            return;
        }
        for (Teacher teacher : teachers) {
            System.out.printf("  %s - %s (%s)%n",
                    teacher.getUsername(), teacher.getFullName(), teacher.getTitle());
        }

        try {
            System.out.print("Teacher username: ");
            services.getCourseService().assignTeacher(courseId, in.nextLine().trim());
            services.saveAll();
        } catch (RuntimeException e) {
            System.out.println("Could not assign teacher: " + e.getMessage());
        }
    }

    private void viewStudentAndTeacherInfo(Scanner in, Services services) {
        System.out.println("  s - Students");
        System.out.println("  t - Teachers");
        System.out.print("> ");
        String choice = in.nextLine().trim().toLowerCase();

        if ("s".equals(choice)) {
            System.out.println("Sort students by: 1=name, 2=GPA, 3=year");
            String sort = in.nextLine().trim();
            Comparator<Student> comparator = switch (sort) {
                case "2" -> Comparator.comparingDouble(
                        (Student s) -> services.getMarkService().getGpa(s.getUsername())).reversed();
                case "3" -> Comparator.comparingInt(Student::getYear);
                default -> Comparator.comparing(User::getFullName);
            };
            services.getUserRepository().getAllUsers().stream()
                    .filter(u -> u instanceof Student)
                    .map(u -> (Student) u)
                    .sorted(comparator)
                    .forEach(s -> System.out.printf("  %s | %s | %s year %d | GPA %.2f%n",
                            s.getUsername(), s.getFullName(), s.getMajor(), s.getYear(),
                            services.getMarkService().getGpa(s.getUsername())));
        } else if ("t".equals(choice)) {
            System.out.println("Sort teachers by: 1=name, 2=rating");
            String sort = in.nextLine().trim();
            Comparator<Teacher> comparator = "2".equals(sort)
                    ? Comparator.comparingDouble(Teacher::getRating).reversed()
                    : Comparator.comparing(User::getFullName);
            services.getUserRepository().getAllUsers().stream()
                    .filter(u -> u instanceof Teacher)
                    .map(u -> (Teacher) u)
                    .sorted(comparator)
                    .forEach(t -> System.out.printf("  %s | %s | %s | rating %.2f%n",
                            t.getUsername(), t.getFullName(), t.getTitle(), t.getRating()));
        } else {
            System.out.println("Unknown option.");
        }
    }

    private void manageNews(Scanner in, Services services) {
        while (true) {
            System.out.println("\n=== News ===");
            System.out.println("  l - List all");
            System.out.println("  c - Create");
            System.out.println("  m - Modify");
            System.out.println("  d - Delete");
            System.out.println("  b - Back");
            System.out.print("> ");

            String choice = in.nextLine().trim().toLowerCase();
            switch (choice) {
                case "l" -> listNews(services);
                case "c" -> createNews(in, services);
                case "m" -> modifyNews(in, services);
                case "d" -> deleteNews(in, services);
                case "b" -> { return; }
                default -> System.out.println("Unknown option.");
            }
        }
    }

    private void createNews(Scanner in, Services services) {
        System.out.print("Title: ");
        String title = in.nextLine().trim();
        System.out.print("Content: ");
        String content = in.nextLine().trim();
        News news = services.getNewsService().create(title, content, getUsername());
        services.saveAll();
        System.out.println("Created: " + news);
    }

    private void modifyNews(Scanner in, Services services) {
        listNews(services);
        System.out.print("News ID: ");
        String id = in.nextLine().trim();
        System.out.print("New title: ");
        String title = in.nextLine().trim();
        System.out.print("New content: ");
        String content = in.nextLine().trim();
        boolean modified = services.getNewsService().modify(id, title, content);
        if (modified) {
            services.saveAll();
            System.out.println("News modified.");
        } else {
            System.out.println("News not found.");
        }
    }

    private void deleteNews(Scanner in, Services services) {
        listNews(services);
        System.out.print("News ID: ");
        boolean deleted = services.getNewsService().delete(in.nextLine().trim());
        if (deleted) {
            services.saveAll();
            System.out.println("News deleted.");
        } else {
            System.out.println("News not found.");
        }
    }

    private void listNews(Services services) {
        List<News> news = services.getNewsService().getAll();
        if (news.isEmpty()) {
            System.out.println("No news found.");
            return;
        }
        news.forEach(n -> System.out.println("  " + n + " | " + n.getContent()));
    }

    private void listCourses(Services services) {
        List<Course> courses = services.getCourseService().getAllCourses();
        if (courses.isEmpty()) {
            System.out.println("No courses found.");
            return;
        }
        courses.forEach(c -> System.out.println("  " + c));
    }

    private void processEmployeeRequests(Scanner in, Services services) {
        System.out.println("No pending employee requests.");
    }

    private void processEmployeeComplaints(Scanner in, Services services) {
        System.out.println("No pending employee complaints.");
    }
}
