package Enums;

public enum Major {
    COMPUTER_SCIENCE("Computer Science"),
    SOFTWARE_ENGINEERING("Software Engineering"),
    INFORMATION_SYSTEMS("Information Systems"),
    DATA_SCIENCE("Data Science"),
    MATHEMATICS("Mathematics"),
    PHYSICS("Physics"),
    MANAGEMENT("Management"),
    ECONOMICS("Economics");

    private final String displayName;

    Major(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }

    @Override
    public String toString() { return displayName; }
}
