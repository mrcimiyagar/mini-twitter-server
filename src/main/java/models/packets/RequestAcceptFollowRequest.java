package models.packets;

import models.packets.base.BaseRequest;

public class RequestAcceptFollowRequest extends BaseRequest {

    public long targetRequestId;

    @Override
    public int getRequestCode() {
        return 16;
    }
}