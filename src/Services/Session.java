package Services;

import Actors.User;
import Enums.LogEventType;

import java.util.Scanner;

public class Session {
    private final Services services;
    private final Scanner scanner;
    private static final int MAX_ATTEMPTS = 3;

    public Session(Services services, Scanner scanner) {
        this.services = services;
        this.scanner = scanner;
    }

    public void run() {
        Logger log = services.getLogger();
        log.log(LogEventType.SESSION_STARTED, "Application started");
        System.out.println("==========================================");
        System.out.println("  Research-Oriented University System");
        System.out.println("==========================================");

        User user = authenticate(log);
        if (user == null) {
            log.log(LogEventType.SESSION_ENDED, "Session ended - authentication failed");
            log.close();
            return;
        }

        log.log(LogEventType.LOGIN_SUCCESS, "user=" + user.getUsername());
        System.out.println("\nWelcome, " + user.getFullName() + "!\n");
        user.showMenu(scanner, services);

        log.log(LogEventType.LOGOUT, "user=" + user.getUsername());
        services.saveAll();
        log.log(LogEventType.SESSION_ENDED, "user=" + user.getUsername() + " logged out");
        log.close();
        System.out.println("\nGoodbye!");
    }

    private User authenticate(Logger log) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            System.out.print("Username: ");
            String username = scanner.nextLine().trim();
            log.log(LogEventType.LOGIN_ATTEMPT, "user=" + username + " attempt=" + attempt);

            User user = services.getUserRepository().findByUsername(username).orElse(null);
            if (user == null) {
                System.out.println("User not found.");
                log.log(LogEventType.LOGIN_FAILED, "user=" + username + " (not found)");
                continue;
            }

            System.out.print("Password: ");
            String password = scanner.nextLine().trim();
            if (user.verifyPassword(password)) return user;

            System.out.println("Wrong password. Attempts left: " + (MAX_ATTEMPTS - attempt));
            log.log(LogEventType.LOGIN_FAILED, "user=" + username + " (wrong password)");
        }
        System.out.println("Too many failed attempts. Exiting.");
        return null;
    }
}
