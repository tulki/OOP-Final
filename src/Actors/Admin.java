package Actors;

import Enums.LogEventType;
import Enums.Major;
import Enums.ManagerType;
import Enums.School;
import Enums.TeacherTitle;
import Enums.YearLevel;
import Interfaces.IResearcher;
import Models.ResearchProfile;
import Services.MenuHelper;
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
        System.out.println("  1  - Manage users");
        System.out.println("  2  - View system logs");
    }

    @Override
    protected boolean handleRoleSpecificChoice(String choice, Scanner in, Services services) {
        switch (choice) {
            case "1" -> manageUsers(in, services);
            case "2" -> viewSystemLogs();
            default ->{ return false; }
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
            System.out.println("  g - Grant/revoke researcher status");
            System.out.println("  b - Back");
            System.out.print("> ");

            String choice = in.nextLine().trim().toLowerCase();
            switch (choice) {
                case "l" -> listUsers(services);
                case "a" -> addUser(in, services);
                case "m" -> modifyUser(in, services);
                case "r" -> removeUser(in, services);
                case "g" -> grantRevokeResearcher(in, services);
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
            System.out.println("  1 - Admin");
            System.out.println("  2 - Student");
            System.out.println("  3 - Teacher");
            System.out.println("  4 - Manager");
            System.out.println("  5 - Dean");
            System.out.println("  6 - Rector");
            System.out.println("  7 - Researcher");
            System.out.print("Role number: ");
            String role = switch (in.nextLine().trim()) {
                case "1" -> "admin";
                case "2" -> "student";
                case "3" -> "teacher";
                case "4" -> "manager";
                case "5" -> "dean";
                case "6" -> "rector";
                case "7" -> "researcher";
                default ->"";
            };
            if (role.isEmpty()) {
                System.out.println("Invalid role number.");
                return;
            }
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
                case "researcher" -> createResearcher(in, username, passwordHash, fullName);
                default -> null;
            };

            if (user == null) {
                System.out.println("Unsupported role.");
                return;
            }
            services.getUserRepository().addUser(user);
            services.getLogger().log(LogEventType.ACTION,
                    "admin " + getUsername() + " added user " + username + " [" + role + "]");
            services.saveAll();
            System.out.println("User added: " + user);
        } catch (RuntimeException e) {
            System.out.println("Could not add user: " + e.getMessage());
        }
    }

    private Student createStudent(Scanner in, String username, String passwordHash, String fullName) {
        Major major = MenuHelper.select(in, "Major number: ", Major.values());
        YearLevel year = MenuHelper.select(in, "Year number: ", YearLevel.values());
        return new Student(username, passwordHash, fullName, major, year);
    }

    private Teacher createTeacher(Scanner in, String username, String passwordHash, String fullName) {
        School school = MenuHelper.select(in, "School number: ", School.values());
        TeacherTitle title = MenuHelper.select(in, "Title number: ", TeacherTitle.values());
        return new Teacher(username, passwordHash, fullName, school, title);
    }

    private Manager createManager(Scanner in, String username, String passwordHash, String fullName) {
        ManagerType type = MenuHelper.select(in, "Manager type number: ", ManagerType.values());
        School school = null;
        if (type == ManagerType.DEPARTMENT) {
            school = MenuHelper.select(in, "School number: ", School.values());
        }
        return new Manager(username, passwordHash, fullName, type, school);
    }

    private Employee createResearcher(Scanner in, String username, String passwordHash, String fullName) {
        School school = MenuHelper.select(in, "School number: ", School.values());
        Employee employee = new Employee(username, passwordHash, fullName, school);
        employee.setResearchProfile(new Models.ResearchProfile(0, school.getDisplayName()));
        return employee;
    }

    private Dean createDean(Scanner in, String username, String passwordHash, String fullName) {
        School school = MenuHelper.select(in, "School number: ", School.values());
        return new Dean(username, passwordHash, fullName, school);
    }

    private Rector createRector(Scanner in, String username, String passwordHash, String fullName) {
        School school = MenuHelper.select(in, "School number: ", School.values());
        return new Rector(username, passwordHash, fullName, school);
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
        services.getLogger().log(LogEventType.ACTION,
                "admin " + getUsername() + " modified user " + user.getUsername());
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
        User user = services.getUserRepository().findByUsername(username).orElse(null);
        if (user == null) {
            System.out.println("User not found.");
            return;
        }
        if (user instanceof Student) {
            services.getCourseService().removePendingFor(username);
        } else if (user instanceof Teacher) {
            services.getCourseService().removeInstructorFor(username);
        }
        services.getUserRepository().removeUser(username);
        services.getLogger().log(LogEventType.ACTION,
                "admin " + getUsername() + " removed user " + username);
        services.saveAll();
        System.out.println("User removed.");
    }

    private void grantRevokeResearcher(Scanner in, Services services) {
        System.out.print("Username: ");
        User user = services.getUserRepository().findByUsername(in.nextLine().trim()).orElse(null);
        if (user == null) { System.out.println("User not found."); return; }
        if (!(user instanceof IResearcher researcher)) {
            System.out.println("This role cannot be a researcher.");
            return;
        }
        if (researcher.isResearcher()) {
            System.out.print("Already a researcher. Revoke status? (y/n): ");
            if ("y".equalsIgnoreCase(in.nextLine().trim())) {
                researcher.setResearchProfile(null);
                services.getLogger().log(LogEventType.ACTION,
                        "admin " + getUsername() + " revoked researcher status from " + user.getUsername());
                services.saveAll();
                System.out.println("Researcher status revoked.");
            }
            return;
        }
        try {
            System.out.print("h-index: ");
            int hIndex = Integer.parseInt(in.nextLine().trim());
            System.out.print("Research school: ");
            String school = in.nextLine().trim();
            researcher.setResearchProfile(new ResearchProfile(hIndex, school));
            services.getLogger().log(LogEventType.ACTION,
                    "admin " + getUsername() + " granted researcher status to " + user.getUsername());
            services.saveAll();
            System.out.println("Researcher status granted to " + user.getUsername() + ".");
        } catch (RuntimeException e) {
            System.out.println("Could not grant researcher status: " + e.getMessage());
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
