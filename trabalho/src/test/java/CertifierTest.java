import middleware.Certifier.BitWriteSet;
import middleware.Certifier.Certifier;
import middleware.Certifier.NoTableDefinedException;
import org.junit.Test;

import java.util.ArrayList;

public class CertifierTest {

    private void checkConflict(Certifier c, BitWriteSet ws, long ts) throws NoTableDefinedException {
        if (!c.hasConflict(ws, new ArrayList<>(), ts)) {
            c.commit(ws, new ArrayList<>());
            System.out.println("Commited");
        }
        else
            System.out.println("Had conflict on ts: " + ts);
    }

    @Test
    public void test() throws NoTableDefinedException {
        Certifier c = new Certifier();

        BitWriteSet ws1 = new BitWriteSet();
        ws1.add("marco".getBytes());

        BitWriteSet ws2 = new BitWriteSet();
        ws2.add("cesar".getBytes());
        ws2.add("daniel".getBytes());

        BitWriteSet ws3 = new BitWriteSet();
        ws3.add("cesar".getBytes());
        ws3.add("carlos".getBytes());

        long t1 = c.getTimestamp();
        System.out.println("WS 1");
        checkConflict(c, ws1, t1);

        long t2 = c.getTimestamp();
        System.out.println("WS 3");
        checkConflict(c, ws3, t1);

        System.out.println("WS 2");
        checkConflict(c, ws2, t2);
    }
}
