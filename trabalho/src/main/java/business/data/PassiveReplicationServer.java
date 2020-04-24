package business.data;

import java.io.Serializable;

public abstract class PassiveReplicationServer {

	/**
	 * 
	 * @param msg
	 */
	public abstract void handleMessage(Serializable msg);

	public void start() {
		// TODO - implement business.data.PassiveReplicationServer.start
		throw new UnsupportedOperationException();
	}

	public void stop() {
		// TODO - implement business.data.PassiveReplicationServer.stop
		throw new UnsupportedOperationException();
	}

}