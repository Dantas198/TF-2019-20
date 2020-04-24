package server;

import middleware.PassiveReplicationServer;

public class GandaGotaServer extends PassiveReplicationServer {

    public GandaGotaServer(int port, String privateName) {
        super(port, privateName);
    }

    @Override
    public void handleMessage(Object message) {

    }
}
