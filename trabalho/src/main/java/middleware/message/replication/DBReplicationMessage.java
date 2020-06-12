package middleware.message.replication;

import middleware.certifier.WriteSet;
import middleware.message.Message;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DBReplicationMessage extends Message {
    private String script;
    private ArrayList<String> logs;
    private Long lowWaterMark;
    private Long timeStamp;
    private ArrayList<WriteSet> writeSets;

    public DBReplicationMessage(String script, ArrayList<String> logs, long lowWaterMark,
                                long timeStamp, Collection<WriteSet> writeSets){
        this.writeSets = new ArrayList<>(writeSets);
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

    public ArrayList<WriteSet> getWriteSets() {
        return writeSets;
    }
}
