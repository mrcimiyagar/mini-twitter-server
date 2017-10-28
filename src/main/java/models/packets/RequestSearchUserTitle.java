package models.packets;

import models.packets.base.BaseRequest;

public class RequestSearchUserTitle extends BaseRequest {

    public String query;

    @Override
    public int getRequestCode() {
        return 7;
    }
}