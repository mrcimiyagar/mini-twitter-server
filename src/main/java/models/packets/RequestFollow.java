package models.packets;

import models.packets.base.BaseRequest;

public class RequestFollow extends BaseRequest {

    public long targetUserId;

    @Override
    public int getRequestCode() {
        return 3;
    }
}