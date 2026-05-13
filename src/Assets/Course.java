package Assets;

import Enums.CourseStatus;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Course implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String courseId;
    private String name;
    private String major;
    private int yearLevel;
    private int credits;
    private CourseStatus status;
    private List<String> instructorUsernames;
    private List<Lesson> lessons;

    public Course(String courseId, String name, String major, int yearLevel, int credits) {
        this.courseId = courseId;
        this.name = name;
        this.major = major;
        this.yearLevel = yearLevel;
        this.credits = credits;
        this.status = CourseStatus.OPEN;
        this.instructorUsernames = new ArrayList<>();
        this.lessons = new ArrayList<>();
    }

    public String getCourseId() { return courseId; }
    public String getName() { return name; }
    public String getMajor() { return major; }
    public int getYearLevel() { return yearLevel; }
    public int getCredits() { return credits; }
    public CourseStatus getStatus() { return status; }
    public List<String> getInstructorUsernames() { return instructorUsernames; }
    public List<Lesson> getLessons() { return lessons; }

    public void setName(String name) { this.name = name; }
    public void setMajor(String major) { this.major = major; }
    public void setYearLevel(int yearLevel) { this.yearLevel = yearLevel; }
    public void setCredits(int credits) { this.credits = credits; }
    public void setStatus(CourseStatus status) { this.status = status; }

    public void addInstructor(String teacherUsername) { instructorUsernames.add(teacherUsername); }
    public void addLesson(Lesson lesson) { lessons.add(lesson); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Course)) return false;
        return courseId.equals(((Course) o).courseId);
    }

    @Override
    public int hashCode() { return courseId.hashCode(); }

    @Override
    public String toString() {
        return String.format("[%s] %s | Major: %s | Year: %d | Credits: %d | %s",
                courseId, name, major, yearLevel, credits, status);
    }
}
