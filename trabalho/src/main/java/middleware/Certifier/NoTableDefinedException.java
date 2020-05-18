package middleware.Certifier;

public class NoTableDefinedException extends Exception{
    public NoTableDefinedException(String errorMsg){
        super(errorMsg);
    }
}
