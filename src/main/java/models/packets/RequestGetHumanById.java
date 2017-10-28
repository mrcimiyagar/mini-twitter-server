package models.packets;

import models.packets.base.BaseRequest;

public class RequestGetHumanById extends BaseRequest {

    public long humanId;

    @Override
    public int getRequestCode() {
        return 8;
    }
}