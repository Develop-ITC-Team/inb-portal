package gov.kh.moi.inb.exception;

/** Thrown on unique-constraint conflicts (e.g. duplicate slug). Mapped to HTTP 409. */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
