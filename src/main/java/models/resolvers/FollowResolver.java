package models.resolvers;

import com.google.gson.Gson;
import drivers.DatabaseDriver;
import drivers.MainDriver;
import models.network.NetClient;
import models.packets.AnswerFollow;
import models.packets.NotifyNewFollow;
import models.packets.RequestFollow;
import models.packets.base.AnswerStatus;
import models.packets.base.BaseNotify;
import models.resolvers.base.BaseResolver;
import org.neo4j.graphdb.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class FollowResolver extends BaseResolver {

    @Override
    public void resolvePacket(NetClient netClient, Object packet) {

        RequestFollow requestFollow = (RequestFollow) packet;

        Transaction tx = MainDriver.getInstance().getDatabaseDriver().getGraphDB().beginTx();
        Lock lock = null;

        try {

            if (netClient.isAuthenticated()) {

                Node myNode = MainDriver.getInstance().getDatabaseDriver().getGraphDB().getNodeById(netClient.getDbHumanNodeId());

                lock = tx.acquireWriteLock(myNode);

                if (myNode != null && myNode.getProperty("node-type").toString().equals("human")) {

                    boolean followExists = false;

                    for (Relationship relationship : myNode.getRelationships(DatabaseDriver.RelationTypes.FOLLOWS, Direction.OUTGOING)) {
                        if (relationship.getEndNodeId() == requestFollow.targetUserId) {
                            followExists = true;
                            break;
                        }
                    }

                    if (!followExists) {

                        Node targetHumanNode = MainDriver.getInstance().getDatabaseDriver().getGraphDB().getNodeById(requestFollow.targetUserId);

                        Lock targetLock = tx.acquireWriteLock(targetHumanNode);

                        if (targetHumanNode != null && targetHumanNode.getProperty("node-type").toString().equals("human")) {

                            myNode.createRelationshipTo(targetHumanNode, DatabaseDriver.RelationTypes.FOLLOWS);

                            int followersCount = (int) targetHumanNode.getProperty("followers-count");
                            followersCount++;
                            targetHumanNode.setProperty("followers-count", followersCount);

                            int followingCount = (int) myNode.getProperty("following-count");
                            followingCount++;
                            myNode.setProperty("following-count", followingCount);

                            AnswerFollow answerFollow = new AnswerFollow();
                            answerFollow.packetCode = requestFollow.packetCode;
                            answerFollow.answerStatus = AnswerStatus.OK;
                            netClient.getConnection().sendTCP(answerFollow);

                            NotifyNewFollow notifyNewFollow = new NotifyNewFollow();
                            notifyNewFollow.setFollowerId(myNode.getId());

                            if ((boolean)targetHumanNode.getProperty("is-online")) {
                                com.esotericsoftware.kryonet.Connection connection = MainDriver.getInstance()
                                        .getNetworkDriver().getConnectionById((int)targetHumanNode
                                                .getProperty("connection-id"));
                                if (connection != null) {
                                    connection.sendTCP(notifyNewFollow);
                                }
                                else {
                                    cacheUpdate(targetHumanNode.getId(), notifyNewFollow);
                                }
                            }
                            else {
                                cacheUpdate(targetHumanNode.getId(), notifyNewFollow);
                            }
                        }
                        else {

                            AnswerFollow answerFollow = new AnswerFollow();
                            answerFollow.packetCode = requestFollow.packetCode;
                            answerFollow.answerStatus = AnswerStatus.ERROR_200;
                            netClient.getConnection().sendTCP(answerFollow);
                        }

                        if (targetLock != null) {
                            targetLock.release();
                        }
                    }
                    else {

                        AnswerFollow answerFollow = new AnswerFollow();
                        answerFollow.packetCode = requestFollow.packetCode;
                        answerFollow.answerStatus = AnswerStatus.ERROR_201;
                        netClient.getConnection().sendTCP(answerFollow);
                    }
                }
                else {

                    AnswerFollow answerFollow = new AnswerFollow();
                    answerFollow.packetCode = requestFollow.packetCode;
                    answerFollow.answerStatus = AnswerStatus.ERROR_202;
                    netClient.getConnection().sendTCP(answerFollow);
                }
            }
            else {

                AnswerFollow answerFollow = new AnswerFollow();
                answerFollow.packetCode = requestFollow.packetCode;
                answerFollow.answerStatus = AnswerStatus.ERROR_203;
                netClient.getConnection().sendTCP(answerFollow);
            }

            if (lock != null) {
                lock.release();
            }

            tx.success();
        }
        catch (Exception ignored) {

            ignored.printStackTrace();

            if (lock != null) {
                lock.release();
            }

            tx.failure();
        }
        finally {

            tx.close();
        }
    }

    private void cacheUpdate(long humanId, BaseNotify baseNotify) throws SQLException{

        String query = "insert into 'Updates" + humanId + "' (Content, ObjType) values (?, ?);";
        PreparedStatement prpStmt = MainDriver.getInstance().getDatabaseDriver().getSqlDB().prepareStatement(query);
        prpStmt.setString(1, new Gson().toJson(baseNotify));
        prpStmt.setString(2, "NewFollow");
        prpStmt.executeUpdate();
        prpStmt.close();
        MainDriver.getInstance().getDatabaseDriver().getSqlDB().commit();
    }
}