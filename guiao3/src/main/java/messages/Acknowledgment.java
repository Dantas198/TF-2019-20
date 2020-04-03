package messages;

import java.io.Serializable;

public class Acknowledgment implements Serializable {
    boolean b;

    public Acknowledgment(boolean b){
        this.b = b;
    }
}
