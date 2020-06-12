package middleware;

import middleware.certifier.Certifier;
import middleware.certifier.WriteSet;
import middleware.message.replication.DBReplicationMessage;
import middleware.message.replication.GetTimeStampMessage;
import middleware.message.Message;
import middleware.message.replication.SendTimeStampMessage;
import middleware.reader.TimestampReader;
import spread.SpreadMessage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.Consumer;

//TODO Servidores à escuta veem que entrou um servidor ou novo ou que tinha ido abaixo.
// Enviam o número de linhas que teem (Refletir sobre a melhor maneiro)
// Initializor espera por isso e compara com os seus logs
// Envia pedido do que falta e recebe o estado que falta

public class Initializer {

    private Queue<SpreadMessage> messageQueue;
    private Boolean initializing;
    private ServerImpl server;
    private ClusterReplicationService service;

    public Initializer(ServerImpl server, ClusterReplicationService service, Connection connection){
        this.server = server;
        this.messageQueue = new LinkedList<>();
        this.initializing = true;
        this.service = service;
    }

    public boolean isInitializing(SpreadMessage spreadMessage, Consumer<SpreadMessage> respondMessage){
        try {
            if(initializing){
                Message received = (Message) spreadMessage.getObject();
                // apagar este if e o seu conteudo quando se remover o state
                if (received instanceof GetTimeStampMessage){
                    System.out.println("Received request for my timestamp");
                    long timestamp = server.getTimestampReader().getTimestamp();
                    Message timeStampMessage = new SendTimeStampMessage(timestamp);
                    service.noAgreementFloodMessage(timeStampMessage, spreadMessage.getSender());
                } else if (received instanceof DBReplicationMessage){
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
        System.out.println("Received DBReplicationMessage");
        String script = received.getScript();
        ArrayList<String> queries = received.getLogs();
        long lowWaterMark = received.getLowWaterMark();
        long timeStamp = received.getTimeStamp();
        List<WriteSet> writeSets = received.getWriteSets();
        if(script != null){
            Files.write(Path.of("db/" + server.getPrivateName() + ".script"), script.getBytes());
        }
        server.updateQueries(queries, service.getDbConnection());
        Certifier certifier = server.getCertifier();
        certifier.setLowWaterMark(lowWaterMark);
        certifier.setTimestamp(timeStamp);
        //TODO Adicionar o writeSets ao certifier
    }

    public void initialized(){
        this.initializing = false;
    }
}
