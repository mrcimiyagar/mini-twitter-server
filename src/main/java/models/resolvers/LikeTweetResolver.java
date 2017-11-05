package models.resolvers;

import drivers.DatabaseDriver;
import drivers.MainDriver;
import models.network.NetClient;
import models.packets.AnswerLikeTweet;
import models.packets.RequestLikeTweet;
import models.packets.base.AnswerStatus;
import models.resolvers.base.BaseResolver;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LikeTweetResolver extends BaseResolver {

    @Override
    public void resolvePacket(NetClient netClient, Object packet) {

        RequestLikeTweet requestLikeTweet = (RequestLikeTweet) packet;

        if (netClient.isAuthenticated()) {

            Transaction tx = MainDriver.getInstance().getDatabaseDriver().getGraphDB().beginTx();

            try {

                Node targetHumanNode = MainDriver.getInstance().getDatabaseDriver().getGraphDB().getNodeById(requestLikeTweet.pageId);

                if (targetHumanNode != null && targetHumanNode.getProperty("node-type").toString().equals("human")) {

                    Node tweetNode = MainDriver.getInstance().getDatabaseDriver().getGraphDB().getNodeById(requestLikeTweet.tweetNodeId);

                    if (tweetNode != null && tweetNode.getProperty("node-type").toString().equals("tweet")) {

                        boolean likeExists = false;

                        for (Relationship relationship : tweetNode.getRelationships(DatabaseDriver.RelationTypes.LIKED, Direction.INCOMING)) {
                            if (relationship.getStartNode().getId() == netClient.getDbHumanNodeId()) {
                                likeExists = true;
                                break;
                            }
                        }

                        if (!likeExists) {

                            int tweetId = (int) tweetNode.getProperty("tweet-id");

                            Node myNode = MainDriver.getInstance().getDatabaseDriver().getGraphDB().getNodeById(netClient.getDbHumanNodeId());

                            myNode.createRelationshipTo(tweetNode, DatabaseDriver.RelationTypes.LIKED);

                            synchronized (MainDriver.getInstance().getDatabaseDriver().getSqlDB()) {

                                String queryGetBefore = "select * from 'Tweets" + requestLikeTweet.pageId + "' where TweetId = ?";
                                PreparedStatement prpStmtGetBefore = MainDriver.getInstance().getDatabaseDriver().getSqlDB().prepareStatement(queryGetBefore);
                                prpStmtGetBefore.setInt(1, tweetId);
                                ResultSet rsGetBefore = prpStmtGetBefore.executeQuery();

                                long oldLikesCount = 0;
                                int parentId = 0;

                                if (rsGetBefore.next()) {
                                    oldLikesCount = rsGetBefore.getLong("LikesCount");
                                    parentId = rsGetBefore.getInt("ParentId");
                                }

                                rsGetBefore.close();
                                prpStmtGetBefore.close();

                                String queryEdit = "update 'Tweets" + requestLikeTweet.pageId + "' set LikesCount = LikesCount + 1 where TweetId = ?";
                                PreparedStatement prpStmtEdit = MainDriver.getInstance().getDatabaseDriver().getSqlDB().prepareStatement(queryEdit);
                                prpStmtEdit.setInt(1, tweetId);
                                prpStmtEdit.executeUpdate();
                                prpStmtEdit.close();

                                MainDriver.getInstance().getDatabaseDriver().getSqlDB().commit();

                                if (parentId == -1) {
                                    MainDriver.getInstance().getDatabaseDriver().updateTweetRank(requestLikeTweet.tweetNodeId
                                            , oldLikesCount, oldLikesCount + 1);
                                }
                            }

                            AnswerLikeTweet answerLikeTweet = new AnswerLikeTweet();
                            answerLikeTweet.answerStatus = AnswerStatus.OK;
                            answerLikeTweet.packetCode = requestLikeTweet.packetCode;
                            netClient.getConnection().sendTCP(answerLikeTweet);
                        }
                        else {

                            AnswerLikeTweet answerLikeTweet = new AnswerLikeTweet();
                            answerLikeTweet.answerStatus = AnswerStatus.ERROR_1100;
                            answerLikeTweet.packetCode = requestLikeTweet.packetCode;
                            netClient.getConnection().sendTCP(answerLikeTweet);
                        }
                    }
                    else {

                        AnswerLikeTweet answerLikeTweet = new AnswerLikeTweet();
                        answerLikeTweet.answerStatus = AnswerStatus.ERROR_1101;
                        answerLikeTweet.packetCode = requestLikeTweet.packetCode;
                        netClient.getConnection().sendTCP(answerLikeTweet);
                    }
                }
                else {

                    AnswerLikeTweet answerLikeTweet = new AnswerLikeTweet();
                    answerLikeTweet.answerStatus = AnswerStatus.ERROR_1102;
                    answerLikeTweet.packetCode = requestLikeTweet.packetCode;
                    netClient.getConnection().sendTCP(answerLikeTweet);
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

            AnswerLikeTweet answerLikeTweet = new AnswerLikeTweet();
            answerLikeTweet.answerStatus = AnswerStatus.ERROR_1103;
            answerLikeTweet.packetCode = requestLikeTweet.packetCode;
            netClient.getConnection().sendTCP(answerLikeTweet);
        }
    }
}