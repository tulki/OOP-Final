package Enums;

public enum YearLevel {
    YEAR_1("Year 1"),
    YEAR_2("Year 2"),
    YEAR_3("Year 3"),
    YEAR_4("Year 4");

    private final String displayName;

    YearLevel(String displayName) { this.displayName = displayName; }

    public String getDisplayName() { return displayName; }

    public int getNumber() { return ordinal() + 1; }

    @Override
    public String toString() { return displayName; }
}
