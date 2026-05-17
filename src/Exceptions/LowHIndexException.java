package Exceptions;

public class LowHIndexException extends RuntimeException {

    private final int actual;
    private final int required;

    public LowHIndexException(int actual, int required) {
        super(String.format(
                "Supervisor h-index %d is below the required minimum of %d.", actual, required));
        this.actual = actual;
        this.required = required;
    }

    public int getActual() { return actual; }
    public int getRequired() { return required; }
}
