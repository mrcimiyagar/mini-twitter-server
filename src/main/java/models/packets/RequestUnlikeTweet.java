package models.packets;

import models.packets.base.BaseRequest;

public class RequestUnlikeTweet extends BaseRequest {

    public long pageId;
    public long tweetNodeId;

    @Override
    public int getRequestCode() {
        return 13;
    }
}