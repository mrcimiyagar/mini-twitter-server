package models.packets;

import models.packets.base.BaseRequest;

public class RequestGetFeed extends BaseRequest {

    @Override
    public int getRequestCode() {
        return 18;
    }
}