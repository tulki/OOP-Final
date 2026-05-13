package Services;

import Assets.Mark;
import Exceptions.TooManyFailsException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MarkService implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final String DATA_FILE = "data/marks.dat";

    private Map<String, Mark> marks;
    private Map<String, Integer> failCounts;

    public MarkService() {
        marks = new HashMap<>();
        failCounts = new HashMap<>();
    }

    public static MarkService load() {
        File file = new File(DATA_FILE);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                return (MarkService) ois.readObject();
            } catch (Exception e) {
                System.err.println("Failed to load marks.dat, starting fresh: " + e.getMessage());
            }
        }
        return new MarkService();
    }

    public void save() {
        new File("data").mkdirs();
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            oos.writeObject(this);
        } catch (IOException e) {
            System.err.println("Failed to save marks.dat: " + e.getMessage());
        }
    }

    public void setMark(String studentUsername, String courseId, Mark mark) {
        String key = key(studentUsername, courseId);
        Mark existing = marks.get(key);

        boolean wasFailed = existing != null && !existing.isPassed();
        boolean isFailed = !mark.isPassed();
        if (!wasFailed && isFailed) {
            int fails = failCounts.merge(studentUsername, 1, Integer::sum);
            if (fails > 3) {
                throw new TooManyFailsException(
                        studentUsername + " has exceeded the maximum of 3 course failures");
            }
        }

        marks.put(key, mark);
    }

    public Mark getMark(String studentUsername, String courseId) {
        return marks.get(key(studentUsername, courseId));
    }

    public List<Map.Entry<String, Mark>> getMarksForStudent(String studentUsername) {
        List<Map.Entry<String, Mark>> result = new ArrayList<>();
        for (Map.Entry<String, Mark> e : marks.entrySet())
            if (e.getKey().startsWith(studentUsername + ":"))
                result.add(e);
        return result;
    }

    public double getGpa(String studentUsername) {
        List<Map.Entry<String, Mark>> studentMarks = getMarksForStudent(studentUsername);
        if (studentMarks.isEmpty()) return 0.0;

        double total = 0.0;
        for (Map.Entry<String, Mark> entry : studentMarks) {
            total += gradePoint(entry.getValue());
        }
        return total / studentMarks.size();
    }

    public int getFailCount(String studentUsername) {
        return failCounts.getOrDefault(studentUsername, 0);
    }

    private double gradePoint(Mark mark) {
        double total = mark.getTotal();
        if (total >= 90) return 4.0;
        if (total >= 80) return 3.0;
        if (total >= 70) return 2.0;
        if (total >= 60) return 1.0;
        return 0.0;
    }

    private String key(String studentUsername, String courseId) {
        return studentUsername + ":" + courseId;
    }
}
