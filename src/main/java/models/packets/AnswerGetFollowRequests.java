package models.packets;

import models.memory.Human;
import models.packets.base.BaseAnswer;

import java.util.ArrayList;

public class AnswerGetFollowRequests extends BaseAnswer {

    public ArrayList<Human> humans;
}