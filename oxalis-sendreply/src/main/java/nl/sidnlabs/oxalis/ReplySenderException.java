package nl.sidnlabs.oxalis;

public class ReplySenderException extends Exception {
    public ReplySenderException() {
    }

    public ReplySenderException(String message) {
        super(message);
    }

    public ReplySenderException(Exception exc) {
        super(exc);
    }
}
