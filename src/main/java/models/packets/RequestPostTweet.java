package models.packets;

import models.packets.base.BaseRequest;

public class RequestPostTweet extends BaseRequest {

    public long pageId;
    public int parentId;
    public String tweetContent;

    @Override
    public int getRequestCode() {
        return 5;
    }
}