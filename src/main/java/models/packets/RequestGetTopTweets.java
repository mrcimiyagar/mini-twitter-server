package models.packets;

import models.packets.base.BaseRequest;

public class RequestGetTopTweets extends BaseRequest {

    @Override
    public int getRequestCode() {
        return 14;
    }
}