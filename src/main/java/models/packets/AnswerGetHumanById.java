package models.packets;

import models.memory.Human;
import models.packets.base.BaseAnswer;

public class AnswerGetHumanById extends BaseAnswer {

    public Human human;
    public int requestCounts;
}