package models.packets;

import models.packets.base.BaseRequest;

public class RequestSwitchProfileMode extends BaseRequest {

    public boolean isPrivate;

    @Override
    public int getRequestCode() {
        return 19;
    }
}