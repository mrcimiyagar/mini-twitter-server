package models.packets;

import models.packets.base.BaseRequest;

public class RequestGetFollowings extends BaseRequest {

    public long humanId;

    @Override
    public int getRequestCode() {
        return 10;
    }
}