package servers;

import bank.ContaSkel;
import io.atomix.cluster.messaging.ManagedMessagingService;
import io.atomix.cluster.messaging.MessagingConfig;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;
import io.atomix.utils.serializer.SerializerBuilder;
import messages.Acknowledgment;
import messages.ReplyMessage;
import messages.RequestMessage;
import messages.TransferStateMessage;

import spread.*;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class Server {
    private String privateName;
    private Address myAddress;
    private CompletableFuture<Void> running;
    private ContaSkel skel = new ContaSkel();

    //Util para secundários que transitam para primários, para que possam responder ao ultimo pedido de cada cliente
    //e para permitir detetar pedidos repetidos que podem acontecer devido ao timeout do lado do cliente
    private HashMap<Address, ReplyMessage> lastReplies;

    //modulo que trata da eleição do lider (quando em modo secundário)
    private ElectionManager electionManager;

    //modulo que trata de replicar estado (quando em modo primário)
    private ReplicationManager replicationManager;

    private SpreadConnection connection;
    private SpreadGroup group;
    private AdvancedMessageListener setSecondaryListener;
    private Serializer s;
    private ExecutorService e;
    private ManagedMessagingService mms;

    public Server(String privateName, int port) {
        this.privateName = privateName;
        this.myAddress = Address.from(port);
        this.lastReplies = new HashMap<>();
        this.running = new CompletableFuture<>();
        this.e = Executors.newFixedThreadPool(1);
        this.s = new SerializerBuilder()
                .addType(ReplyMessage.class)
                .addType(RequestMessage.class)
                .build();
        this.connection = new SpreadConnection();

        // O que fazer quando sou eleito
        Consumer<Integer> becomePrimary = (x) -> {
            connection.remove(this.setSecondaryListener);
            replicationManager = new ReplicationManager(x);
            connection.add(replicationManager.getListener());

            //Todos os servidores irão utilizar a mesma porta. Quando o primário falha a porta deverá ficar livre para
            // o próximo
            startPrimaryComponent();
            startClientsListener();

            //envia os ultimos pedidos dos clientes. Caso seja repetido o cliente simplesmente irá descartar, devido ao id
            lastReplies.forEach((a,b) -> mms.sendAsync(a,"reply",s.encode(b)));
        };

        //Começo como secundário, quando sou o primeiro a entrar sou logo eleito
        this.electionManager = new ElectionManager(connection, new ElectionHandlerImpl(becomePrimary));
    }

    //Função de arranque no modo secundário
    public void start() throws UnknownHostException, SpreadException, InterruptedException, ExecutionException {
        this.connection.connect(InetAddress.getByName("localhost"), 0,
                "server:" + privateName, false, true);
        this.group = new SpreadGroup();
        this.group.join(connection, "bank");

        // Adiciona o listener do secundário à conexão
        setSecondaryListener();
        connection.add(this.setSecondaryListener);
        this.running.get();
    }

    private void startPrimaryComponent(){
        this.mms = new NettyMessagingService(
                "server",
                myAddress,
                new MessagingConfig());
        this.mms.start();
        System.out.println("Primary component");
    }

    // Para responder a pedidos dos clientes
    private void startClientsListener(){
        mms.registerHandler("request", (a,b) -> {
            RequestMessage reqm = s.decode(b);
            ReplyMessage old = lastReplies.get(a);

            //Só irei mudar o estado e responder se ainda não respondi já a esta msg
            //Ocorre quando o cliente dá timeout, mas o servidor já tinha respondido
            //Se já respondi então o cliente não irá estar, em princípio, à espera da resposta à pergunta repetida
            if(old == null || !old.answerToSameRequest(reqm)){
                ReplyMessage repm = skel.resolve(reqm);
                lastReplies.put(a, repm);
                // se não é um pedido de saldo e a operação foi bem sucedida
                if(repm.type != 1 && repm.b){
                    try {

                        replicationManager.createEntry(repm.reqId, x -> mms.sendAsync(a,"reply",s.encode(repm)));
                        sendMsg(new TransferStateMessage(skel.getContaImpl().saldo(), a, repm), group);
                    } catch (SpreadException ex) {
                        ex.printStackTrace();
                    }
                }
                else
                    mms.sendAsync(a,"reply",s.encode(repm));
            }
        },e);
    }

    // listener para a receção das transferencias de estado e eleição de primário
    private void setSecondaryListener(){
        this.setSecondaryListener =  new AdvancedMessageListener() {
            @Override
            public void regularMessageReceived(SpreadMessage spreadMessage) {
                TransferStateMessage tsm;
                try {
                    tsm = (TransferStateMessage) spreadMessage.getObject();
                    System.out.println("Server " + privateName + "Received state tranfer");

                    //Guardo a última resposta do primário replicada, para quando eu me tornar primário a enviar
                    lastReplies.put(Address.from(tsm.client), tsm.lastReply);
                    skel.setSaldo(tsm.saldo);
                    sendMsg(new Acknowledgment(true, tsm.lastReply.reqId), spreadMessage.getSender());
                } catch (SpreadException ex) {
                    ex.printStackTrace();
                }
            }
            @Override
            public void membershipMessageReceived(SpreadMessage msg) {
                electionManager.receive(msg);
            }
        };
    }

    private <T extends Serializable> void sendMsg(T rm, SpreadGroup g) throws SpreadException {
        SpreadMessage m = new SpreadMessage();
        m.addGroup(g);
        m.setObject(rm);
        m.setReliable();
        connection.multicast(m);
    }

    public static void main(String[] args) throws InterruptedException, SpreadException, UnknownHostException, ExecutionException {
        new Server("bom",10000).start();
    }
}
