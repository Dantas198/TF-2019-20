package client.message;

import middleware.message.ContentMessage;

public class GetHistoryMessage extends ContentMessage<String> {
    public GetHistoryMessage(String costumer){
        super(costumer);
    }
}
