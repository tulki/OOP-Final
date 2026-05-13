package Exceptions;

public class CreditLimitExceededException extends RuntimeException {
    public CreditLimitExceededException(String message) { super(message); }
}
