package models.resolvers;

import drivers.DatabaseDriver;
import drivers.MainDriver;
import models.memory.Human;
import models.memory.Tweet;
import models.network.NetClient;
import models.packets.AnswerGetTopTweets;
import models.packets.RequestGetTopTweets;
import models.packets.base.AnswerStatus;
import models.resolvers.base.BaseResolver;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;

public class GetTopTweetsResolver extends BaseResolver {

    @Override
    public void resolvePacket(NetClient netClient, Object packet) {

        RequestGetTopTweets requestGetTopTweets = (RequestGetTopTweets) packet;

        if (netClient.isAuthenticated()) {

            try {

                ArrayList<Long> topTweets = MainDriver.getInstance().getDatabaseDriver().getTopTweets();

                Transaction tx = MainDriver.getInstance().getDatabaseDriver().getGraphDB().beginTx();

                try {

                    Node myNode = MainDriver.getInstance().getDatabaseDriver().getGraphDB().getNodeById(netClient.getDbHumanNodeId());

                    HashSet<Long> myLikedTweetsIds = new HashSet<>();

                    for (Relationship relationship : myNode.getRelationships(DatabaseDriver.RelationTypes.LIKED, Direction.OUTGOING)) {
                        myLikedTweetsIds.add(relationship.getEndNodeId());
                    }

                    ArrayList<Tweet> tweets = new ArrayList<>();

                    Hashtable<Long, Human> cachedHumans = new Hashtable<>();

                    for (Long tweetNodeId : topTweets) {

                        Node tweetNode = MainDriver.getInstance().getDatabaseDriver().getGraphDB().getNodeById(tweetNodeId);

                        int tweetId = (int) tweetNode.getProperty("tweet-id");
                        long pageId = (long) tweetNode.getProperty("page-id");

                        String query = "select * from 'Tweets" + pageId + "' where TweetId = ?";
                        PreparedStatement prpStmt = MainDriver.getInstance().getDatabaseDriver().getSqlDB().prepareStatement(query);
                        prpStmt.setInt(1, tweetId);
                        ResultSet rs = prpStmt.executeQuery();

                        if (rs.next()) {

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

                            tweet.setNodeId(tweetNodeId);
                            tweet.setLikesCount(rs.getLong("LikesCount"));

                            tweet.setLikedByMe(myLikedTweetsIds.contains(tweetNodeId));

                            String queryComment = "select * from 'Tweets" + pageId + "' where ParentId = ? limit 2";
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
                                    human.setHumanId(humanId);
                                    human.setUserTitle(targetAuthorNode.getProperty("user-title").toString());
                                    comment.setAuthor(human);

                                    cachedHumans.put(humanId, human);
                                }

                                comment.setParentId(resultSet.getInt("ParentId"));
                                comment.setContent(resultSet.getString("Content"));
                                comment.setTime(resultSet.getLong("Time"));
                                comment.setNodeId(resultSet.getLong("NodeId"));
                                comment.setLikesCount(resultSet.getLong("LikesCount"));

                                comments.add(comment);
                            }

                            tweet.setTopComments(comments);

                            tweets.add(tweet);
                        }

                        rs.close();
                        prpStmt.close();
                    }

                    AnswerGetTopTweets answerGetTopTweets = new AnswerGetTopTweets();
                    answerGetTopTweets.answerStatus = AnswerStatus.OK;
                    answerGetTopTweets.packetCode = requestGetTopTweets.packetCode;
                    answerGetTopTweets.tweets = tweets;
                    netClient.getConnection().sendTCP(answerGetTopTweets);

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
            catch (Exception ignored) {
                ignored.printStackTrace();
            }
        }
        else {

            AnswerGetTopTweets answerGetTopTweets = new AnswerGetTopTweets();
            answerGetTopTweets.answerStatus = AnswerStatus.ERROR_1300;
            answerGetTopTweets.packetCode = requestGetTopTweets.packetCode;
            netClient.getConnection().sendTCP(answerGetTopTweets);
        }
    }
}