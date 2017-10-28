package models.resolvers;

import com.google.gson.Gson;
import drivers.DatabaseDriver;
import drivers.MainDriver;
import models.network.NetClient;
import models.packets.AnswerUnFollow;
import models.packets.NotifyNewFollow;
import models.packets.NotifyUnFollow;
import models.packets.RequestUnFollow;
import models.packets.base.AnswerStatus;
import models.packets.base.BaseNotify;
import models.resolvers.base.BaseResolver;
import org.neo4j.graphdb.*;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class UnFollowResolver extends BaseResolver {

    @Override
    public void resolvePacket(NetClient netClient, Object packet) {

        RequestUnFollow requestUnFollow = (RequestUnFollow) packet;

        if (netClient.isAuthenticated()) {

            Transaction tx = MainDriver.getInstance().getDatabaseDriver().getGraphDB().beginTx();

            Node myNode = MainDriver.getInstance().getDatabaseDriver().getGraphDB().getNodeById(netClient.getDbHumanNodeId());

            Lock lock = tx.acquireWriteLock(myNode);

            try {

                if (myNode != null && myNode.getProperty("node-type").toString().equals("human")) {

                    Relationship targetRelationship = null;

                    for (Relationship relationship : myNode.getRelationships(DatabaseDriver.RelationTypes.FOLLOWS, Direction.OUTGOING)) {
                        if (relationship.getEndNodeId() == requestUnFollow.targetUserId) {
                            targetRelationship = relationship;
                            break;
                        }
                    }

                    if (targetRelationship != null) {

                        Node targetHumanNode = targetRelationship.getEndNode();

                        Lock targetLock = tx.acquireWriteLock(targetHumanNode);

                        targetRelationship.delete();

                        int followersCount = (int) targetHumanNode.getProperty("followers-count");
                        followersCount--;
                        targetHumanNode.setProperty("followers-count", followersCount);

                        int followingCount = (int) myNode.getProperty("following-count");
                        followingCount--;
                        myNode.setProperty("following-count", followingCount);

                        AnswerUnFollow answerUnFollow = new AnswerUnFollow();
                        answerUnFollow.packetCode = requestUnFollow.packetCode;
                        answerUnFollow.answerStatus = AnswerStatus.OK;
                        netClient.getConnection().sendTCP(answerUnFollow);

                        if (targetLock != null) {
                            targetLock.release();
                        }

                        NotifyUnFollow notifyUnFollow = new NotifyUnFollow();
                        notifyUnFollow.setUnFollowerId(myNode.getId());

                        if ((boolean)targetHumanNode.getProperty("is-online")) {
                            com.esotericsoftware.kryonet.Connection connection = MainDriver.getInstance()
                                    .getNetworkDriver().getConnectionById((int)targetHumanNode
                                            .getProperty("connection-id"));
                            if (connection != null) {
                                connection.sendTCP(notifyUnFollow);
                            }
                            else {
                                cacheUpdate(targetHumanNode.getId(), notifyUnFollow);
                            }
                        }
                        else {
                            cacheUpdate(targetHumanNode.getId(), notifyUnFollow);
                        }

                    } else {

                        AnswerUnFollow answerUnFollow = new AnswerUnFollow();
                        answerUnFollow.packetCode = requestUnFollow.packetCode;
                        answerUnFollow.answerStatus = AnswerStatus.ERROR_300;
                        netClient.getConnection().sendTCP(answerUnFollow);
                    }
                } else {

                    AnswerUnFollow answerUnFollow = new AnswerUnFollow();
                    answerUnFollow.packetCode = requestUnFollow.packetCode;
                    answerUnFollow.answerStatus = AnswerStatus.ERROR_301;
                    netClient.getConnection().sendTCP(answerUnFollow);
                }

                lock.release();
                tx.success();

            } catch (Exception ignored) {
                ignored.printStackTrace();

                if (lock != null) {
                    lock.release();
                }

                tx.failure();

            } finally {
                tx.close();
            }
        }
        else {
            AnswerUnFollow answerUnFollow = new AnswerUnFollow();
            answerUnFollow.packetCode = requestUnFollow.packetCode;
            answerUnFollow.answerStatus = AnswerStatus.ERROR_302;
            netClient.getConnection().sendTCP(answerUnFollow);
        }
    }

    private void cacheUpdate(long humanId, BaseNotify baseNotify) throws SQLException {

        String query = "insert into 'Updates" + humanId + "' (Content, ObjType) values (?, ?);";
        PreparedStatement prpStmt = MainDriver.getInstance().getDatabaseDriver().getSqlDB().prepareStatement(query);
        prpStmt.setString(1, new Gson().toJson(baseNotify));
        prpStmt.setString(2, "UnFollow");
        prpStmt.executeUpdate();
        prpStmt.close();
        MainDriver.getInstance().getDatabaseDriver().getSqlDB().commit();
    }
}