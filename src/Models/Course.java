package Models;

import Enums.CourseStatus;
import Enums.Major;
import Enums.YearLevel;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class Course implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String courseId;
    private String name;
    private Set<Major> majors;
    private Set<YearLevel> yearLevels;
    private int credits;
    private CourseStatus status;
    private List<String> instructorUsernames;
    private List<Lesson> lessons;

    public Course(String courseId, String name, int credits) {
        this.courseId = courseId;
        this.name = name;
        this.majors = new LinkedHashSet<>();
        this.yearLevels = new LinkedHashSet<>();
        this.credits = credits;
        this.status = CourseStatus.OPEN;
        this.instructorUsernames = new ArrayList<>();
        this.lessons = new ArrayList<>();
    }

    public String getCourseId() { return courseId; }
    public String getName() { return name; }
    public Set<Major> getMajors() { return Collections.unmodifiableSet(majors); }
    public Set<YearLevel> getYearLevels() { return Collections.unmodifiableSet(yearLevels); }
    public int getCredits() { return credits; }
    public CourseStatus getStatus() { return status; }
    public List<String> getInstructorUsernames() { return instructorUsernames; }
    public List<Lesson> getLessons() { return lessons; }

    public void setName(String name) { this.name = name; }
    public void setCredits(int credits) { this.credits = credits; }
    public void setStatus(CourseStatus status) { this.status = status; }

    public void addMajor(Major major) { majors.add(major); }
    public void setMajors(Set<Major> majors) { this.majors = new LinkedHashSet<>(majors); }

    public void addYearLevel(YearLevel year) { yearLevels.add(year); }
    public void setYearLevels(Set<YearLevel> yearLevels) { this.yearLevels = new LinkedHashSet<>(yearLevels); }

    public void addInstructor(String teacherUsername) { instructorUsernames.add(teacherUsername); }
    public void addLesson(Lesson lesson) { lessons.add(lesson); }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (majors == null) majors = new LinkedHashSet<>();
        if (yearLevels == null) yearLevels = new LinkedHashSet<>();
        if (instructorUsernames == null) instructorUsernames = new ArrayList<>();
        if (lessons == null) lessons = new ArrayList<>();
    }

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
        StringBuilder majSb = new StringBuilder();
        for (Major m : majors) {
            if (majSb.length() > 0) majSb.append(", ");
            majSb.append(m.getDisplayName());
        }
        StringBuilder yrSb = new StringBuilder();
        for (YearLevel y : yearLevels) {
            if (yrSb.length() > 0) yrSb.append(", ");
            yrSb.append(y);
        }
        return String.format("[%s] %s | Majors: %s | Years: %s | Credits: %d | %s",
                courseId, name,
                majSb.length() > 0 ? majSb : "none",
                yrSb.length() > 0 ? yrSb : "none",
                credits, status);
    }
}
