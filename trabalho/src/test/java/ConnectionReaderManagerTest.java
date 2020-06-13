
import middleware.dbutils.ConnectionReaderManager;
import org.junit.Test;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConnectionReaderManagerTest {

    public class ConnectionReaderManagerImpl extends ConnectionReaderManager<Integer>{
        int num;

        public ConnectionReaderManagerImpl(int maxConnectionNumber){
            super(maxConnectionNumber);
            this.num = 1;
        }

        @Override
        public Integer produceConnection() {
            return num;
        }
    }


    @Test
    public void test() throws InterruptedException {
        ConnectionReaderManager<Integer> crm = new ConnectionReaderManagerImpl(100);
        ExecutorService taskExecutor = Executors.newFixedThreadPool(8);
        ArrayList<Integer> sums = new ArrayList<>(100);
        for(int i = 0; i < 100; i++){
            int finalI = i;
            crm.assignRequest().thenAcceptAsync((x) -> {
                sums.add(x);
                crm.releaseConnection(x);
            }, taskExecutor);
        }

        Thread.sleep(5000);
        System.out.println(sums);
        int res = sums.stream().reduce(0, Integer::sum);
        System.out.println(res);
    }
}
