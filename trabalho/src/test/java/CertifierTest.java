import middleware.Certifier.BitWriteSet;
import middleware.Certifier.Certifier;
import org.junit.Test;

public class CertifierTest {
/*
    private void checkConflict(Certifier c, BitWriteSet ws, long ts){
        if (!c.hasConflict(ws, ts)) {
            c.commit(ws);
            System.out.println("Commited");
        }
        else
            System.out.println("Had conflict on ts: " + ts);
    }

    @Test
    public void test(){
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
    */
}
