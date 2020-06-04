import middleware.certifier.BitWriteSet;
import middleware.certifier.Certifier;
import middleware.certifier.NoTableDefinedException;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;

public class CertifierTest {


    private boolean checkConflict(Certifier c, Map<String, BitWriteSet> ws, long ts) throws NoTableDefinedException {
        if (!c.hasConflict(ws, ts)) {
            c.commit(ws);
            System.out.println("Commited");
            return true;
        }
        else
            System.out.println("Had conflict on ts: " + ts);
        return false;
    }

    @Test
    public void test() throws NoTableDefinedException {
        Certifier c = new Certifier();
        c.addTables(Arrays.asList("customer"));

        BitWriteSet bws1 = new BitWriteSet();
        bws1.add("marco".getBytes());

        BitWriteSet bws2 = new BitWriteSet();
        bws2.add("cesar".getBytes());
        bws2.add("daniel".getBytes());

        BitWriteSet bws3 = new BitWriteSet();
        bws3.add("cesar".getBytes());
        bws3.add("carlos".getBytes());

        long t1 = c.getTimestamp();
        System.out.println("WS 1");
        HashMap<String, BitWriteSet> ws1 = new HashMap<>();
        ws1.put("customer", bws1);
        assertTrue("Shouldn't conflict", checkConflict(c, ws1, t1));

        HashMap<String, BitWriteSet> ws3 = new HashMap<>();
        ws3.put("customer", bws3);
        long t2 = c.getTimestamp();
        System.out.println("WS 3");
        assertTrue("Shouldn't conflict", checkConflict(c, ws3, t1));

        HashMap<String, BitWriteSet> ws2 = new HashMap<>();
        ws2.put("customer", bws2);
        System.out.println("WS 2");
        assertTrue("Should conflict", !checkConflict(c, ws2, t2));
    }

}
