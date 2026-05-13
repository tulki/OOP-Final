package Actors;

import Enums.ManagerType;

public class Rector extends Manager {
    private static final long serialVersionUID = 1L;

    public Rector(String username, String passwordHash, String fullName, String department) {
        super(username, passwordHash, fullName, department, ManagerType.RECTOR);
    }
}
