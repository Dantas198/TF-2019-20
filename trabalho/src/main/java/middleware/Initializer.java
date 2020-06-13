package middleware;

import middleware.certifier.Certifier;
import middleware.certifier.OperationalSets;
import middleware.message.LeaderProposal;
import middleware.message.Message;
import middleware.message.replication.DBReplicationMessage;
import middleware.message.replication.GetTimeStampMessage;
import middleware.message.replication.SendTimeStampMessage;
import middleware.reader.Pair;
import spread.AdvancedMessageListener;
import spread.SpreadGroup;
import spread.SpreadMessage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class Initializer {

    private Queue<SpreadMessage> messageQueue;
    private Boolean initializing;
    private ServerImpl server;
    private ClusterReplicationService service;
    private String privateName;
    private Long commitedTimestamp;
    private CompletableFuture<Void> electionTerminatedCallback;


    public Initializer(ServerImpl server, ClusterReplicationService service, String privateName){
        this.server = server;
        this.messageQueue = new LinkedList<>();
        this.initializing = true;
        this.service = service;
        this.privateName = privateName;
        try {
            this.commitedTimestamp = server.getTimestamp();
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.electionTerminatedCallback = new CompletableFuture<>();
    }

    public boolean isInitializing(SpreadMessage spreadMessage){
        try {
            if(initializing){
                Message received = (Message) spreadMessage.getObject();
                // apagar este if e o seu conteudo quando se remover o state
                if (received instanceof GetTimeStampMessage){
                    System.out.println(privateName + ": Received RegularMessage -> request for timestamp");

                    LeaderProposal incominglp = ((GetTimeStampMessage) received).getBody();
                    service.electionManager.handleLeaderProposal(incominglp, spreadMessage.getSender());

                    LeaderProposal mylp = new LeaderProposal(commitedTimestamp, service.getGroupsLeftForLeader());
                    Message timeStampMessage = new SendTimeStampMessage(mylp);
                    service.noAgreementFloodMessage(timeStampMessage, spreadMessage.getSender());

                } else if (received instanceof DBReplicationMessage){
                    System.out.println(privateName + ": Received RegularMessage -> db replication message");
                    handleDBReplicationMessage((DBReplicationMessage) received);
                    initializing = false;

                    //líder já foi eleito e foi quem lhe enviou a GetTimeStampMessage, daí ele já estar no electionManager

                    service.electionManager.elect();
                    for (SpreadMessage message:
                         messageQueue) {
                        AdvancedMessageListener listener = service.messageListener();
                        if(message.isMembership()) {
                            listener.membershipMessageReceived(message);
                        } else {
                            listener.regularMessageReceived(message);
                        }
                    }
                //Recebi uma resposta com proposta de líder, por isso a eleição ainda decorre
                } else if (received instanceof SendTimeStampMessage){
                    LeaderProposal lp = ((SendTimeStampMessage) received).getBody();
                    service.electionManager.handleLeaderProposal(lp, spreadMessage.getSender());
                }
                else {
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
        server.resetDBConnection();
    }


    public void initialized(){
        this.initializing = false;
    }

    public void reset() {
        this.initializing = false;
    }
}
