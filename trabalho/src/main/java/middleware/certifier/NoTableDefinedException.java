package middleware.certifier;

public class NoTableDefinedException extends Exception{
    public NoTableDefinedException(String errorMsg){
        super(errorMsg);
    }
}
