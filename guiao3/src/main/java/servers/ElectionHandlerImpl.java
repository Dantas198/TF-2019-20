package servers;

import spread.SpreadMessage;

import java.util.function.Consumer;

public class ElectionHandlerImpl implements ElectionHandler {
    private Consumer<Void> job;


    public ElectionHandlerImpl(Consumer<Void> job){
        this.job = job;
    }

    @Override
    public void elected(SpreadMessage message) {
        System.out.println("I am the primary Server");
        job.accept(null);
    }

    @Override
    public void waitingForElection(SpreadMessage message) {
        System.out.println("Waiting for election");
    }
}
