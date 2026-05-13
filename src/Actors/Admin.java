package Actors;

import Enums.ManagerType;
import Enums.TeacherTitle;
import Services.Services;
import Services.UserRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

public class Admin extends User {
    private static final long serialVersionUID = 1L;

    public Admin(String username, String passwordHash, String fullName) {
        super(username, passwordHash, fullName);
    }

    @Override
    protected void printRoleSpecificMenu() {
        System.out.println("--- Admin ---");
        System.out.println("  1  - Manage users (add / modify / remove)");
        System.out.println("  2  - View system logs");
    }

    @Override
    protected boolean handleRoleSpecificChoice(String choice, Scanner in, Services services) {
        switch (choice) {
            case "1" -> manageUsers(in, services);
            case "2" -> viewSystemLogs();
            default  -> { return false; }
        }
        return true;
    }

    private void manageUsers(Scanner in, Services services) {
        while (true) {
            System.out.println("\n=== Users ===");
            System.out.println("  l - List all");
            System.out.println("  a - Add user");
            System.out.println("  m - Modify user");
            System.out.println("  r - Remove user");
            System.out.println("  b - Back");
            System.out.print("> ");

            String choice = in.nextLine().trim().toLowerCase();
            switch (choice) {
                case "l" -> listUsers(services);
                case "a" -> addUser(in, services);
                case "m" -> modifyUser(in, services);
                case "r" -> removeUser(in, services);
                case "b" -> { return; }
                default -> System.out.println("Unknown option.");
            }
        }
    }

    private void listUsers(Services services) {
        List<User> users = services.getUserRepository().getAllUsers();
        if (users.isEmpty()) {
            System.out.println("No users found.");
            return;
        }
        for (User user : users) System.out.println("  " + user);
    }

    private void addUser(Scanner in, Services services) {
        try {
            System.out.print("Role (admin/student/teacher/manager/dean/rector): ");
            String role = in.nextLine().trim().toLowerCase();
            System.out.print("Username: ");
            String username = in.nextLine().trim();
            if (services.getUserRepository().findByUsername(username).isPresent()) {
                System.out.println("Username already exists.");
                return;
            }
            System.out.print("Password: ");
            String passwordHash = UserRepository.hashPassword(in.nextLine().trim());
            System.out.print("Full name: ");
            String fullName = in.nextLine().trim();

            User user = switch (role) {
                case "admin" -> new Admin(username, passwordHash, fullName);
                case "student" -> createStudent(in, username, passwordHash, fullName);
                case "teacher" -> createTeacher(in, username, passwordHash, fullName);
                case "manager" -> createManager(in, username, passwordHash, fullName);
                case "dean" -> createDean(in, username, passwordHash, fullName);
                case "rector" -> createRector(in, username, passwordHash, fullName);
                default -> null;
            };

            if (user == null) {
                System.out.println("Unsupported role.");
                return;
            }
            services.getUserRepository().addUser(user);
            services.saveAll();
            System.out.println("User added: " + user);
        } catch (RuntimeException e) {
            System.out.println("Could not add user: " + e.getMessage());
        }
    }

    private Student createStudent(Scanner in, String username, String passwordHash, String fullName) {
        System.out.print("Major: ");
        String major = in.nextLine().trim();
        System.out.print("Year: ");
        int year = Integer.parseInt(in.nextLine().trim());
        return new Student(username, passwordHash, fullName, major, year);
    }

    private Teacher createTeacher(Scanner in, String username, String passwordHash, String fullName) {
        System.out.print("Department: ");
        String department = in.nextLine().trim();
        System.out.print("Title (TUTOR/SENIOR_LECTOR/PROFESSOR): ");
        TeacherTitle title = TeacherTitle.valueOf(in.nextLine().trim().toUpperCase());
        return new Teacher(username, passwordHash, fullName, department, title);
    }

    private Manager createManager(Scanner in, String username, String passwordHash, String fullName) {
        System.out.print("Department: ");
        String department = in.nextLine().trim();
        System.out.print("Manager type (OR/DEPARTMENT/DEAN/RECTOR): ");
        ManagerType type = ManagerType.valueOf(in.nextLine().trim().toUpperCase());
        return new Manager(username, passwordHash, fullName, department, type);
    }

    private Dean createDean(Scanner in, String username, String passwordHash, String fullName) {
        System.out.print("Department: ");
        return new Dean(username, passwordHash, fullName, in.nextLine().trim());
    }

    private Rector createRector(Scanner in, String username, String passwordHash, String fullName) {
        System.out.print("Department: ");
        return new Rector(username, passwordHash, fullName, in.nextLine().trim());
    }

    private void modifyUser(Scanner in, Services services) {
        System.out.print("Username: ");
        User user = services.getUserRepository().findByUsername(in.nextLine().trim()).orElse(null);
        if (user == null) {
            System.out.println("User not found.");
            return;
        }

        System.out.println("  1 - Change full name");
        System.out.println("  2 - Reset password");
        System.out.print("> ");
        String choice = in.nextLine().trim();
        if ("1".equals(choice)) {
            System.out.print("New full name: ");
            user.setFullName(in.nextLine().trim());
        } else if ("2".equals(choice)) {
            System.out.print("New password: ");
            user.changePassword(in.nextLine().trim());
        } else {
            System.out.println("Unknown option.");
            return;
        }
        services.saveAll();
        System.out.println("User updated.");
    }

    private void removeUser(Scanner in, Services services) {
        System.out.print("Username: ");
        String username = in.nextLine().trim();
        if (getUsername().equalsIgnoreCase(username)) {
            System.out.println("Cannot remove current admin account.");
            return;
        }
        boolean removed = services.getUserRepository().removeUser(username);
        if (removed) {
            services.saveAll();
            System.out.println("User removed.");
        } else {
            System.out.println("User not found.");
        }
    }

    private void viewSystemLogs() {
        Path path = Path.of("data", "logs.txt");
        if (!Files.exists(path)) {
            System.out.println("No logs found.");
            return;
        }
        try {
            Files.lines(path).forEach(System.out::println);
        } catch (IOException e) {
            System.out.println("Could not read logs: " + e.getMessage());
        }
    }
}
