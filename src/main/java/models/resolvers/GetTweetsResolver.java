package models.resolvers;

import drivers.MainDriver;
import models.memory.Human;
import models.memory.Tweet;
import models.network.NetClient;
import models.packets.AnswerGetTweets;
import models.packets.RequestGetTweets;
import models.packets.base.AnswerStatus;
import models.resolvers.base.BaseResolver;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
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
                        }
                        else {

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



                        tweets.add(tweet);
                    }

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