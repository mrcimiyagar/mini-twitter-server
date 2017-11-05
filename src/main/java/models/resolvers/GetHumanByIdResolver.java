package models.resolvers;

import drivers.DatabaseDriver;
import drivers.MainDriver;
import models.memory.Human;
import models.network.NetClient;
import models.packets.AnswerGetHumanById;
import models.packets.RequestGetHumanById;
import models.packets.RequestSearchUserTitle;
import models.packets.base.AnswerStatus;
import models.resolvers.base.BaseResolver;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import sun.rmi.runtime.Log;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class GetHumanByIdResolver extends BaseResolver {

    @Override
    public void resolvePacket(NetClient netClient, Object packet) {

        RequestGetHumanById requestGetHumanById = (RequestGetHumanById) packet;

        if (netClient.isAuthenticated()) {

            Transaction tx = MainDriver.getInstance().getDatabaseDriver().getGraphDB().beginTx();

            try {

                Node targetNode = MainDriver.getInstance().getDatabaseDriver().getGraphDB().getNodeById(requestGetHumanById.humanId);

                if (targetNode != null && targetNode.getProperty("node-type").toString().equals("human")) {

                    Human human = new Human();
                    human.setHumanId(requestGetHumanById.humanId);
                    human.setUserTitle(targetNode.getProperty("user-title").toString());

                    String query = "select * from UsersTitles where HumanId = ?";
                    PreparedStatement prpStmt = MainDriver.getInstance().getDatabaseDriver().getSqlDB().prepareStatement(query);
                    prpStmt.setLong(1, requestGetHumanById.humanId);
                    ResultSet rs = prpStmt.executeQuery();

                    String userBio = "";

                    if (rs.next()) {
                        userBio = rs.getString("UserBio");
                    }

                    rs.close();
                    prpStmt.close();

                    human.setUserBio(userBio);

                    human.setPostsCount((int)targetNode.getProperty("posts-count"));
                    human.setFollowersCount((int)targetNode.getProperty("followers-count"));
                    human.setFollowingCount((int)targetNode.getProperty("following-count"));
                    human.setProfilePrivate((boolean)targetNode.getProperty("is-private"));

                    int requestsCount = 0;

                    if (requestGetHumanById.humanId == netClient.getDbHumanNodeId()) {
                        for (Relationship relationship : targetNode.getRelationships(DatabaseDriver.RelationTypes.REQUESTED, Direction.INCOMING)) {
                            requestsCount++;
                        }
                    }

                    AnswerGetHumanById answerGetHumanById = new AnswerGetHumanById();
                    answerGetHumanById.answerStatus = AnswerStatus.OK;
                    answerGetHumanById.packetCode = requestGetHumanById.packetCode;
                    answerGetHumanById.human = human;
                    answerGetHumanById.requestCounts = requestsCount;
                    netClient.getConnection().sendTCP(answerGetHumanById);
                }
                else {

                    AnswerGetHumanById answerGetHumanById = new AnswerGetHumanById();
                    answerGetHumanById.answerStatus = AnswerStatus.ERROR_700;
                    answerGetHumanById.packetCode = requestGetHumanById.packetCode;
                    netClient.getConnection().sendTCP(answerGetHumanById);
                }

                tx.success();
            }
            catch (Exception ignored) {
                ignored.printStackTrace();
                tx.failure();
            }
            finally {
                tx.close();
            }
        }
        else {

            AnswerGetHumanById answerGetHumanById = new AnswerGetHumanById();
            answerGetHumanById.answerStatus = AnswerStatus.ERROR_701;
            answerGetHumanById.packetCode = requestGetHumanById.packetCode;
            netClient.getConnection().sendTCP(answerGetHumanById);
        }
    }
}