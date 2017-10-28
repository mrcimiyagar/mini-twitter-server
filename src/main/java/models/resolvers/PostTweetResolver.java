package models.resolvers;

import com.google.gson.Gson;
import drivers.DatabaseDriver;
import drivers.MainDriver;
import models.memory.Human;
import models.memory.Tweet;
import models.network.NetClient;
import models.packets.AnswerPostTweet;
import models.packets.NotifyNewTweet;
import models.packets.RequestPostTweet;
import models.packets.base.AnswerStatus;
import models.packets.base.BaseNotify;
import models.resolvers.base.BaseResolver;
import org.neo4j.graphdb.*;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class PostTweetResolver extends BaseResolver {

    @Override
    public void resolvePacket(NetClient netClient, Object packet) {

        RequestPostTweet requestPostTweet = (RequestPostTweet) packet;

        Transaction tx = MainDriver.getInstance().getDatabaseDriver().getGraphDB().beginTx();

        try {
            if (netClient.isAuthenticated()) {

                Node myNode = MainDriver.getInstance().getDatabaseDriver().getGraphDB().getNodeById(netClient.getDbHumanNodeId());

                if (myNode != null && myNode.getProperty("node-type").toString().equals("human")) {

                    if (requestPostTweet.parentId >= 0 || (requestPostTweet.parentId == -1 && requestPostTweet.pageId == netClient.getDbHumanNodeId())) {

                        if (requestPostTweet.tweetContent.length() > 0) {

                            Node targetHumanNode = MainDriver.getInstance().getDatabaseDriver().getGraphDB().getNodeById(requestPostTweet.pageId);

                            if (targetHumanNode != null && targetHumanNode.getProperty("node-type").toString().equals("human")) {

                                boolean found = false;

                                if (requestPostTweet.parentId >= 0) {
                                    System.out.println("requested parent id : " + requestPostTweet.parentId);

                                    String queryExist = "select (count(*) > 0) as found from 'Tweets" + requestPostTweet.pageId + "' where TweetId = ?";
                                    PreparedStatement pst = MainDriver.getInstance().getDatabaseDriver().getSqlDB().prepareStatement(queryExist);
                                    pst.setInt(1, requestPostTweet.parentId);

                                    try (ResultSet rs = pst.executeQuery()) {

                                        if (rs.next()) {

                                            found = rs.getBoolean(1);
                                        }
                                    }

                                    pst.close();
                                    MainDriver.getInstance().getDatabaseDriver().getSqlDB().commit();
                                } else {

                                    found = true;
                                }

                                if (found) {

                                    long time = System.currentTimeMillis();

                                    String query = "insert into 'Tweets" + requestPostTweet.pageId + "' (AuthorId, PageId, ParentId, Content, Time) values (?, ?, ?, ?, ?);";
                                    PreparedStatement statement = MainDriver.getInstance().getDatabaseDriver().getSqlDB().prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
                                    statement.setLong(1, netClient.getDbHumanNodeId());
                                    statement.setLong(2, requestPostTweet.pageId);
                                    statement.setInt(3, requestPostTweet.parentId);
                                    statement.setString(4, requestPostTweet.tweetContent);
                                    statement.setLong(5, time);
                                    int affectedRows = statement.executeUpdate();

                                    if (affectedRows > 0) {

                                        try (ResultSet resultSet = statement.getGeneratedKeys()) {

                                            if (resultSet.next()) {

                                                int tweetId = resultSet.getInt(1);

                                                Node tweetNode = MainDriver.getInstance().getDatabaseDriver().getGraphDB().createNode();
                                                tweetNode.setProperty("node-type", "tweet");
                                                tweetNode.setProperty("tweet-id", tweetId);

                                                myNode.createRelationshipTo(tweetNode, DatabaseDriver.RelationTypes.TWEETED);

                                                if (requestPostTweet.parentId == -1) {

                                                    Lock lock = tx.acquireWriteLock(myNode);

                                                    int postsCount = (int) myNode.getProperty("posts-count");
                                                    postsCount++;
                                                    myNode.setProperty("posts-count", postsCount);

                                                    if (lock != null) {
                                                        lock.release();
                                                    }
                                                }

                                                Tweet tweet = new Tweet();
                                                tweet.setTweetId(tweetId);
                                                tweet.setPageId(requestPostTweet.pageId);

                                                Human myHuman = new Human();
                                                myHuman.setHumanId(myNode.getId());
                                                myHuman.setUserTitle(myNode.getProperty("user-title").toString());

                                                tweet.setAuthor(myHuman);
                                                tweet.setParentId(requestPostTweet.parentId);
                                                tweet.setContent(requestPostTweet.tweetContent);
                                                tweet.setTime(time);

                                                AnswerPostTweet answerPostTweet = new AnswerPostTweet();
                                                answerPostTweet.packetCode = requestPostTweet.packetCode;
                                                answerPostTweet.answerStatus = AnswerStatus.OK;
                                                answerPostTweet.tweet = tweet;
                                                netClient.getConnection().sendTCP(answerPostTweet);

                                                NotifyNewTweet notifyNewTweet = new NotifyNewTweet();
                                                notifyNewTweet.setTweet(tweet);

                                                if ((boolean) targetHumanNode.getProperty("is-online")) {
                                                    com.esotericsoftware.kryonet.Connection connection = MainDriver.getInstance()
                                                            .getNetworkDriver().getConnectionById((int) targetHumanNode
                                                                    .getProperty("connection-id"));
                                                    if (connection != null) {
                                                        connection.sendTCP(notifyNewTweet);
                                                    } else {
                                                        cacheUpdate(targetHumanNode.getId(), notifyNewTweet);
                                                    }
                                                } else {
                                                    cacheUpdate(targetHumanNode.getId(), notifyNewTweet);
                                                }

                                                if (requestPostTweet.parentId == -1) {
                                                    new Thread(new Runnable() {
                                                        @Override
                                                        public void run() {

                                                            Transaction tx = MainDriver.getInstance().getDatabaseDriver().getGraphDB().beginTx();

                                                            Node myHumanNode = MainDriver.getInstance().getDatabaseDriver().getGraphDB().getNodeById(netClient.getDbHumanNodeId());

                                                            for (Relationship relationship : myHumanNode.getRelationships(DatabaseDriver.RelationTypes.FOLLOWS, Direction.INCOMING)) {

                                                                System.out.println("sending notify tweet to user : " + relationship.getStartNode().getProperty("user-title").toString());

                                                                try {
                                                                    Node followerNode = relationship.getStartNode();

                                                                    if (followerNode.getId() != myHumanNode.getId()) {

                                                                        if ((boolean) followerNode.getProperty("is-online")) {
                                                                            com.esotericsoftware.kryonet.Connection connection = MainDriver.getInstance()
                                                                                    .getNetworkDriver().getConnectionById((int) followerNode
                                                                                            .getProperty("connection-id"));
                                                                            if (connection != null) {
                                                                                connection.sendTCP(notifyNewTweet);
                                                                            } else {
                                                                                cacheUpdate(followerNode.getId(), notifyNewTweet);
                                                                            }
                                                                        } else {
                                                                            cacheUpdate(followerNode.getId(), notifyNewTweet);
                                                                        }
                                                                    }
                                                                } catch (Exception ignored) {
                                                                    ignored.printStackTrace();
                                                                }
                                                            }

                                                            tx.success();
                                                            tx.close();
                                                        }
                                                    }).start();
                                                }

                                            } else {

                                                AnswerPostTweet answerPostTweet = new AnswerPostTweet();
                                                answerPostTweet.packetCode = requestPostTweet.packetCode;
                                                answerPostTweet.answerStatus = AnswerStatus.ERROR_400;
                                                netClient.getConnection().sendTCP(answerPostTweet);
                                            }
                                        }
                                    } else {

                                        AnswerPostTweet answerPostTweet = new AnswerPostTweet();
                                        answerPostTweet.packetCode = requestPostTweet.packetCode;
                                        answerPostTweet.answerStatus = AnswerStatus.ERROR_400;
                                        netClient.getConnection().sendTCP(answerPostTweet);
                                    }

                                    statement.close();
                                    MainDriver.getInstance().getDatabaseDriver().getSqlDB().commit();

                                } else {

                                    AnswerPostTweet answerPostTweet = new AnswerPostTweet();
                                    answerPostTweet.packetCode = requestPostTweet.packetCode;
                                    answerPostTweet.answerStatus = AnswerStatus.ERROR_404;
                                    netClient.getConnection().sendTCP(answerPostTweet);
                                }
                            } else {

                                AnswerPostTweet answerPostTweet = new AnswerPostTweet();
                                answerPostTweet.packetCode = requestPostTweet.packetCode;
                                answerPostTweet.answerStatus = AnswerStatus.ERROR_405;
                                netClient.getConnection().sendTCP(answerPostTweet);
                            }
                        } else {

                            AnswerPostTweet answerPostTweet = new AnswerPostTweet();
                            answerPostTweet.packetCode = requestPostTweet.packetCode;
                            answerPostTweet.answerStatus = AnswerStatus.ERROR_401;
                            netClient.getConnection().sendTCP(answerPostTweet);
                        }
                    }
                    else {

                        AnswerPostTweet answerPostTweet = new AnswerPostTweet();
                        answerPostTweet.packetCode = requestPostTweet.packetCode;
                        answerPostTweet.answerStatus = AnswerStatus.ERROR_406;
                        netClient.getConnection().sendTCP(answerPostTweet);
                    }
                }
                else {

                    AnswerPostTweet answerPostTweet = new AnswerPostTweet();
                    answerPostTweet.packetCode = requestPostTweet.packetCode;
                    answerPostTweet.answerStatus = AnswerStatus.ERROR_402;
                    netClient.getConnection().sendTCP(answerPostTweet);
                }
            }
            else {

                AnswerPostTweet answerPostTweet = new AnswerPostTweet();
                answerPostTweet.packetCode = requestPostTweet.packetCode;
                answerPostTweet.answerStatus = AnswerStatus.ERROR_403;
                netClient.getConnection().sendTCP(answerPostTweet);
            }

            tx.success();
        } catch (Exception ignored) {
            ignored.printStackTrace();
            tx.failure();
        } finally {
            tx.close();
        }
    }

    private void cacheUpdate(long humanId, BaseNotify baseNotify) throws SQLException {

        String query = "insert into 'Updates" + humanId + "' (Content, ObjType) values (?, ?);";
        PreparedStatement prpStmt = MainDriver.getInstance().getDatabaseDriver().getSqlDB().prepareStatement(query);
        prpStmt.setString(1, new Gson().toJson(baseNotify));
        prpStmt.setString(2, "NewTweet");
        prpStmt.executeUpdate();
        prpStmt.close();
        MainDriver.getInstance().getDatabaseDriver().getSqlDB().commit();
    }
}