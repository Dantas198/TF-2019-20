package middleware;

import java.io.Serializable;

public interface Server {
    void start() throws Exception;
    void stop() throws Exception;
}
