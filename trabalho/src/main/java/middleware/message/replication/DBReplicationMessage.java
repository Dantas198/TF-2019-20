package middleware.message.replication;

import middleware.certifier.OperationalSets;
import middleware.message.Message;
import middleware.reader.Pair;

import java.util.ArrayList;
import java.util.HashMap;

public class DBReplicationMessage extends Message {
    private String script;
    private ArrayList<Pair<String, Long>> logs;
    private Long lowWaterMark;
    private Long timeStamp;

    public DBReplicationMessage(String script, ArrayList<Pair<String, Long>> logs, long lowWaterMark,
                                long timeStamp, HashMap<String, HashMap<Long, OperationalSets>> writeSets){
        this.script = script;
        this.logs = logs;
        this.lowWaterMark = lowWaterMark;
        this.timeStamp = timeStamp;
    }

    public String getScript() {
        return script;
    }

    public ArrayList<Pair<String, Long>> getLogs() {
        return logs;
    }

    public Long getLowWaterMark() {
        return lowWaterMark;
    }

    public Long getTimeStamp() {
        return timeStamp;
    }
}
