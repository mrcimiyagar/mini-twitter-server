package models.packets;

import models.packets.base.BaseNotify;

public class NotifyFollowRequestAccepted extends BaseNotify {

    private long humanId;

    public long getHumanId() {
        return humanId;
    }

    public void setHumanId(long humanId) {
        this.humanId = humanId;
    }
}