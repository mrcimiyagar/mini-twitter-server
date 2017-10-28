package models.packets;

import models.packets.base.BaseNotify;

public class NotifyUnFollow extends BaseNotify {

    private long unFollowerId;

    public long getUnFollowerId() {
        return unFollowerId;
    }

    public void setUnFollowerId(long unFollowerId) {
        this.unFollowerId = unFollowerId;
    }
}