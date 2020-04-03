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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class Server {
    private String privateName;
    private Address myAddress;

    private CompletableFuture<Void> reply;
    private CompletableFuture<Void> running;
    private ContaSkel skel = new ContaSkel();

    private ElectionManager electionManager;
    private ReplicationManager replicationManager;

    private SpreadConnection connection;
    private SpreadGroup group;
    private Serializer s;
    private ExecutorService e;
    private ManagedMessagingService mms;

    public Server(String privateName, int port) throws InterruptedException, ExecutionException, SpreadException, UnknownHostException {
        this.privateName = privateName;
        this.myAddress = Address.from(port);
        this.running = new CompletableFuture<>();
        this.e = Executors.newFixedThreadPool(1);
        this.s = new SerializerBuilder()
                .addType(ReplyMessage.class)
                .addType(RequestMessage.class)
                .build();
        this.connection = new SpreadConnection();

        // o que fazer quando sou eleito
        Consumer<Void> becomePrimary = (x) -> {
            connection.remove(getSecondaryListener());
            replicationManager = new ReplicationManager(new ReplicationHandlerImpl((y) -> reply.complete(null)));
            connection.add(replicationManager.getListener());


            //todos os servidores irão utilizar a mesma porta. Quando o primário falha a porta deverá ficar livre, mas
            //temos de verficar, caso contrário teremos de comunicar com o cliente para estabelecer a porta.
            //de qualquer das maneiras muito provávelmente o cliente vai dar uma exceção do atomix, pq por momentos não vai
            //haver quem leia deste lado o que ele envia.
            startPrimaryComponent();
            startClientsListener();
        };

        // começo como secundário. Se estiver correto o algoritmo se eu for o primeiro a entrar sou logo eleito
        this.electionManager = new ElectionManager(connection, new ElectionHandlerImpl(becomePrimary));
    }


    public void start() throws UnknownHostException, SpreadException, InterruptedException, ExecutionException {
        this.connection.connect(InetAddress.getByName("localhost"), 0,
                "server:" + privateName, false, true);
        this.group = new SpreadGroup();
        this.group.join(connection, "bank");
        // adiciona o listener do secundário à conexão
        connection.add(getSecondaryListener());

        //cuidado! imagino que o que corra o becomePrimary seja a thread associada ao connection.add() do listener que
        //o chamou, mas se não foi isto pode dar problemas
        this.running.get();
    }

    private void startPrimaryComponent(){
        this.mms = new NettyMessagingService(
                "server",
                myAddress,
                new MessagingConfig());
        this.mms.start();
    }

    // para responder a pedidos dos clientes
    private void startClientsListener(){
        mms.registerHandler("request", (a,b) -> {
            RequestMessage reqm = s.decode(b);
            ReplyMessage repm = skel.resolve(reqm);
            // se não é um pedido de saldo e a operação foi bem sucedida
            if(repm.type != 1 && repm.b){
                try {
                    this.reply = new CompletableFuture<>();
                    sendMsg(new TransferStateMessage(skel.getContaImpl().saldo()), group);
                    //tem de esperar para ser sequencial, visto que concorrência de pedidos envolve mais coisas
                    reply.thenAccept(x -> mms.sendAsync(a,"reply",s.encode(repm))).get();
                } catch (SpreadException | InterruptedException | ExecutionException ex) {
                    ex.printStackTrace();
                }
            }
            else
                mms.sendAsync(a,"reply",s.encode(repm));
        },e);
    }

    // listener para a receção das transferencias de estado e eleição de primário
    private AdvancedMessageListener getSecondaryListener(){
        return new AdvancedMessageListener() {
            @Override
            public void regularMessageReceived(SpreadMessage spreadMessage) {
                TransferStateMessage tsm = null;
                try {
                    tsm = (TransferStateMessage) spreadMessage.getObject();
                    System.out.println("Server " + privateName + "Received state tranfer");
                    skel.setSaldo(tsm.saldo);
                    sendMsg(new Acknowledgment(true), spreadMessage.getSender());
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
