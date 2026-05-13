package Actors;

import Assets.Course;
import Assets.Lesson;
import Assets.Mark;
import Assets.ResearchProfile;
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

    public static final int MAX_CREDITS      = 21;
    public static final int MAX_FAILS        = 3;
    public static final int SUPERVISOR_YEAR  = 4;
    public static final int MIN_H_INDEX      = 3;

    private String major;
    private int year;
    private int enrolledCredits;
    private int failCount;
    private List<Course> courses;
    private ResearchProfile researchProfile;
    private IResearcher researchSupervisor;

    public Student(String username, String passwordHash, String fullName,
                   String major, int year) {
        super(username, passwordHash, fullName);
        this.major = major;
        this.year  = year;
        this.enrolledCredits = 0;
        this.failCount = 0;
        this.courses = new ArrayList<>();
    }

    public String getMajor()            { return major; }
    public int getYear()                { return year; }
    public int getEnrolledCredits()     { return enrolledCredits; }
    public int getFailCount()           { return failCount; }
    public List<Course> getCourses()    { return courses; }
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
                    "Adding '" + course.getName() + "' (" + course.getCredits()
                    + " cr) would exceed the " + MAX_CREDITS + "-credit limit");
        if (!courses.contains(course)) {
            courses.add(course);
            enrolledCredits += course.getCredits();
        }
    }

    public void dropCourse(Course course) {
        if (courses.remove(course)) enrolledCredits -= course.getCredits();
    }

    public void recordFail() { failCount++; }

    public void assignResearchSupervisor(IResearcher supervisor) {
        if (year != SUPERVISOR_YEAR)
            throw new IllegalStateException("Only 4th-year students can choose a research supervisor");
        if (supervisor == null || !supervisor.isResearcher())
            throw new NotAResearcherException("Supervisor must be an active researcher");
        if (supervisor.getHIndex() < MIN_H_INDEX)
            throw new LowHIndexException(
                    "Supervisor h-index " + supervisor.getHIndex()
                    + " is below required minimum of " + MIN_H_INDEX);
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
        if (year == SUPERVISOR_YEAR)
            System.out.println("  7  - Choose research supervisor");
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
            case "7" -> { if (year == SUPERVISOR_YEAR) chooseSupervisor(in, services);
                          else return false; }
            default  -> { return false; }
        }
        return true;
    }

    private void registerForCourse(Scanner in, Services services) {
        List<Course> available = services.getCourseService().getCoursesForMajorAndYear(major, year);
        if (available.isEmpty()) {
            System.out.println("No open courses found for " + major + ", year " + year + ".");
            return;
        }
        available.forEach(c -> System.out.println("  " + c));
        System.out.print("Course ID: ");
        try {
            services.getCourseService().requestRegistration(getUsername(), in.nextLine().trim());
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
            System.out.print("Rating (1-5): ");
            teacher.addRating(Integer.parseInt(in.nextLine().trim()));
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
        List<IResearcher> researchers = services.getResearchService().getAllResearchers();
        if (researchers.isEmpty()) {
            System.out.println("No researchers found.");
            return;
        }
        for (int i = 0; i < researchers.size(); i++) {
            IResearcher r = researchers.get(i);
            if (r == this) continue;
            String label = r instanceof User user
                    ? user.getFullName() + " (" + user.getUsername() + ")"
                    : r.toString();
            System.out.printf("%d. %s | h-index: %d%n", i + 1, label, r.getHIndex());
        }
        try {
            System.out.print("Researcher number: ");
            int index = Integer.parseInt(in.nextLine().trim()) - 1;
            if (index < 0 || index >= researchers.size()) {
                System.out.println("Invalid researcher number.");
                return;
            }
            assignResearchSupervisor(researchers.get(index));
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

    private Course canonicalCourse(Course course, Services services) {
        Course current = services.getCourseService().findById(course.getCourseId());
        return current != null ? current : course;
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
