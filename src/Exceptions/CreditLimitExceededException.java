package Exceptions;

public class CreditLimitExceededException extends RuntimeException {

    private final String courseName;
    private final int courseCredits;
    private final int currentCredits;
    private final int maxCredits;

    public CreditLimitExceededException(String courseName, int courseCredits,
                                        int currentCredits, int maxCredits) {
        super(String.format(
                "Cannot register for '%s' (%d cr): current load %d cr would exceed the %d-credit limit.",
                courseName, courseCredits, currentCredits + courseCredits, maxCredits));
        this.courseName = courseName;
        this.courseCredits = courseCredits;
        this.currentCredits = currentCredits;
        this.maxCredits = maxCredits;
    }

    public String getCourseName() { return courseName; }
    public int getCourseCredits() { return courseCredits; }
    public int getCurrentCredits() { return currentCredits; }
    public int getMaxCredits() { return maxCredits; }
}
