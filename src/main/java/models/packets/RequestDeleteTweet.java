package models.packets;

import models.packets.base.BaseRequest;

public class RequestDeleteTweet extends BaseRequest {

    public long tweetNodeId;

    @Override
    public int getRequestCode() {
        return 20;
    }
}