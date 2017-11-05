package models.resolvers;

import drivers.DatabaseDriver;
import drivers.MainDriver;
import models.memory.Human;
import models.network.NetClient;
import models.packets.AnswerGetFollowRequests;
import models.packets.RequestGetFollowRequests;
import models.packets.base.AnswerStatus;
import models.resolvers.base.BaseResolver;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import java.util.ArrayList;

public class GetFollowRequestsResolver extends BaseResolver {

    @Override
    public void resolvePacket(NetClient netClient, Object packet) {

        RequestGetFollowRequests requestGetFollowRequests = (RequestGetFollowRequests) packet;

        if (netClient.isAuthenticated()) {

            Transaction tx = MainDriver.getInstance().getDatabaseDriver().getGraphDB().beginTx();

            try {

                Node myNode = MainDriver.getInstance().getDatabaseDriver().getGraphDB().getNodeById(netClient.getDbHumanNodeId());

                ArrayList<Human> humans = new ArrayList<>();

                for (Relationship relationship : myNode.getRelationships(DatabaseDriver.RelationTypes.REQUESTED, Direction.INCOMING)) {
                    Node humanNode = relationship.getStartNode();
                    Human human = new Human();
                    human.setHumanId(humanNode.getId());
                    human.setUserTitle(humanNode.getProperty("user-title").toString());
                    humans.add(human);
                }

                AnswerGetFollowRequests answerGetFollowRequests = new AnswerGetFollowRequests();
                answerGetFollowRequests.answerStatus = AnswerStatus.OK;
                answerGetFollowRequests.packetCode = requestGetFollowRequests.packetCode;
                answerGetFollowRequests.humans = humans;
                netClient.getConnection().sendTCP(answerGetFollowRequests);

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

            AnswerGetFollowRequests answerGetFollowRequests = new AnswerGetFollowRequests();
            answerGetFollowRequests.answerStatus = AnswerStatus.ERROR_1400;
            answerGetFollowRequests.packetCode = requestGetFollowRequests.packetCode;
            netClient.getConnection().sendTCP(answerGetFollowRequests);
        }
    }
}