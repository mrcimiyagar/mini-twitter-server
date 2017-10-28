package models.packets;

import models.packets.base.BaseRequest;

public class RequestLogin extends BaseRequest {

    public long userId;
    public String passKey;

    @Override
    public int getRequestCode() {
        return 2;
    }
}