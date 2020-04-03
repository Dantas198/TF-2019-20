package servers;

import spread.SpreadMessage;

public interface ElectionHandler {
    void elected(SpreadMessage message);
    void waitingForElection(SpreadMessage message);
}
