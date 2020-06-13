import middleware.certifier.BitOperationSet;
import middleware.certifier.Certifier;
import middleware.certifier.OperationalSets;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CertifierTest {
    Certifier c;

    private boolean checkConflict(Map<String, OperationalSets> ws, long ts){
        if (c.isWritable(ws, ts)) {
            c.commit(ws);
            c.shutDownLocalStartedTransaction(ws.keySet(), ts);
            System.out.println("Commited");
            return false;
        }
        else {
            System.out.println("Had conflict on ts: " + ts);
            return true;
        }
    }


    private long startTransaction(Set<String> tables){
        long t = c.getTimestamp();
        c.transactionStarted(tables, t);
        return t;
    }

    @Test
    public void test() {

        this.c = new Certifier();
        int running = 0;
        BitOperationSet write1 = new BitOperationSet();
        write1.add("marco");
        BitOperationSet read1 = new BitOperationSet();
        write1.add("marco");

        OperationalSets os1 = new OperationalSets(write1, read1);

        BitOperationSet write11 = new BitOperationSet();
        write11.add("bananas");

        OperationalSets os11 = new OperationalSets(write11, null);

        BitOperationSet write2 = new BitOperationSet();
        write2.add("cesar");
        write2.add("daniel");

        OperationalSets os2 = new OperationalSets(write2, null);

        BitOperationSet write3 = new BitOperationSet();
        write3.add("cesar");
        write3.add("carlos");

        OperationalSets os3 = new OperationalSets(write3, null);

        HashMap<String, OperationalSets> ws1 = new HashMap<>();
        ws1.put("customer", os1);
        ws1.put("products", os11);

        HashMap<String, OperationalSets> ws2 = new HashMap<>();
        ws2.put("customer", os2);

        HashMap<String, OperationalSets> ws3 = new HashMap<>();
        ws3.put("customer", os3);

        long t1 = startTransaction(ws1.keySet());
        long t11 = startTransaction(ws2.keySet());
        running += 2;

        assertTrue("Shouldn't conflict 1", !checkConflict(ws1, t1));
        running--;

        long t2 = startTransaction(ws3.keySet());
        assertTrue("Shouldn't conflict 2", !checkConflict(ws2, t11));
        running--;

        assertTrue("Should conflict", checkConflict(ws3, t2));

        //started and not commited
        long t3 = startTransaction(ws2.keySet());
        running++;

        long newLowWaterMark = c.getSafeToDeleteTimestamp();

        c.evictStoredWriteSets(newLowWaterMark);
        assertEquals(c.getLowWaterMark(), t1);

    }
}
