package models.packets;

import models.packets.base.BaseRequest;

public class RequestUnFollow extends BaseRequest {

    public long targetUserId;

    @Override
    public int getRequestCode() {
        return 4;
    }
}