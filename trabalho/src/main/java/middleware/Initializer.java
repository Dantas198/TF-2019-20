package middleware;

import middleware.certifier.Certifier;
import middleware.certifier.OperationalSets;
import middleware.message.Message;
import middleware.message.replication.DBReplicationMessage;
import middleware.message.replication.GetTimeStampMessage;
import middleware.message.replication.SendTimeStampMessage;
import middleware.reader.Pair;
import spread.SpreadMessage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.*;
import java.util.function.Consumer;

public class Initializer {

    private Queue<SpreadMessage> messageQueue;
    private Boolean initializing;
    private ServerImpl<?> server;
    private ClusterReplicationService service;
    private String privateName;

    public Initializer(ServerImpl<?> server, ClusterReplicationService service, String privateName){
        this.server = server;
        this.messageQueue = new LinkedList<>();
        this.initializing = true;
        this.service = service;
        this.privateName = privateName;
    }

    public boolean isInitializing(SpreadMessage spreadMessage){
        try {
            if(initializing){
                Message received = (Message) spreadMessage.getObject();
                // apagar este if e o seu conteudo quando se remover o state
                if (received instanceof GetTimeStampMessage){
                    System.out.println(privateName + ": Received RegularMessage -> request for timestamp");
                    long timestamp = server.getTimestampReader().getTimestamp();
                    Message timeStampMessage = new SendTimeStampMessage(timestamp);
                    service.noAgreementFloodMessage(timeStampMessage, spreadMessage.getSender());
                } else if (received instanceof DBReplicationMessage){
                    System.out.println(privateName + ": Received RegularMessage -> db replication message");
                    handleDBReplicationMessage((DBReplicationMessage) received);
                    initializing = false;
                } else {
                    messageQueue.add(spreadMessage);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
         return initializing;
    }

    private void handleDBReplicationMessage(DBReplicationMessage received) throws Exception {
        String script = received.getScript();
        ArrayList<Pair<String, Long>> queries = received.getLogs();
        long lowWaterMark = received.getLowWaterMark();
        long timeStamp = received.getTimeStamp();
        if(script != null){
            Files.write(Path.of("db/" + server.getPrivateName() + ".script"), script.getBytes());
        }
        server.updateQueries(queries, service.getDbConnection());
        Certifier certifier = server.getCertifier();
        certifier.setLowWaterMark(lowWaterMark);
        certifier.setTimestamp(timeStamp);
    }


    public void initialized(){
        this.initializing = false;
    }

    public void reset() {
        this.initializing = false;
    }
}
