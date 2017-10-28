package models.packets;

import models.packets.base.BaseRequest;

public class RequestRegister extends BaseRequest {

    public String userTitle;

    @Override
    public int getRequestCode() {
        return 1;
    }
}