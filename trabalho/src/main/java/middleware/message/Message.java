package middleware.message;

import java.util.UUID;

public class Message {
    private String id;

    public Message(){ this.id = UUID.randomUUID().toString(); }

    public String getId() {
        return id;
    }
}
