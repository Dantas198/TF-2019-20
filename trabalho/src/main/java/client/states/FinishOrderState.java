package client.states;

import client.message.bodies.FinishOrderBody;

import java.io.Serializable;
import java.util.HashMap;

//TODO alterar o tipo consoante a lógica
/**
 * State relevant to make a finished order that has been certified to persist.
 */
public class FinishOrderState implements Serializable {
    private FinishOrderBody order;
    private HashMap<String, Integer> stockAlterations;

    public FinishOrderState(FinishOrderBody order, HashMap<String, Integer> stockAlterations) {
        this.order = order;
        this.stockAlterations = stockAlterations;
    }
}
