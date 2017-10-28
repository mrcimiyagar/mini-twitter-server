package models.packets;

import models.memory.Tweet;
import models.packets.base.BaseAnswer;

import java.util.ArrayList;

public class AnswerGetTweets extends BaseAnswer {

    public ArrayList<Tweet> tweets;
}