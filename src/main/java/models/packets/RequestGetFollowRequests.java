package models.packets;

import models.packets.base.BaseRequest;

public class RequestGetFollowRequests extends BaseRequest {

    @Override
    public int getRequestCode() {
        return 15;
    }
}