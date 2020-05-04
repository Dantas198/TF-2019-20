package middleware.message;

import java.io.Serializable;
import java.util.UUID;

public class Message implements Serializable {
    private String id;

    public Message(){ this.id = UUID.randomUUID().toString(); }

    public String getId() {
        return id;
    }
}
