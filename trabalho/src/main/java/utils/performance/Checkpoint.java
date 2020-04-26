package timer;

public class Checkpoint {
    String info;
    long time;

    public Checkpoint(long currentTime){
        this.time = currentTime;
        this.info = "";
    }

    public Checkpoint(long currentTime, String info){
        this.time = currentTime;
        this.info = info;
    }
}
