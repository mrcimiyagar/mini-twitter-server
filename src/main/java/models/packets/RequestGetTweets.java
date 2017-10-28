package models.packets;

import models.packets.base.BaseRequest;

public class RequestGetTweets extends BaseRequest {

    public long targetUserId;
    public int targetParentId;

    @Override
    public int getRequestCode() {
        return 6;
    }
}