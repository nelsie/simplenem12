public class InvalidNem12FileException extends RuntimeException{
    public InvalidNem12FileException(String message) {
        super(message);
    }

    public InvalidNem12FileException(Throwable cause) {
        super(cause);
    }
}
