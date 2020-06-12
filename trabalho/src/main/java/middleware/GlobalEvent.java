package middleware;

import java.io.Serializable;

public class GlobalEvent implements Serializable {
    private final String type;
    private final int time;

    public GlobalEvent(String type, int time){
        this.time = time;
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public int getTime() {
        return time;
    }
}
