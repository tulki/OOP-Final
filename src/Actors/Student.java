package Actors;

import Models.Course;
import Models.Lesson;
import Models.Mark;
import Models.ResearchProfile;
import Enums.LogEventType;
import Enums.Major;
import Enums.YearLevel;
import Exceptions.CreditLimitExceededException;
import Exceptions.LowHIndexException;
import Exceptions.NotAResearcherException;
import Interfaces.IResearcher;
import Services.Services;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class Student extends User implements IResearcher {
    private static final long serialVersionUID = 1L;

    public static final int MAX_CREDITS = 21;
    public static final int MAX_FAILS = 3;
    public static final int MIN_H_INDEX = 3;

    private Major major;
    private YearLevel year;
    private int enrolledCredits;
    private List<Course> courses;
    private Set<String> ratedTeachers;
    private ResearchProfile researchProfile;
    private IResearcher researchSupervisor;

    public Student(String username, String passwordHash, String fullName,
                   Major major, YearLevel year) {
        super(username, passwordHash, fullName);
        this.major = major;
        this.year = year;
        this.enrolledCredits = 0;
        this.courses = new ArrayList<>();
        this.ratedTeachers = new LinkedHashSet<>();
    }

    public Major getMajor() { return major; }
    public YearLevel getYear() { return year; }
    public int getEnrolledCredits() { return enrolledCredits; }
    public List<Course> getCourses() { return courses; }
    public IResearcher getResearchSupervisor() { return researchSupervisor; }

    @Override
    public ResearchProfile getResearchProfile() { return researchProfile; }

    @Override
    public void setResearchProfile(ResearchProfile researchProfile) {
        this.researchProfile = researchProfile;
    }

    public void addCourse(Course course) {
        if (enrolledCredits + course.getCredits() > MAX_CREDITS)
            throw new CreditLimitExceededException(
                    course.getName(), course.getCredits(), enrolledCredits, MAX_CREDITS);
        if (!courses.contains(course)) {
            courses.add(course);
            enrolledCredits += course.getCredits();
        }
    }

    public void dropCourse(Course course) {
        if (courses.remove(course)) enrolledCredits -= course.getCredits();
    }

    public void assignResearchSupervisor(IResearcher supervisor) {
        if (year != YearLevel.YEAR_4)
            throw new IllegalStateException("Only 4th-year students can choose a research supervisor.");
        if (supervisor == null || !supervisor.isResearcher()) {
            String name = (supervisor instanceof User u) ? u.getUsername() : "unknown";
            throw new NotAResearcherException(name);
        }
        if (supervisor.getHIndex() < MIN_H_INDEX)
            throw new LowHIndexException(supervisor.getHIndex(), MIN_H_INDEX);
        this.researchSupervisor = supervisor;
    }

    @Override
    protected void printRoleSpecificMenu() {
        System.out.println("--- Student ---");
        System.out.println("  1  - Register for a course");
        System.out.println("  2  - View my courses (drop / teacher details)");
        System.out.println("  3  - View marks");
        System.out.println("  4  - View transcript");
        System.out.println("  5  - Rate a teacher");
        System.out.println("  6  - View schedule");
        if (year == YearLevel.YEAR_4)
            System.out.println("  7  - Choose research supervisor");
        if (isResearcher())
            System.out.println("  8  - Research block");
    }

    @Override
    protected boolean handleRoleSpecificChoice(String choice, Scanner in, Services services) {
        switch (choice) {
            case "1" -> registerForCourse(in, services);
            case "2" -> viewMyCourses(in, services);
            case "3" -> viewMarks(services);
            case "4" -> viewTranscript(services);
            case "5" -> rateTeacher(in, services);
            case "6" -> viewSchedule(services);
            case "7" -> { if (year == YearLevel.YEAR_4) chooseSupervisor(in, services);
                          else return false; }
            case "8" -> { if (isResearcher()) manageResearch(in, services); else return false; }
            default ->{ return false; }
        }
        return true;
    }

    private void registerForCourse(Scanner in, Services services) {
        List<Course> available = services.getCourseService().getCoursesForMajorAndYear(major, year);
        if (available.isEmpty()) {
            System.out.println("No open courses found for " + major + ", year " + year + ".");
            return;
        }
        for (int i = 0; i < available.size(); i++) {
            System.out.printf("  %d. %s%n", i + 1, available.get(i));
        }
        System.out.print("Course number: ");
        int index = readIndex(in, available.size());
        if (index < 0) return;
        try {
            String courseId = available.get(index).getCourseId();
            services.getCourseService().requestRegistration(getUsername(), courseId);
            services.getLogger().log(LogEventType.ACTION,
                    "student " + getUsername() + " submitted registration request for course " + courseId);
            services.saveAll();
        } catch (RuntimeException e) {
            System.out.println("Could not request registration: " + e.getMessage());
        }
    }

    private void viewMyCourses(Scanner in, Services services) {
        if (courses.isEmpty()) {
            System.out.println("You are not enrolled in any courses.");
            return;
        }
        printEnrolledCourses(services);
        System.out.println("  d - Drop course");
        System.out.println("  t - View teacher details");
        System.out.println("  b - Back");
        System.out.print("> ");

        String choice = in.nextLine().trim().toLowerCase();
        if ("b".equals(choice)) return;

        System.out.print("Course number: ");
        int index = readIndex(in, courses.size());
        if (index < 0) return;

        Course selected = canonicalCourse(courses.get(index), services);
        if ("d".equals(choice)) {
            dropCourse(selected);
            services.saveAll();
            System.out.println("Course dropped.");
        } else if ("t".equals(choice)) {
            printTeacherDetails(selected, services);
        } else {
            System.out.println("Unknown option.");
        }
    }

    private void viewMarks(Services services) {
        List<Map.Entry<String, Mark>> marks = services.getMarkService().getMarksForStudent(getUsername());
        if (marks.isEmpty()) {
            System.out.println("No marks found.");
            return;
        }
        for (Map.Entry<String, Mark> entry : marks) {
            String courseId = entry.getKey().split(":")[1];
            System.out.printf("  %-12s %s%n", courseId, entry.getValue());
        }
    }

    private void viewTranscript(Services services) {
        services.getReportService().generateTranscript(getUsername());
    }

    private void rateTeacher(Scanner in, Services services) {
        Set<String> teacherUsernames = new LinkedHashSet<>();
        for (Course course : courses) {
            teacherUsernames.addAll(canonicalCourse(course, services).getInstructorUsernames());
        }
        if (teacherUsernames.isEmpty()) {
            System.out.println("No teachers found for your courses.");
            return;
        }
        teacherUsernames.forEach(username -> System.out.println("  " + username));

        try {
            System.out.print("Teacher username: ");
            String username = in.nextLine().trim();
            User user = services.getUserRepository().findByUsername(username).orElse(null);
            if (!(user instanceof Teacher teacher) || !teacherUsernames.contains(username)) {
                System.out.println("Teacher not found among your courses.");
                return;
            }
            if (ratedTeachers.contains(username)) {
                System.out.println("You have already rated this teacher.");
                return;
            }
            System.out.print("Rating (1-5): ");
            teacher.addRating(Integer.parseInt(in.nextLine().trim()));
            ratedTeachers.add(username);
            services.saveAll();
            System.out.println("Rating saved.");
        } catch (RuntimeException e) {
            System.out.println("Could not rate teacher: " + e.getMessage());
        }
    }

    private void viewSchedule(Services services) {
        List<Lesson> lessons = new ArrayList<>();
        for (Course course : courses) lessons.addAll(canonicalCourse(course, services).getLessons());
        lessons.sort(Comparator.comparing(Lesson::getDayOfWeek).thenComparing(Lesson::getStartTime));
        if (lessons.isEmpty()) {
            System.out.println("No lessons scheduled.");
            return;
        }
        lessons.forEach(System.out::println);
    }

    private void chooseSupervisor(Scanner in, Services services) {
        List<IResearcher> eligible = services.getResearchService().getAllResearchers().stream()
                .filter(r -> r != this && r.getHIndex() >= MIN_H_INDEX)
                .toList();
        if (eligible.isEmpty()) {
            System.out.println("No eligible supervisors found (h-index >= " + MIN_H_INDEX + " required).");
            return;
        }
        for (int i = 0; i < eligible.size(); i++) {
            IResearcher r = eligible.get(i);
            String label = r instanceof User user
                    ? user.getFullName() + " (" + user.getUsername() + ")"
                    : r.toString();
            System.out.printf("%d. %s | h-index: %d%n", i + 1, label, r.getHIndex());
        }
        try {
            System.out.print("Researcher number: ");
            int index = Integer.parseInt(in.nextLine().trim()) - 1;
            if (index < 0 || index >= eligible.size()) {
                System.out.println("Invalid researcher number.");
                return;
            }
            assignResearchSupervisor(eligible.get(index));
            services.saveAll();
            System.out.println("Research supervisor assigned.");
        } catch (RuntimeException e) {
            System.out.println("Could not assign supervisor: " + e.getMessage());
        }
    }

    private void printEnrolledCourses(Services services) {
        for (int i = 0; i < courses.size(); i++) {
            System.out.printf("%d. %s%n", i + 1, canonicalCourse(courses.get(i), services));
        }
    }

    private void printTeacherDetails(Course course, Services services) {
        List<String> instructors = course.getInstructorUsernames();
        if (instructors.isEmpty()) {
            System.out.println("No instructors assigned.");
            return;
        }
        for (String username : instructors) {
            User user = services.getUserRepository().findByUsername(username).orElse(null);
            if (user instanceof Teacher teacher) {
                System.out.printf("  %s | %s | %s | rating %.2f%n",
                        teacher.getUsername(), teacher.getFullName(), teacher.getTitle(), teacher.getRating());
            } else {
                System.out.println("  " + username);
            }
        }
    }

    private void readObject(java.io.ObjectInputStream in)
            throws java.io.IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (courses == null) courses = new ArrayList<>();
        if (ratedTeachers == null) ratedTeachers = new LinkedHashSet<>();
    }

    private Course canonicalCourse(Course course, Services services) {
        Course current = services.getCourseService().findById(course.getCourseId());
        return current != null ? current : course;
    }

}
