package Actors;

import Models.Course;
import Models.Mark;
import Models.ResearchProfile;
import Enums.LogEventType;
import Enums.School;
import Enums.TeacherTitle;
import Services.Services;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Represents a teaching staff member of the university.
 *
 * <p>Teachers hold one of four academic titles defined by
 * {@link Enums.TeacherTitle}: {@code TUTOR}, {@code LECTOR},
 * {@code SENIOR_LECTOR}, or {@code PROFESSOR}. Professors are
 * automatically activated as researchers upon creation; other titles
 * can opt in by updating their {@link Models.ResearchProfile}.
 *
 * <p>Core responsibilities:
 * <ul>
 *   <li>Managing assigned courses and recording student marks
 *       (ATT1 0–30, ATT2 0–30, Final 0–40).</li>
 *   <li>Viewing per-course statistics (average total, pass rate).</li>
 *   <li>Research activities: publishing papers (with co-author linking),
 *       creating and joining {@link Models.ResearchProject}s,
 *       updating h-index and school.</li>
 * </ul>
 *
 * <p>Teacher rating is accumulated from student votes (1–5 scale) and
 * exposed as a running average via {@link #getRating()}.
 */
public class Teacher extends Employee {
    private static final long serialVersionUID = 1L;

    private TeacherTitle title;
    private List<Course> courses;
    private double ratingSum;
    private int ratingCount;

    public Teacher(String username, String passwordHash, String fullName,
                   School school, TeacherTitle title) {
        super(username, passwordHash, fullName, school);
        this.title = title;
        this.courses = new ArrayList<>();
        if (isProfessor()) setResearchProfile(new ResearchProfile(0, school.getDisplayName()));
    }

    public TeacherTitle getTitle() { return title; }
    public List<Course> getCourses() { return courses; }
    public boolean isProfessor() { return title == TeacherTitle.PROFESSOR; }

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
        if (isResearcher())
            System.out.println("  4  - Research block");
    }

    @Override
    protected boolean handleRoleSpecificChoice(String choice, Scanner in, Services services) {
        switch (choice) {
            case "1" -> manageMyCourses(in, services);
            case "2" -> sendComplaint(in, services);
            case "3" -> manageOwnRequests(in, services);
            case "4" -> { if (isResearcher()) manageResearch(in, services); else return false; }
            default ->{ return false; }
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
        List<Student> enrolled = studentsForCourse(course, services);
        if (enrolled.isEmpty()) {
            System.out.println("No students enrolled in this course.");
            return;
        }
        printStudentsForCourse(course, services);
        try {
            System.out.print("Student username: ");
            String studentUsername = in.nextLine().trim();
            boolean isEnrolled = enrolled.stream()
                    .anyMatch(s -> s.getUsername().equalsIgnoreCase(studentUsername));
            if (!isEnrolled) {
                System.out.println("Student is not enrolled in this course.");
                return;
            }
            System.out.print("First attestation (0-30): ");
            double first = Double.parseDouble(in.nextLine().trim());
            System.out.print("Second attestation (0-30): ");
            double second = Double.parseDouble(in.nextLine().trim());
            System.out.print("Final exam (0-40): ");
            double finalExam = Double.parseDouble(in.nextLine().trim());

            services.getMarkService().setMark(studentUsername, course.getCourseId(),
                    new Mark(first, second, finalExam));
            services.getLogger().log(LogEventType.ACTION,
                    "teacher " + getUsername() + " set mark for " + studentUsername
                            + " in " + course.getCourseId()
                            + " [ATT1=" + first + " ATT2=" + second + " Final=" + finalExam + "]");
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

}
