package Actors;

import Assets.ResearchProfile;
import Enums.LogEventType;
import Interfaces.IResearcher;
import Services.Services;

import java.util.Scanner;

public abstract class Employee extends User implements IResearcher {
    private static final long serialVersionUID = 1L;

    private String department;
    private ResearchProfile researchProfile;

    public Employee(String username, String passwordHash, String fullName, String department) {
        super(username, passwordHash, fullName);
        this.department = department;
    }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    @Override
    public ResearchProfile getResearchProfile() { return researchProfile; }

    @Override
    public void setResearchProfile(ResearchProfile researchProfile) {
        this.researchProfile = researchProfile;
    }

    protected void sendComplaint(Scanner in, Services services) {
        System.out.print("Complaint text: ");
        String text = in.nextLine().trim();
        if (text.isBlank()) {
            System.out.println("Complaint cannot be empty.");
            return;
        }
        services.getLogger().log(LogEventType.ACTION,
                "complaint from " + getUsername() + ": " + text);
        System.out.println("Complaint recorded in system logs.");
    }

    protected void manageOwnRequests(Scanner in, Services services) {
        System.out.print("Request text: ");
        String text = in.nextLine().trim();
        if (text.isBlank()) {
            System.out.println("Request cannot be empty.");
            return;
        }
        services.getLogger().log(LogEventType.ACTION,
                "employee request from " + getUsername() + ": " + text);
        System.out.println("Request recorded in system logs.");
    }
}
