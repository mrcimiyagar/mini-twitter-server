package models.resolvers;

import drivers.DatabaseDriver;
import drivers.MainDriver;
import models.memory.Human;
import models.network.NetClient;
import models.packets.AnswerGetFollowers;
import models.packets.RequestGetFollowers;
import models.packets.base.AnswerStatus;
import models.resolvers.base.BaseResolver;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import java.util.ArrayList;

public class GetFollowersResolver extends BaseResolver {

    @Override
    public void resolvePacket(NetClient netClient, Object packet) {

        RequestGetFollowers requestGetFollowers = (RequestGetFollowers) packet;

        if (netClient.isAuthenticated()) {

            Transaction tx = MainDriver.getInstance().getDatabaseDriver().getGraphDB().beginTx();

            try {

                Node targetHumanNode = MainDriver.getInstance().getDatabaseDriver().getGraphDB().getNodeById(requestGetFollowers.humanId);

                if (targetHumanNode != null && targetHumanNode.getProperty("node-type").toString().equals("human")) {

                    ArrayList<Human> result = new ArrayList<>();

                    for (Relationship relationship : targetHumanNode.getRelationships(DatabaseDriver.RelationTypes.FOLLOWS, Direction.INCOMING)) {
                        Human human = new Human();
                        Node node = relationship.getStartNode();
                        human.setHumanId(node.getId());
                        human.setUserTitle(node.getProperty("user-title").toString());
                        result.add(human);
                    }

                    AnswerGetFollowers answerGetFollowers = new AnswerGetFollowers();
                    answerGetFollowers.answerStatus = AnswerStatus.OK;
                    answerGetFollowers.packetCode = requestGetFollowers.packetCode;
                    answerGetFollowers.humans = result;
                    netClient.getConnection().sendTCP(answerGetFollowers);
                }
                else {

                    AnswerGetFollowers answerGetFollowers = new AnswerGetFollowers();
                    answerGetFollowers.answerStatus = AnswerStatus.ERROR_800;
                    answerGetFollowers.packetCode = requestGetFollowers.packetCode;
                    netClient.getConnection().sendTCP(answerGetFollowers);
                }
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

            AnswerGetFollowers answerGetFollowers = new AnswerGetFollowers();
            answerGetFollowers.answerStatus = AnswerStatus.ERROR_801;
            answerGetFollowers.packetCode = requestGetFollowers.packetCode;
            netClient.getConnection().sendTCP(answerGetFollowers);
        }
    }
}