package Services;

import Actors.Student;
import Actors.Teacher;
import Actors.User;
import Assets.Course;
import Enums.CourseStatus;
import Exceptions.CreditLimitExceededException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CourseService implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final String DATA_FILE = "data/courses.dat";

    private List<Course> courses;
    private List<String[]> pendingRegistrations;
    private transient UserRepository userRepository;

    public CourseService() {
        this(null);
    }

    public CourseService(UserRepository userRepository) {
        courses = new ArrayList<>();
        pendingRegistrations = new ArrayList<>();
        this.userRepository = userRepository;
    }

    public static CourseService load(UserRepository userRepository) {
        File file = new File(DATA_FILE);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                CourseService service = (CourseService) ois.readObject();
                service.setUserRepository(userRepository);
                return service;
            } catch (Exception e) {
                System.err.println("Failed to load courses.dat, starting fresh: " + e.getMessage());
            }
        }
        return new CourseService(userRepository);
    }

    public void save() {
        new File("data").mkdirs();
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            oos.writeObject(this);
        } catch (IOException e) {
            System.err.println("Failed to save courses.dat: " + e.getMessage());
        }
    }

    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
        if (courses == null) courses = new ArrayList<>();
        if (pendingRegistrations == null) pendingRegistrations = new ArrayList<>();
    }

    public void addCourse(Course course) {
        if (course != null && findById(course.getCourseId()) == null) courses.add(course);
    }

    public List<Course> getAllCourses() { return new ArrayList<>(courses); }

    public List<Course> getCoursesForMajorAndYear(String major, int year) {
        List<Course> result = new ArrayList<>();
        for (Course c : courses) {
            if (c.getStatus() == CourseStatus.OPEN
                    && c.getMajor().equalsIgnoreCase(major)
                    && c.getYearLevel() == year) {
                result.add(c);
            }
        }
        return result;
    }

    public Course findById(String courseId) {
        return courses.stream()
                .filter(c -> c.getCourseId().equalsIgnoreCase(courseId))
                .findFirst().orElse(null);
    }

    public void requestRegistration(String studentUsername, String courseId) {
        Course course = requireCourse(courseId);
        if (course.getStatus() != CourseStatus.OPEN) {
            throw new IllegalStateException("Course is not open for registration: " + courseId);
        }

        Student student = requireStudent(studentUsername);
        if (student.getCourses().contains(course)) {
            throw new IllegalStateException(studentUsername + " is already enrolled in " + courseId);
        }
        if (isRegistrationPending(studentUsername, courseId)) {
            throw new IllegalStateException("Registration request is already pending");
        }
        if (student.getEnrolledCredits() + course.getCredits() > Student.MAX_CREDITS) {
            throw new CreditLimitExceededException("Registration would exceed the 21-credit limit");
        }

        pendingRegistrations.add(new String[]{studentUsername, courseId});
        System.out.println("Registration request submitted (pending approval).");
    }

    public void approveRegistration(String studentUsername, String courseId) {
        Course course = requireCourse(courseId);
        Student student = requireStudent(studentUsername);

        if (!student.getCourses().contains(course)) student.addCourse(course);
        removePendingRegistration(studentUsername, courseId);
        System.out.println("Registration approved: " + studentUsername + " -> " + courseId);
    }

    public void rejectRegistration(String studentUsername, String courseId) {
        removePendingRegistration(studentUsername, courseId);
        System.out.println("Registration rejected for " + studentUsername);
    }

    public void assignTeacher(String courseId, String teacherUsername) {
        Course course = requireCourse(courseId);
        Teacher teacher = requireTeacher(teacherUsername);

        if (!course.getInstructorUsernames().contains(teacherUsername)) {
            course.addInstructor(teacherUsername);
        }
        if (!teacher.getCourses().contains(course)) teacher.addCourse(course);
        System.out.println("Teacher assigned: " + teacherUsername + " -> " + courseId);
    }

    public List<String[]> getPendingRegistrations() { return new ArrayList<>(pendingRegistrations); }

    private boolean isRegistrationPending(String studentUsername, String courseId) {
        for (String[] request : pendingRegistrations) {
            if (request[0].equalsIgnoreCase(studentUsername)
                    && request[1].equalsIgnoreCase(courseId)) {
                return true;
            }
        }
        return false;
    }

    private void removePendingRegistration(String studentUsername, String courseId) {
        pendingRegistrations.removeIf(r ->
                r[0].equalsIgnoreCase(studentUsername) && r[1].equalsIgnoreCase(courseId));
    }

    private Course requireCourse(String courseId) {
        Course course = findById(courseId);
        if (course == null) throw new IllegalArgumentException("Course not found: " + courseId);
        return course;
    }

    private Student requireStudent(String username) {
        User user = requireUser(username);
        if (!(user instanceof Student student)) {
            throw new IllegalArgumentException("User is not a student: " + username);
        }
        return student;
    }

    private Teacher requireTeacher(String username) {
        User user = requireUser(username);
        if (!(user instanceof Teacher teacher)) {
            throw new IllegalArgumentException("User is not a teacher: " + username);
        }
        return teacher;
    }

    private User requireUser(String username) {
        if (userRepository == null) throw new IllegalStateException("UserRepository is not connected");
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }
}
