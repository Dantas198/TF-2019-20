package middleware.message;

public class ErrorMessage extends ContentMessage<Throwable> {
    public ErrorMessage(Throwable body) {
        super(body);
    }
}
