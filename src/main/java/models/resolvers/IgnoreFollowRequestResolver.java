package models.resolvers;

import com.google.gson.Gson;
import drivers.DatabaseDriver;
import drivers.MainDriver;
import models.network.NetClient;
import models.packets.*;
import models.packets.base.AnswerStatus;
import models.packets.base.BaseNotify;
import models.resolvers.base.BaseResolver;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class IgnoreFollowRequestResolver extends BaseResolver {

    @Override
    public void resolvePacket(NetClient netClient, Object packet) {

        RequestIgnoreFollowRequest requestAcceptFollowRequest = (RequestIgnoreFollowRequest) packet;

        if (netClient.isAuthenticated()) {

            Transaction tx = MainDriver.getInstance().getDatabaseDriver().getGraphDB().beginTx();

            try {

                Node myNode = MainDriver.getInstance().getDatabaseDriver().getGraphDB().getNodeById(netClient.getDbHumanNodeId());

                Relationship followRequestRelationship = null;

                for (Relationship relationship : myNode.getRelationships(DatabaseDriver.RelationTypes.REQUESTED, Direction.INCOMING)) {
                    if (relationship.getStartNodeId() == requestAcceptFollowRequest.targetRequestId) {
                        followRequestRelationship = relationship;
                        break;
                    }
                }

                if (followRequestRelationship != null) {

                    Node requesterNode = followRequestRelationship.getStartNode();

                    followRequestRelationship.delete();

                    AnswerIgnoreFollowRequest answerFollow = new AnswerIgnoreFollowRequest();
                    answerFollow.answerStatus = AnswerStatus.OK;
                    answerFollow.packetCode = requestAcceptFollowRequest.packetCode;
                    netClient.getConnection().sendTCP(answerFollow);

                    NotifyFollowRequestIgnored notifyNewFollow = new NotifyFollowRequestIgnored();
                    notifyNewFollow.setHumanId(myNode.getId());

                    if ((boolean) requesterNode.getProperty("is-online")) {
                        com.esotericsoftware.kryonet.Connection connection = MainDriver.getInstance()
                                .getNetworkDriver().getConnectionById((int) requesterNode
                                        .getProperty("connection-id"));
                        if (connection != null) {
                            connection.sendTCP(notifyNewFollow);
                        } else {
                            cacheUpdate(requesterNode.getId(), notifyNewFollow);
                        }
                    } else {
                        cacheUpdate(requesterNode.getId(), notifyNewFollow);
                    }
                }
                else {

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

            AnswerIgnoreFollowRequest answerFollow = new AnswerIgnoreFollowRequest();
            answerFollow.answerStatus = AnswerStatus.ERROR_1600;
            answerFollow.packetCode = requestAcceptFollowRequest.packetCode;
            netClient.getConnection().sendTCP(answerFollow);
        }
    }

    private void cacheUpdate(long humanId, BaseNotify baseNotify) throws SQLException {

        String query = "insert into 'Updates" + humanId + "' (Content, ObjType) values (?, ?);";
        PreparedStatement prpStmt = MainDriver.getInstance().getDatabaseDriver().getSqlDB().prepareStatement(query);
        prpStmt.setString(1, new Gson().toJson(baseNotify));
        prpStmt.setString(2, "NewFollow");
        prpStmt.executeUpdate();
        prpStmt.close();
        MainDriver.getInstance().getDatabaseDriver().getSqlDB().commit();
    }
}