package middleware.message;

import java.io.Serializable;

public class StateMessage extends Message {
    private String serverName;

    public StateMessage(Serializable body, String serverName) {
        super(body);
        this.serverName = serverName;
    }

    public String getServerName(){
        return serverName;
    }
}
