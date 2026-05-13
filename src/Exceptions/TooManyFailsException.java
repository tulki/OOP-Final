package Exceptions;

public class TooManyFailsException extends RuntimeException {
    public TooManyFailsException(String message) { super(message); }
}
