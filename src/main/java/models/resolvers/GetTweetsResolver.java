package models.resolvers;

import drivers.DatabaseDriver;
import drivers.MainDriver;
import models.memory.Human;
import models.memory.Tweet;
import models.network.NetClient;
import models.packets.AnswerGetTweets;
import models.packets.RequestGetTweets;
import models.packets.base.AnswerStatus;
import models.resolvers.base.BaseResolver;
import org.neo4j.graphdb.*;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;

public class GetTweetsResolver extends BaseResolver {

    @Override
    public void resolvePacket(NetClient netClient, Object packet) {

        RequestGetTweets requestGetTweets = (RequestGetTweets) packet;

        Transaction tx = MainDriver.getInstance().getDatabaseDriver().getGraphDB().beginTx();

        try {

            if (netClient.isAuthenticated()) {

                Node targetNode = MainDriver.getInstance().getDatabaseDriver().getGraphDB().getNodeById(requestGetTweets.targetUserId);

                if (targetNode != null && targetNode.getProperty("node-type").toString().equals("human")) {

                    Node myNode = MainDriver.getInstance().getDatabaseDriver().getGraphDB().getNodeById(netClient.getDbHumanNodeId());

                    boolean isPrivate = (boolean)targetNode.getProperty("is-private");

                    boolean accessPermitted = false;

                    if (requestGetTweets.targetUserId == netClient.getDbHumanNodeId()) {
                        accessPermitted = true;
                    }
                    else if (!isPrivate) {
                        accessPermitted = true;
                    }
                    else {
                        for (Relationship relationship : myNode.getRelationships(DatabaseDriver.RelationTypes.FOLLOWED, Direction.OUTGOING)) {
                            if (relationship.getEndNodeId() == requestGetTweets.targetUserId) {
                                accessPermitted = true;
                                break;
                            }
                        }
                    }

                    if (accessPermitted) {

                        HashSet<Long> myLikedTweetsIds = new HashSet<>();

                        for (Relationship relationship : myNode.getRelationships(DatabaseDriver.RelationTypes.LIKED, Direction.OUTGOING)) {
                            myLikedTweetsIds.add(relationship.getEndNodeId());
                        }

                        PreparedStatement stmt = MainDriver.getInstance().getDatabaseDriver().getSqlDB().prepareStatement
                                ("select * from 'Tweets" + requestGetTweets.targetUserId + "' where ParentId = ? order by TweetId desc");
                        stmt.setInt(1, requestGetTweets.targetParentId);
                        ResultSet rs = stmt.executeQuery();

                        ArrayList<Tweet> tweets = new ArrayList<>();

                        Hashtable<Long, Human> cachedHumans = new Hashtable<>();

                        while (rs.next()) {

                            Tweet tweet = new Tweet();
                            tweet.setTweetId(rs.getInt("TweetId"));
                            tweet.setPageId(rs.getInt("PageId"));

                            long humanId = rs.getLong("AuthorId");

                            if (cachedHumans.containsKey(humanId)) {
                                tweet.setAuthor(cachedHumans.get(humanId));
                            } else {

                                Node targetAuthorNode = MainDriver.getInstance().getDatabaseDriver().getGraphDB().getNodeById(humanId);

                                Human human = new Human();
                                human.setHumanId(humanId);
                                human.setUserTitle(targetAuthorNode.getProperty("user-title").toString());
                                tweet.setAuthor(human);

                                cachedHumans.put(humanId, human);
                            }

                            tweet.setParentId(rs.getInt("ParentId"));
                            tweet.setContent(rs.getString("Content"));
                            tweet.setTime(rs.getLong("Time"));

                            long tweetNodeId = rs.getLong("NodeId");

                            tweet.setNodeId(tweetNodeId);
                            tweet.setLikesCount(rs.getLong("LikesCount"));

                            tweet.setLikedByMe(myLikedTweetsIds.contains(tweetNodeId));

                            String queryComment = "select * from 'Tweets" + requestGetTweets.targetUserId + "' where ParentId = ? limit 2";
                            PreparedStatement prpStmtComment = MainDriver.getInstance().getDatabaseDriver().getSqlDB().prepareStatement(queryComment);
                            prpStmtComment.setInt(1, tweet.getTweetId());
                            ResultSet resultSet = prpStmtComment.executeQuery();

                            ArrayList<Tweet> comments = new ArrayList<>();

                            while (resultSet.next()) {

                                Tweet comment = new Tweet();
                                comment.setTweetId(resultSet.getInt("TweetId"));
                                comment.setPageId(resultSet.getInt("PageId"));

                                long commentHumanId = resultSet.getLong("AuthorId");

                                if (cachedHumans.containsKey(commentHumanId)) {

                                    comment.setAuthor(cachedHumans.get(commentHumanId));
                                } else {

                                    Node targetAuthorNode = MainDriver.getInstance().getDatabaseDriver().getGraphDB().getNodeById(commentHumanId);

                                    Human human = new Human();
                                    human.setHumanId(targetAuthorNode.getId());
                                    human.setUserTitle(targetAuthorNode.getProperty("user-title").toString());
                                    comment.setAuthor(human);

                                    cachedHumans.put(human.getHumanId(), human);
                                }

                                comment.setParentId(resultSet.getInt("ParentId"));
                                comment.setContent(resultSet.getString("Content"));
                                comment.setTime(resultSet.getLong("Time"));
                                comment.setNodeId(resultSet.getLong("NodeId"));
                                comment.setLikesCount(resultSet.getLong("LikesCount"));

                                comments.add(comment);
                            }

                            resultSet.close();
                            prpStmtComment.close();

                            tweet.setTopComments(comments);

                            tweets.add(tweet);
                        }

                        rs.close();
                        stmt.close();

                        MainDriver.getInstance().getDatabaseDriver().getSqlDB().commit();

                        AnswerGetTweets answerGetTweets = new AnswerGetTweets();
                        answerGetTweets.packetCode = requestGetTweets.packetCode;
                        answerGetTweets.answerStatus = AnswerStatus.OK;
                        answerGetTweets.tweets = tweets;
                        netClient.getConnection().sendTCP(answerGetTweets);
                    }
                    else {

                        AnswerGetTweets answerGetTweets = new AnswerGetTweets();
                        answerGetTweets.packetCode = requestGetTweets.packetCode;
                        answerGetTweets.answerStatus = AnswerStatus.ERROR_502;
                        netClient.getConnection().sendTCP(answerGetTweets);
                    }
                }
                else {

                    AnswerGetTweets answerGetTweets = new AnswerGetTweets();
                    answerGetTweets.packetCode = requestGetTweets.packetCode;
                    answerGetTweets.answerStatus = AnswerStatus.ERROR_500;
                    netClient.getConnection().sendTCP(answerGetTweets);
                }
            }
            else {

                AnswerGetTweets answerGetTweets = new AnswerGetTweets();
                answerGetTweets.packetCode = requestGetTweets.packetCode;
                answerGetTweets.answerStatus = AnswerStatus.ERROR_501;
                netClient.getConnection().sendTCP(answerGetTweets);
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
}