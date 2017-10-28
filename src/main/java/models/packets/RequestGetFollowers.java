package models.packets;

import models.packets.base.BaseRequest;

public class RequestGetFollowers extends BaseRequest {

    public long humanId;

    @Override
    public int getRequestCode() {
        return 9;
    }
}