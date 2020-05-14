package client.message;

import client.states.FinishOrderState;
import middleware.Certifier.BitWriteSet;
import middleware.message.replication.CertifyWriteMessage;

/**
 * Class that holds preprocessed information like the WriteSet of the operation.
 */
public class FinishOrderPreProcessedMessage extends CertifyWriteMessage<FinishOrderState>{
    //TODO passar lógica de conversão de order para write set
    public FinishOrderPreProcessedMessage(BitWriteSet bws, FinishOrderState calculatedState){
        super(bws, calculatedState);
    }
}
