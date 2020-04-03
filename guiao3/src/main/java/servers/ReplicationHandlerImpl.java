package servers;

import java.util.function.Consumer;

public class ReplicationHandlerImpl implements ReplicationHandler{
    private Consumer<Void> job;

    public ReplicationHandlerImpl(Consumer<Void> job){
        this.job = job;
    }

    @Override
    public void answerRequest() {
        job.accept(null);
    }
}
