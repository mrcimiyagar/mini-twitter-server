package models.packets;

import models.packets.base.BaseRequest;

public class RequestIgnoreFollowRequest extends BaseRequest {

    public long targetRequestId;

    @Override
    public int getRequestCode() {
        return 17;
    }
}