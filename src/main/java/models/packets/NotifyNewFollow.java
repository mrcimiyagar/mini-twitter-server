package models.packets;

import models.packets.base.BaseNotify;

public class NotifyNewFollow extends BaseNotify {

    private long followerId;

    public long getFollowerId() {
        return followerId;
    }

    public void setFollowerId(long followerId) {
        this.followerId = followerId;
    }
}