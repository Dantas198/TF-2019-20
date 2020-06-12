package middleware.message.replication;

import middleware.certifier.OperationalSets;
import middleware.message.Message;

import java.util.ArrayList;
import java.util.HashMap;

public class DBReplicationMessage extends Message {
    private String script;
    private ArrayList<String> logs;
    private Long lowWaterMark;
    private Long timeStamp;
    private HashMap<String, HashMap<Long, OperationalSets>> writeSets;

    public DBReplicationMessage(String script, ArrayList<String> logs, long lowWaterMark,
                                long timeStamp, HashMap<String, HashMap<Long, OperationalSets>> writeSets){
        this.writeSets = writeSets;
        this.script = script;
        this.logs = logs;
        this.lowWaterMark = lowWaterMark;
        this.timeStamp = timeStamp;
    }

    public String getScript() {
        return script;
    }

    public ArrayList<String> getLogs() {
        return logs;
    }

    public Long getLowWaterMark() {
        return lowWaterMark;
    }

    public Long getTimeStamp() {
        return timeStamp;
    }

    public HashMap<String, HashMap<Long, OperationalSets>> getWriteSets() {
        return writeSets;
    }
}
