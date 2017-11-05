package models.resolvers;

import drivers.DatabaseDriver;
import drivers.MainDriver;
import models.memory.Human;
import models.memory.Tweet;
import models.network.NetClient;
import models.packets.AnswerGetFeed;
import models.packets.RequestGetFeed;
import models.packets.base.AnswerStatus;
import models.resolvers.base.BaseResolver;
import org.neo4j.graphdb.*;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

public class GetFeedResolver extends BaseResolver {

    @Override
    public void resolvePacket(NetClient netClient, Object packet) {

        RequestGetFeed requestGetFeed = (RequestGetFeed) packet;

        if (netClient.isAuthenticated()) {

            Transaction tx = MainDriver.getInstance().getDatabaseDriver().getGraphDB().beginTx();

            try {

                Node myNode = MainDriver.getInstance().getDatabaseDriver().getGraphDB().getNodeById(netClient.getDbHumanNodeId());

                HashSet<Long> myLikedTweetsIds = new HashSet<>();

                for (Relationship relationship : myNode.getRelationships(DatabaseDriver.RelationTypes.LIKED, Direction.OUTGOING)) {
                    myLikedTweetsIds.add(relationship.getEndNodeId());
                }

                ArrayList<Tweet> tweets = new ArrayList<>();

                Hashtable<Long, Human> cachedHumans = new Hashtable<>();

                for (Relationship relationship : myNode.getRelationships(DatabaseDriver.RelationTypes.FOLLOWED, Direction.OUTGOING)) {

                    Node targetNode = relationship.getEndNode();

                    Human mainHuman = new Human();
                    mainHuman.setHumanId(targetNode.getId());
                    mainHuman.setUserTitle(targetNode.getProperty("user-title").toString());

                    String query = "select * from 'Tweets" + mainHuman.getHumanId() + "' where ParentId = -1 order by TweetId desc limit 5";
                    PreparedStatement prpStmt = MainDriver.getInstance().getDatabaseDriver().getSqlDB().prepareStatement(query);
                    ResultSet rs = prpStmt.executeQuery();

                    while (rs.next()) {

                        Tweet tweet = new Tweet();
                        tweet.setTweetId(rs.getInt("TweetId"));
                        tweet.setPageId(rs.getLong("PageId"));
                        tweet.setAuthor(mainHuman);
                        tweet.setNodeId(rs.getLong("NodeId"));
                        tweet.setTime(rs.getLong("Time"));
                        tweet.setParentId(rs.getInt("ParentId"));
                        tweet.setContent(rs.getString("Content"));
                        tweet.setLikesCount(rs.getLong("LikesCount"));
                        tweet.setLikedByMe(myLikedTweetsIds.contains(tweet.getNodeId()));

                        String queryComment = "select * from 'Tweets" + mainHuman.getHumanId() + "' where ParentId = ? limit 2";
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
                            }
                            else {

                                Node targetAuthorNode = MainDriver.getInstance().getDatabaseDriver().getGraphDB().getNodeById(commentHumanId);

                                Human human = new Human();
                                human.setHumanId(commentHumanId);
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
                    prpStmt.close();
                }

                tweets.sort(new Comparator<Tweet>() {
                    @Override
                    public int compare(Tweet t1, Tweet t2) {

                        long result = t1.getTime() - t2.getTime();

                        if (result > 0) {
                            return 1;
                        } else if (result < 0) {
                            return -1;
                        } else {
                            return 0;
                        }
                    }
                });

                AnswerGetFeed answerGetFeed = new AnswerGetFeed();
                answerGetFeed.answerStatus = AnswerStatus.OK;
                answerGetFeed.packetCode = requestGetFeed.packetCode;
                answerGetFeed.tweets = tweets;
                netClient.getConnection().sendTCP(answerGetFeed);

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

            AnswerGetFeed answerGetFeed = new AnswerGetFeed();
            answerGetFeed.answerStatus = AnswerStatus.ERROR_1700;
            answerGetFeed.packetCode = requestGetFeed.packetCode;
            netClient.getConnection().sendTCP(answerGetFeed);
        }
    }
}