package messages;

import java.io.Serializable;

public class Acknowledgment implements Serializable {
    public boolean b;
    public int requestId;

    public Acknowledgment(boolean b, int id){
        this.b = b;
        this.requestId = id;
    }
}
