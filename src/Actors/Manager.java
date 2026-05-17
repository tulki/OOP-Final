package Actors;

import Exceptions.TooManyFailsException;
import Models.Course;
import Models.Lesson;
import Models.News;
import Models.Request;
import Enums.CourseStatus;
import Enums.LessonType;
import Enums.LogEventType;
import Enums.Major;
import Enums.ManagerType;
import Enums.RequestType;
import Enums.School;
import Enums.YearLevel;
import Services.MenuHelper;
import Services.Services;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Set;
import java.util.UUID;

import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

public class Manager extends Employee {
    private static final long serialVersionUID = 1L;

    private ManagerType managerType;

    public Manager(String username, String passwordHash, String fullName,
                   ManagerType managerType, School school) {
        super(username, passwordHash, fullName, school);
        this.managerType = managerType;
    }

    public ManagerType getManagerType() { return managerType; }

    protected String getRoleLabel() {
        if (getSchool() != null) return "Manager (" + managerType + ", " + getSchool().getDisplayName() + ")";
        return "Manager (" + managerType + ")";
    }

    @Override
    protected void printRoleSpecificMenu() {
        System.out.println("--- " + getRoleLabel() + " ---");
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
            default ->{ return false; }
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
            System.out.print("Credits (1-30): ");
            int credits = Integer.parseInt(in.nextLine().trim());
            if (credits < 1 || credits > 30)
                throw new IllegalArgumentException("Credits must be between 1 and 30.");
            Set<Major> majors = MenuHelper.selectMultiple(in, "Major number: ", Major.values());
            Set<YearLevel> yearLevels = MenuHelper.selectMultiple(in, "Year number: ", YearLevel.values());

            Course course = new Course(courseId, name, credits);
            majors.forEach(course::addMajor);
            yearLevels.forEach(course::addYearLevel);
            services.getCourseService().addCourse(course);
            services.getLogger().log(LogEventType.ACTION,
                    "manager " + getUsername() + " added course " + courseId);
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
            System.out.println("  m - Set majors");
            System.out.println("  y - Set year level");
            System.out.println("  c - Set credits");
            System.out.println("  s - Set status");
            System.out.println("  l - Manage lessons");
            System.out.print("> ");
            String choice = in.nextLine().trim().toLowerCase();
            switch (choice) {
                case "n" -> {
                    System.out.print("New name: ");
                    course.setName(in.nextLine().trim());
                }
                case "m" -> {
                    course.setMajors(MenuHelper.selectMultiple(in, "Major number: ", Major.values()));
                }
                case "y" -> {
                    course.setYearLevels(MenuHelper.selectMultiple(in, "Year number: ", YearLevel.values()));
                }
                case "c" -> {
                    System.out.print("New credits (1-30): ");
                    int credits = Integer.parseInt(in.nextLine().trim());
                    if (credits < 1 || credits > 30)
                        throw new IllegalArgumentException("Credits must be between 1 and 30.");
                    course.setCredits(credits);
                }
                case "s" -> {
                    course.setStatus(MenuHelper.select(in, "Status number: ", CourseStatus.values()));
                }
                case "l" -> {
                    manageCourseLessons(course, in, services);
                    return;
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
            int fails = services.getMarkService().getFailCount(request[0]);
            String tag = fails >= Student.MAX_FAILS ? " [MAX FAILS]" : "";
            System.out.printf("%d. %s -> %s%s%n", i + 1, request[0], request[1], tag);
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
                int fails = services.getMarkService().getFailCount(request[0]);
                if (fails >= Student.MAX_FAILS)
                    throw new TooManyFailsException(request[0], fails, Student.MAX_FAILS);
                services.getCourseService().approveRegistration(request[0], request[1]);
                services.getLogger().log(LogEventType.ACTION,
                        "manager " + getUsername() + " approved registration " + request[0] + " -> " + request[1]);
            } else if ("r".equals(action)) {
                services.getCourseService().rejectRegistration(request[0], request[1]);
                services.getLogger().log(LogEventType.ACTION,
                        "manager " + getUsername() + " rejected registration " + request[0] + " -> " + request[1]);
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
        for (int i = 0; i < teachers.size(); i++) {
            Teacher t = teachers.get(i);
            System.out.printf("  %d. %s - %s (%s)%n", i + 1,
                    t.getUsername(), t.getFullName(), t.getTitle());
        }

        try {
            System.out.print("Teacher number: ");
            int index = Integer.parseInt(in.nextLine().trim()) - 1;
            if (index < 0 || index >= teachers.size()) {
                System.out.println("Invalid teacher number.");
                return;
            }
            String teacherUsername = teachers.get(index).getUsername();
            services.getCourseService().assignTeacher(courseId, teacherUsername);
            services.getLogger().log(LogEventType.ACTION,
                    "manager " + getUsername() + " assigned teacher " + teacherUsername + " to course " + courseId);
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
                case "3" -> Comparator.comparing(Student::getYear);
                default -> Comparator.comparing(User::getFullName);
            };
            services.getUserRepository().getAllUsers().stream()
                    .filter(u -> u instanceof Student)
                    .map(u -> (Student) u)
                    .sorted(comparator)
                    .forEach(s -> System.out.printf("  %s | %s | %s %s | GPA %.2f%n",
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

    private void manageCourseLessons(Course course, Scanner in, Services services) {
        while (true) {
            List<Lesson> lessons = course.getLessons();
            System.out.println("\n=== Lessons for " + course.getCourseId() + " ===");
            if (lessons.isEmpty()) {
                System.out.println("  (no lessons)");
            } else {
                for (int i = 0; i < lessons.size(); i++) {
                    System.out.printf("  %d. %s%n", i + 1, lessons.get(i));
                }
            }
            System.out.println("  a - Add lesson");
            System.out.println("  r - Remove lesson");
            System.out.println("  b - Back");
            System.out.print("> ");

            String choice = in.nextLine().trim().toLowerCase();
            switch (choice) {
                case "a" -> addLesson(course, in, services);
                case "r" -> removeLesson(course, in, services);
                case "b" -> { return; }
                default -> System.out.println("Unknown option.");
            }
        }
    }

    private void addLesson(Course course, Scanner in, Services services) {
        try {
            LessonType type = MenuHelper.select(in, "Type number: ", LessonType.values());
            System.out.print("Room: ");
            String room = in.nextLine().trim();
            DayOfWeek day = MenuHelper.select(in, "Day number: ", DayOfWeek.values());
            System.out.print("Start time (HH:mm): ");
            LocalTime start = LocalTime.parse(in.nextLine().trim());
            System.out.print("End time (HH:mm): ");
            LocalTime end = LocalTime.parse(in.nextLine().trim());
            if (!end.isAfter(start))
                throw new IllegalArgumentException("End time must be after start time.");
            String id = UUID.randomUUID().toString().substring(0, 8);
            course.addLesson(new Lesson(id, type, room, day, start, end));
            services.saveAll();
            System.out.println("Lesson added.");
        } catch (DateTimeParseException e) {
            System.out.println("Invalid time format. Use HH:mm.");
        } catch (RuntimeException e) {
            System.out.println("Could not add lesson: " + e.getMessage());
        }
    }

    private void removeLesson(Course course, Scanner in, Services services) {
        List<Lesson> lessons = course.getLessons();
        if (lessons.isEmpty()) {
            System.out.println("No lessons to remove.");
            return;
        }
        System.out.print("Lesson number: ");
        try {
            int index = Integer.parseInt(in.nextLine().trim()) - 1;
            if (index < 0 || index >= lessons.size()) {
                System.out.println("Invalid lesson number.");
                return;
            }
            lessons.remove(index);
            services.saveAll();
            System.out.println("Lesson removed.");
        } catch (NumberFormatException e) {
            System.out.println("Invalid number.");
        }
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
        processRequestsOfType(in, services, RequestType.GENERAL, "General Requests");
    }

    private void processEmployeeComplaints(Scanner in, Services services) {
        processRequestsOfType(in, services, RequestType.COMPLAINT, "Complaints");
    }

    private boolean canSignRequests() {
        return false;
    }

    private void processRequestsOfType(Scanner in, Services services,
                                       RequestType type, String label) {
        List<Request> pending = services.getRequestService().getPending().stream()
                .filter(r -> r.getType() == type)
                .toList();

        System.out.println("\n=== " + label + " (pending: " + pending.size() + ") ===");
        if (pending.isEmpty()) {
            System.out.println("No pending " + label.toLowerCase() + ".");
            return;
        }
        for (int i = 0; i < pending.size(); i++) {
            Request r = pending.get(i);
            System.out.printf("%d. %s%n", i + 1, r);
            System.out.println("   Content: " + r.getContent());
        }

        if (!canSignRequests()) {
            System.out.println("(View only — approval authority belongs to Dean or Rector.)");
            return;
        }

        System.out.print("Request number to process (0=skip): ");
        int idx;
        try {
            idx = Integer.parseInt(in.nextLine().trim()) - 1;
        } catch (NumberFormatException e) {
            System.out.println("Invalid number.");
            return;
        }
        if (idx < 0 || idx >= pending.size()) return;
        String id = pending.get(idx).getRequestId();

        System.out.println("  a - Approve");
        System.out.println("  r - Reject");
        System.out.print("> ");
        String action = in.nextLine().trim().toLowerCase();

        System.out.print("Review note (optional): ");
        String note = in.nextLine().trim();

        boolean done;
        if ("a".equals(action)) {
            done = services.getRequestService().approve(id, getUsername(), note);
        } else if ("r".equals(action)) {
            done = services.getRequestService().reject(id, getUsername(), note);
        } else {
            System.out.println("Unknown action.");
            return;
        }

        if (done) {
            services.saveAll();
            System.out.println("Request processed by " + getRoleLabel() + ".");
        } else {
            System.out.println("Request not found or already processed.");
        }
    }
}

