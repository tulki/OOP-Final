package Exceptions;

public class TooManyFailsException extends RuntimeException {

    private final String studentUsername;
    private final int failCount;
    private final int maxFails;

    public TooManyFailsException(String studentUsername, int failCount, int maxFails) {
        super(String.format(
                "Student '%s' has reached the maximum of %d course failures (attempted %d).",
                studentUsername, maxFails, failCount));
        this.studentUsername = studentUsername;
        this.failCount = failCount;
        this.maxFails = maxFails;
    }

    public String getStudentUsername() { return studentUsername; }
    public int getFailCount() { return failCount; }
    public int getMaxFails() { return maxFails; }
}
