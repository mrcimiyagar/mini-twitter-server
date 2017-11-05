package models.resolvers;

import drivers.DatabaseDriver;
import drivers.MainDriver;
import models.network.NetClient;
import models.packets.AnswerDeleteTweet;
import models.packets.RequestDeleteTweet;
import models.packets.base.AnswerStatus;
import models.resolvers.base.BaseResolver;
import org.neo4j.graphdb.*;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DeleteTweetResolver extends BaseResolver {

    @Override
    public void resolvePacket(NetClient netClient, Object packet) {

        RequestDeleteTweet requestDeleteTweet = (RequestDeleteTweet) packet;

        if (netClient.isAuthenticated()) {

            Transaction tx = MainDriver.getInstance().getDatabaseDriver().getGraphDB().beginTx();

            try {

                Node tweetNode = MainDriver.getInstance().getDatabaseDriver().getGraphDB().getNodeById(requestDeleteTweet.tweetNodeId);

                if (tweetNode != null && tweetNode.getProperty("node-type").toString().equals("tweet")) {

                    long pageId = (long) tweetNode.getProperty("page-id");

                    if (tweetNode.getSingleRelationship(DatabaseDriver.RelationTypes.TWEETED, Direction.INCOMING)
                            .getStartNodeId() == netClient.getDbHumanNodeId() || pageId == netClient.getDbHumanNodeId()) {

                        int tweetId = (int) tweetNode.getProperty("tweet-id");

                        synchronized (MainDriver.getInstance().getDatabaseDriver().getSortedTweets()) {

                            String queryGetLC = "select * from 'Tweets" + pageId + "' where TweetId = ?";
                            PreparedStatement prpStmtGetLC = MainDriver.getInstance().getDatabaseDriver().getSqlDB().prepareStatement(queryGetLC);
                            prpStmtGetLC.setInt(1, tweetId);
                            ResultSet rs = prpStmtGetLC.executeQuery();

                            if (rs.next()) {

                                long likesCount = rs.getLong("LikesCount");

                                MainDriver.getInstance().getDatabaseDriver().deleteTweetRank(tweetNode.getId(), likesCount);
                            }

                            rs.close();
                            prpStmtGetLC.close();
                        }

                        Node myNode = tweetNode.getSingleRelationship(DatabaseDriver.RelationTypes.TWEETED, Direction.INCOMING).getStartNode();

                        Lock lock = tx.acquireWriteLock(myNode);

                        int postsCount = (int) myNode.getProperty("posts-count");
                        postsCount--;
                        myNode.setProperty("posts-count", postsCount);

                        lock.release();

                        for (Relationship relationship : tweetNode.getRelationships()) {
                            relationship.delete();
                        }

                        tweetNode.delete();

                        String queryDeleteTweet = "delete from 'Tweets" + pageId + "' where TweetId = ?";
                        PreparedStatement prpStmtDeleteTweet = MainDriver.getInstance().getDatabaseDriver().getSqlDB().prepareStatement(queryDeleteTweet);
                        prpStmtDeleteTweet.setInt(1, tweetId);
                        prpStmtDeleteTweet.executeUpdate();
                        prpStmtDeleteTweet.close();

                        AnswerDeleteTweet answerDeleteTweet = new AnswerDeleteTweet();
                        answerDeleteTweet.answerStatus = AnswerStatus.OK;
                        answerDeleteTweet.packetCode = requestDeleteTweet.packetCode;
                        netClient.getConnection().sendTCP(answerDeleteTweet);
                    }
                    else {

                        AnswerDeleteTweet answerDeleteTweet = new AnswerDeleteTweet();
                        answerDeleteTweet.answerStatus = AnswerStatus.ERROR_1902;
                        answerDeleteTweet.packetCode = requestDeleteTweet.packetCode;
                        netClient.getConnection().sendTCP(answerDeleteTweet);
                    }
                }
                else {

                    AnswerDeleteTweet answerDeleteTweet = new AnswerDeleteTweet();
                    answerDeleteTweet.answerStatus = AnswerStatus.ERROR_1901;
                    answerDeleteTweet.packetCode = requestDeleteTweet.packetCode;
                    netClient.getConnection().sendTCP(answerDeleteTweet);
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

            AnswerDeleteTweet answerDeleteTweet = new AnswerDeleteTweet();
            answerDeleteTweet.answerStatus = AnswerStatus.ERROR_1900;
            answerDeleteTweet.packetCode = requestDeleteTweet.packetCode;
            netClient.getConnection().sendTCP(answerDeleteTweet);
        }
    }
}