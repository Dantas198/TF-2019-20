package client.message;

import client.states.FinishOrderState;
import middleware.certifier.BitWriteSet;
import middleware.certifier.WriteSet;
import middleware.message.replication.CertifyWriteMessage;

import java.util.Map;

/**
 * Class that holds preprocessed information like the WriteSet of the operation.
 */
public class FinishOrderPreProcessedMessage<K extends WriteSet<?>> extends CertifyWriteMessage<K, FinishOrderState>{
    //TODO passar lógica de conversão de order para write set
    public FinishOrderPreProcessedMessage(Map<String, K> ws, FinishOrderState calculatedState){
        super(ws, calculatedState);
    }
}
