package models.packets;

import models.memory.Tweet;
import models.packets.base.BaseNotify;

public class NotifyNewTweet extends BaseNotify {

    private Tweet tweet;

    public Tweet getTweet() {
        return tweet;
    }

    public void setTweet(Tweet tweet) {
        this.tweet = tweet;
    }
}