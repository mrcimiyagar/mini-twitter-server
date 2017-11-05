package models.packets;

import models.packets.base.BaseRequest;

public class RequestEditUserBio extends BaseRequest {

    public String newBio;

    @Override
    public int getRequestCode() {
        return 11;
    }
}