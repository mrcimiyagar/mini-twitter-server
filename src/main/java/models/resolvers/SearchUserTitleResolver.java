package models.resolvers;

import drivers.MainDriver;
import models.memory.Human;
import models.network.NetClient;
import models.packets.AnswerSearchUserTitle;
import models.packets.RequestSearchUserTitle;
import models.packets.base.AnswerStatus;
import models.resolvers.base.BaseResolver;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

public class SearchUserTitleResolver extends BaseResolver {

    @Override
    public void resolvePacket(NetClient netClient, Object packet) {

        RequestSearchUserTitle requestSearchUserTitle = (RequestSearchUserTitle) packet;

        if (netClient.isAuthenticated()) {

            try {

                String queryTitle = requestSearchUserTitle.query;

                String dbQuery = "select * from UsersTitles where UserTitle like ? ;";
                PreparedStatement prpStmt = MainDriver.getInstance().getDatabaseDriver().getSqlDB().prepareStatement(dbQuery);
                prpStmt.setString(1, "%" + queryTitle + "%");
                ResultSet resultSet = prpStmt.executeQuery();

                ArrayList<Human> humans = new ArrayList<>();

                while (resultSet.next()) {
                    Human human = new Human();
                    human.setHumanId(resultSet.getInt("HumanId"));
                    human.setUserTitle(resultSet.getString("UserTitle"));
                    human.setPostsCount(0);
                    human.setFollowersCount(0);
                    human.setFollowingCount(0);
                    human.setProfilePrivate(false);

                    humans.add(human);
                }

                AnswerSearchUserTitle answerSearchUserTitle = new AnswerSearchUserTitle();
                answerSearchUserTitle.packetCode = requestSearchUserTitle.packetCode;
                answerSearchUserTitle.answerStatus = AnswerStatus.OK;
                answerSearchUserTitle.humans = humans;
                netClient.getConnection().sendTCP(answerSearchUserTitle);
            }
            catch (Exception ignored) {
                ignored.printStackTrace();
            }
        }
        else {

            AnswerSearchUserTitle answerSearchUserTitle = new AnswerSearchUserTitle();
            answerSearchUserTitle.packetCode = requestSearchUserTitle.packetCode;
            answerSearchUserTitle.answerStatus = AnswerStatus.ERROR_600;
            netClient.getConnection().sendTCP(answerSearchUserTitle);
        }
    }
}