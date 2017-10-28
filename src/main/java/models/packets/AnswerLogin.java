package models.packets;

import models.packets.base.BaseAnswer;

public class AnswerLogin extends BaseAnswer {

    public String userTitle;
    public int postsCount;
    public int followersCount;
    public int followingCount;
}