package models.resolvers;

import drivers.DatabaseDriver;
import drivers.MainDriver;
import models.memory.Human;
import models.network.NetClient;
import models.packets.AnswerGetFollowers;
import models.packets.AnswerGetFollowings;
import models.packets.RequestGetFollowings;
import models.packets.base.AnswerStatus;
import models.resolvers.base.BaseResolver;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import java.util.ArrayList;

public class GetFollowingsResolver extends BaseResolver {

    @Override
    public void resolvePacket(NetClient netClient, Object packet) {

        RequestGetFollowings requestGetFollowings = (RequestGetFollowings) packet;

        if (netClient.isAuthenticated()) {

            Transaction tx = MainDriver.getInstance().getDatabaseDriver().getGraphDB().beginTx();

            try {

                Node targetHumanNode = MainDriver.getInstance().getDatabaseDriver().getGraphDB().getNodeById(requestGetFollowings.humanId);

                if (targetHumanNode != null && targetHumanNode.getProperty("node-type").toString().equals("human")) {

                    Node myNode = MainDriver.getInstance().getDatabaseDriver().getGraphDB().getNodeById(netClient.getDbHumanNodeId());

                    boolean isPrivate = (boolean)targetHumanNode.getProperty("is-private");

                    boolean accessPermitted = false;

                    if (requestGetFollowings.humanId == netClient.getDbHumanNodeId()) {
                        accessPermitted = true;
                    }
                    else if (!isPrivate) {
                        accessPermitted = true;
                    }
                    else {
                        for (Relationship relationship : myNode.getRelationships(DatabaseDriver.RelationTypes.FOLLOWED, Direction.OUTGOING)) {
                            if (relationship.getEndNodeId() == requestGetFollowings.humanId) {
                                accessPermitted = true;
                                break;
                            }
                        }
                    }

                    if (accessPermitted) {

                        ArrayList<Human> result = new ArrayList<>();

                        for (Relationship relationship : targetHumanNode.getRelationships(DatabaseDriver.RelationTypes.FOLLOWED, Direction.OUTGOING)) {
                            Human human = new Human();
                            Node node = relationship.getEndNode();
                            human.setHumanId(node.getId());
                            human.setUserTitle(node.getProperty("user-title").toString());
                            result.add(human);
                        }

                        AnswerGetFollowings answerGetFollowings = new AnswerGetFollowings();
                        answerGetFollowings.answerStatus = AnswerStatus.OK;
                        answerGetFollowings.packetCode = requestGetFollowings.packetCode;
                        answerGetFollowings.humans = result;
                        netClient.getConnection().sendTCP(answerGetFollowings);
                    }
                    else {

                        AnswerGetFollowings answerGetFollowings = new AnswerGetFollowings();
                        answerGetFollowings.answerStatus = AnswerStatus.ERROR_902;
                        answerGetFollowings.packetCode = requestGetFollowings.packetCode;
                        netClient.getConnection().sendTCP(answerGetFollowings);
                    }
                }
                else {

                    AnswerGetFollowings answerGetFollowings = new AnswerGetFollowings();
                    answerGetFollowings.answerStatus = AnswerStatus.ERROR_900;
                    answerGetFollowings.packetCode = requestGetFollowings.packetCode;
                    netClient.getConnection().sendTCP(answerGetFollowings);
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

            AnswerGetFollowings answerGetFollowings = new AnswerGetFollowings();
            answerGetFollowings.answerStatus = AnswerStatus.ERROR_901;
            answerGetFollowings.packetCode = requestGetFollowings.packetCode;
            netClient.getConnection().sendTCP(answerGetFollowings);
        }
    }
}