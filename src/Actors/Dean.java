package Actors;

import Enums.ManagerType;

public class Dean extends Manager {
    private static final long serialVersionUID = 1L;

    public Dean(String username, String passwordHash, String fullName, String department) {
        super(username, passwordHash, fullName, department, ManagerType.DEAN);
    }
}
