package Exceptions;

public class NotAResearcherException extends RuntimeException {

    private final String username;

    public NotAResearcherException(String username) {
        super(String.format(
                "User '%s' is not registered as a researcher.", username));
        this.username = username;
    }

    /** Used when the offending entity is not a User (e.g., an unknown object). */
    public NotAResearcherException() {
        super("The given entity is not a registered researcher.");
        this.username = null;
    }

    public String getUsername() { return username; }
}
