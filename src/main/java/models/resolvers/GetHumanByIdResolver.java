package models.resolvers;

import drivers.MainDriver;
import models.memory.Human;
import models.network.NetClient;
import models.packets.AnswerGetHumanById;
import models.packets.RequestGetHumanById;
import models.packets.RequestSearchUserTitle;
import models.packets.base.AnswerStatus;
import models.resolvers.base.BaseResolver;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import sun.rmi.runtime.Log;

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
                    human.setPostsCount((int)targetNode.getProperty("posts-count"));
                    human.setFollowersCount((int)targetNode.getProperty("followers-count"));
                    human.setFollowingCount((int)targetNode.getProperty("following-count"));

                    AnswerGetHumanById answerGetHumanById = new AnswerGetHumanById();
                    answerGetHumanById.answerStatus = AnswerStatus.OK;
                    answerGetHumanById.packetCode = requestGetHumanById.packetCode;
                    answerGetHumanById.human = human;
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