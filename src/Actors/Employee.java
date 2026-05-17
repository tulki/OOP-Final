package Actors;

import Enums.LogEventType;
import Enums.RequestType;
import Enums.School;
import Models.Request;
import Models.ResearchProfile;
import Interfaces.IResearcher;
import Services.Services;

import java.util.List;
import java.util.Scanner;

public class Employee extends User implements IResearcher {
    private static final long serialVersionUID = 1L;

    private School school;
    private ResearchProfile researchProfile;

    public Employee(String username, String passwordHash, String fullName, School school) {
        super(username, passwordHash, fullName);
        this.school = school;
    }

    public School getSchool() { return school; }
    public void setSchool(School school) { this.school = school; }

    @Override
    public ResearchProfile getResearchProfile() { return researchProfile; }

    @Override
    public void setResearchProfile(ResearchProfile researchProfile) {
        this.researchProfile = researchProfile;
    }

    @Override
    protected void printRoleSpecificMenu() {
        System.out.println("--- Researcher ---");
        if (isResearcher())
            System.out.println("  1  - Research block");
        System.out.println("  2  - Send complaint");
        System.out.println("  3  - Manage my requests");
    }

    @Override
    protected boolean handleRoleSpecificChoice(String choice, Scanner in, Services services) {
        switch (choice) {
            case "1" -> { if (isResearcher()) manageResearch(in, services); else return false; }
            case "2" -> sendComplaint(in, services);
            case "3" -> manageOwnRequests(in, services);
            default -> { return false; }
        }
        return true;
    }

    protected void sendComplaint(Scanner in, Services services) {
        System.out.println("\n=== Submit Complaint ===");
        System.out.print("Complaint text: ");
        String text = in.nextLine().trim();
        if (text.isBlank()) {
            System.out.println("Complaint cannot be empty.");
            return;
        }
        Request req = services.getRequestService().submit(getUsername(), RequestType.COMPLAINT, text);
        services.getLogger().log(LogEventType.ACTION,
                "complaint submitted by " + getUsername() + " [" + req.getRequestId() + "]");
        services.saveAll();
        System.out.println("Complaint submitted. ID: " + req.getRequestId() + " | Status: PENDING.");
    }

    protected void manageOwnRequests(Scanner in, Services services) {
        while (true) {
            System.out.println("\n=== My Requests ===");
            System.out.println("  1 - Submit a general request");
            System.out.println("  2 - View my requests");
            System.out.println("  0 - Back");
            System.out.print("> ");

            switch (in.nextLine().trim()) {
                case "1" -> submitGeneralRequest(in, services);
                case "2" -> viewOwnRequests(services);
                case "0" -> { return; }
                default -> System.out.println("Unknown option.");
            }
        }
    }

    private void submitGeneralRequest(Scanner in, Services services) {
        System.out.print("Request text: ");
        String text = in.nextLine().trim();
        if (text.isBlank()) {
            System.out.println("Request cannot be empty.");
            return;
        }
        Request req = services.getRequestService().submit(getUsername(), RequestType.GENERAL, text);
        services.getLogger().log(LogEventType.ACTION,
                "general request submitted by " + getUsername() + " [" + req.getRequestId() + "]");
        services.saveAll();
        System.out.println("Request submitted. ID: " + req.getRequestId() + " | Status: PENDING.");
    }

    private void viewOwnRequests(Services services) {
        List<Request> mine = services.getRequestService().getByUsername(getUsername());
        if (mine.isEmpty()) {
            System.out.println("You have no submitted requests.");
            return;
        }
        mine.forEach(r -> {
            System.out.println("  " + r);
            System.out.println("    " + r.getContent());
        });
    }
}
