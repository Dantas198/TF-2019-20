package middleware;

import middleware.message.replication.SafeDeleteRequestMessage;
import spread.SpreadGroup;

import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SafeDeleteScheduler {

    private ScheduledThreadPoolExecutor executor;
    private SpreadGroup[] members;
    private List<SpreadGroup> sdRequested; // SafeDeleteRequest sent to these members


    private class SafeDeleteEvent implements Runnable {
        @Override
        public void run() {
            if (!imLeader) return;
            SafeDeleteRequestMessage msg = new SafeDeleteRequestMessage();
            try {
                evicting = true;
                noAgreementFloodMessage(msg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void scheduleSafeDeleteEvent() {
        long minutesUntilSafeDelete = 1000;
        executor.schedule(new ClusterReplicationService.SafeDeleteEvent(), minutesUntilSafeDelete, TimeUnit.MINUTES);
    }
}
