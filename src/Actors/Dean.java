package Actors;

import Enums.School;

public class Dean extends Signatory {
    private static final long serialVersionUID = 1L;

    public Dean(String username, String passwordHash, String fullName, School school) {
        super(username, passwordHash, fullName, school);
    }

    @Override
    protected String getRoleName() { return "Dean"; }
}
