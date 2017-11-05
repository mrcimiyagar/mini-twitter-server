package models.resolvers;

import drivers.DatabaseDriver;
import drivers.MainDriver;
import models.network.NetClient;
import models.packets.AnswerLikeTweet;
import models.packets.AnswerUnlikeTweet;
import models.packets.RequestUnlikeTweet;
import models.packets.base.AnswerStatus;
import models.resolvers.base.BaseResolver;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UnlikeTweetResolver extends BaseResolver {

    @Override
    public void resolvePacket(NetClient netClient, Object packet) {

        RequestUnlikeTweet requestUnlikeTweet = (RequestUnlikeTweet) packet;

        if (netClient.isAuthenticated()) {

            Transaction tx = MainDriver.getInstance().getDatabaseDriver().getGraphDB().beginTx();

            try {

                Node targetHumanNode = MainDriver.getInstance().getDatabaseDriver().getGraphDB().getNodeById(requestUnlikeTweet.pageId);

                if (targetHumanNode != null && targetHumanNode.getProperty("node-type").toString().equals("human")) {

                    Node tweetNode = MainDriver.getInstance().getDatabaseDriver().getGraphDB().getNodeById(requestUnlikeTweet.tweetNodeId);

                    if (tweetNode != null && tweetNode.getProperty("node-type").toString().equals("tweet")) {

                        Relationship targetRs = null;

                        for (Relationship relationship : tweetNode.getRelationships(DatabaseDriver.RelationTypes.LIKED, Direction.INCOMING)) {
                            if (relationship.getStartNode().getId() == netClient.getDbHumanNodeId()) {
                                targetRs = relationship;
                                break;
                            }
                        }

                        if (targetRs != null) {

                            int tweetId = (int) tweetNode.getProperty("tweet-id");

                            targetRs.delete();

                            synchronized (MainDriver.getInstance().getDatabaseDriver().getSqlDB()) {

                                String queryGetBefore = "select * from 'Tweets" + requestUnlikeTweet.pageId + "' where TweetId = ?";
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

                                String query = "update 'Tweets" + requestUnlikeTweet.pageId + "' set LikesCount = LikesCount - 1 where TweetId = ?";
                                PreparedStatement prpStmtEdit = MainDriver.getInstance().getDatabaseDriver().getSqlDB().prepareStatement(query);
                                prpStmtEdit.setInt(1, tweetId);
                                prpStmtEdit.executeUpdate();
                                prpStmtEdit.close();

                                MainDriver.getInstance().getDatabaseDriver().getSqlDB().commit();

                                if (parentId == -1) {
                                    MainDriver.getInstance().getDatabaseDriver().updateTweetRank(requestUnlikeTweet.tweetNodeId
                                            , oldLikesCount, oldLikesCount - 1);
                                }
                            }

                            AnswerUnlikeTweet answerLikeTweet = new AnswerUnlikeTweet();
                            answerLikeTweet.answerStatus = AnswerStatus.OK;
                            answerLikeTweet.packetCode = requestUnlikeTweet.packetCode;
                            netClient.getConnection().sendTCP(answerLikeTweet);
                        }
                        else {

                            AnswerUnlikeTweet answerLikeTweet = new AnswerUnlikeTweet();
                            answerLikeTweet.answerStatus = AnswerStatus.ERROR_1200;
                            answerLikeTweet.packetCode = requestUnlikeTweet.packetCode;
                            netClient.getConnection().sendTCP(answerLikeTweet);
                        }
                    }
                    else {

                        AnswerUnlikeTweet answerLikeTweet = new AnswerUnlikeTweet();
                        answerLikeTweet.answerStatus = AnswerStatus.ERROR_1201;
                        answerLikeTweet.packetCode = requestUnlikeTweet.packetCode;
                        netClient.getConnection().sendTCP(answerLikeTweet);
                    }
                }
                else {

                    AnswerUnlikeTweet answerLikeTweet = new AnswerUnlikeTweet();
                    answerLikeTweet.answerStatus = AnswerStatus.ERROR_1202;
                    answerLikeTweet.packetCode = requestUnlikeTweet.packetCode;
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

            AnswerUnlikeTweet answerLikeTweet = new AnswerUnlikeTweet();
            answerLikeTweet.answerStatus = AnswerStatus.ERROR_1203;
            answerLikeTweet.packetCode = requestUnlikeTweet.packetCode;
            netClient.getConnection().sendTCP(answerLikeTweet);
        }
    }
}