package Enums;

public enum School {
    SCHOOL_OF_IT("School of Information Technologies"),
    SCHOOL_OF_ENERGY("School of Energy and Environmental Engineering"),
    SCHOOL_OF_ECONOMICS("School of Economics and Management"),
    SCHOOL_OF_SCIENCE("School of Science and Technology"),
    SCHOOL_OF_CHEMICAL_ENGINEERING("School of Chemical Engineering"),
    SCHOOL_OF_MINING("School of Mining and Geological Engineering"),
    GRADUATE_SCHOOL("Graduate School");

    private final String displayName;

    School(String displayName) { this.displayName = displayName; }

    public String getDisplayName() { return displayName; }

    @Override
    public String toString() { return displayName; }
}
