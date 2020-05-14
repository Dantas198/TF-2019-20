package middleware.message;

import java.io.Serializable;
import java.util.UUID;

public class Message implements Serializable {
    private String id;

    public Message(){ this.id = UUID.randomUUID().toString(); }

    public String getId() {
        return id;
    }

    public Message from(Message m){
        this.id = m.getId();
        return this;
    }

    public void setId(String id) {
        this.id = id;
    }
}
