package Services;

import Actors.Student;
import Actors.User;
import Models.Mark;

import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportService implements Serializable {
    private static final long serialVersionUID = 1L;

    private final MarkService markService;
    private final UserRepository userRepository;

    public ReportService(MarkService markService, UserRepository userRepository) {
        this.markService = markService;
        this.userRepository = userRepository;
    }

    public void generateAcademicPerformanceReport() {
        List<Student> students = userRepository.getAllUsers().stream()
                .filter(u -> u instanceof Student)
                .map(u -> (Student) u)
                .sorted(Comparator.comparing(User::getFullName))
                .toList();

        if (students.isEmpty()) {
            System.out.println("No students found.");
            return;
        }

        Map<Enums.Major, Double> gpaByMajor = new HashMap<>();
        Map<Enums.Major, Integer> countByMajor = new HashMap<>();
        for (Student student : students) {
            double gpa = markService.getGpa(student.getUsername());
            gpaByMajor.merge(student.getMajor(), gpa, Double::sum);
            countByMajor.merge(student.getMajor(), 1, Integer::sum);
        }

        System.out.println("=== Academic performance report ===");
        for (Student student : students) {
            System.out.printf("  %s | %s | %s %s | GPA %.2f | fails %d%n",
                    student.getUsername(), student.getFullName(), student.getMajor(), student.getYear(),
                    markService.getGpa(student.getUsername()), markService.getFailCount(student.getUsername()));
        }

        System.out.println("--- Average GPA by major ---");
        for (Enums.Major major : gpaByMajor.keySet()) {
            System.out.printf("  %s: %.2f%n", major, gpaByMajor.get(major) / countByMajor.get(major));
        }

        System.out.println("--- Top students by GPA ---");
        students.stream()
                .sorted(Comparator.comparingDouble(
                        (Student s) -> markService.getGpa(s.getUsername())).reversed())
                .limit(5)
                .forEach(s -> System.out.printf("  %s | %s | GPA %.2f%n",
                        s.getUsername(), s.getFullName(), markService.getGpa(s.getUsername())));
    }

    public void generateTranscript(String studentUsername) {
        User user = userRepository.findByUsername(studentUsername).orElse(null);
        if (!(user instanceof Student student)) {
            System.out.println("Student not found: " + studentUsername);
            return;
        }

        List<Map.Entry<String, Mark>> studentMarks = markService.getMarksForStudent(studentUsername);
        if (studentMarks.isEmpty()) {
            System.out.println("No marks found for " + studentUsername);
            return;
        }
        System.out.println("=== Transcript: " + student.getFullName() + " (" + studentUsername + ") ===");
        for (Map.Entry<String, Mark> e : studentMarks) {
            String courseId = e.getKey().split(":")[1];
            System.out.printf("  %-15s  %s%n", courseId, e.getValue());
        }
        System.out.printf("  GPA: %.2f%n", markService.getGpa(studentUsername));
    }
}

