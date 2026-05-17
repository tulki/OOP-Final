package Actors;

import Enums.LogEventType;
import Enums.RequestType;
import Enums.School;
import Models.Request;
import Services.Services;

import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

abstract class Signatory extends Employee {

    protected Signatory(String username, String passwordHash, String fullName, School school) {
        super(username, passwordHash, fullName, school);
    }

    protected abstract String getRoleName();

    @Override
    protected void printRoleSpecificMenu() {
        System.out.println("--- " + getRoleName() + " ---");
        System.out.println("  1  - View academic performance report");
        System.out.println("  2  - View student & teacher info");
        System.out.println("  3  - Process employee requests");
        System.out.println("  4  - Process employee complaints");
        System.out.println("  5  - Send complaint");
        System.out.println("  6  - Manage my requests");
    }

    @Override
    protected boolean handleRoleSpecificChoice(String choice, Scanner in, Services services) {
        switch (choice) {
            case "1" -> services.getReportService().generateAcademicPerformanceReport();
            case "2" -> viewStudentAndTeacherInfo(in, services);
            case "3" -> processRequestsOfType(in, services, RequestType.GENERAL, "General Requests");
            case "4" -> processRequestsOfType(in, services, RequestType.COMPLAINT, "Complaints");
            case "5" -> sendComplaint(in, services);
            case "6" -> manageOwnRequests(in, services);
            default ->{ return false; }
        }
        return true;
    }

    private void viewStudentAndTeacherInfo(Scanner in, Services services) {
        System.out.println("  s - Students");
        System.out.println("  t - Teachers");
        System.out.print("> ");
        String view = in.nextLine().trim().toLowerCase();

        if ("s".equals(view)) {
            System.out.println("Sort by: 1=name, 2=GPA, 3=year");
            String sort = in.nextLine().trim();
            Comparator<Student> comparator = switch (sort) {
                case "2" -> Comparator.comparingDouble(
                        (Student s) -> services.getMarkService().getGpa(s.getUsername())).reversed();
                case "3" -> Comparator.comparing(Student::getYear);
                default ->Comparator.comparing(User::getFullName);
            };
            services.getUserRepository().getAllUsers().stream()
                    .filter(u -> u instanceof Student)
                    .map(u -> (Student) u)
                    .sorted(comparator)
                    .forEach(s -> System.out.printf("  %s | %s | %s %s | GPA %.2f%n",
                            s.getUsername(), s.getFullName(), s.getMajor(), s.getYear(),
                            services.getMarkService().getGpa(s.getUsername())));
        } else if ("t".equals(view)) {
            System.out.println("Sort by: 1=name, 2=rating");
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
            services.getLogger().log(LogEventType.ACTION,
                    getRoleName() + " " + getUsername()
                            + " " + ("a".equals(action) ? "approved" : "rejected")
                            + " request " + id);
            services.saveAll();
            System.out.println("Request processed by " + getRoleName() + ".");
        } else {
            System.out.println("Request not found or already processed.");
        }
    }
}
