package models.resolvers;

import com.google.gson.Gson;
import drivers.MainDriver;
import models.network.NetClient;
import models.packets.AnswerLogin;
import models.packets.NotifyNewTweet;
import models.packets.RequestLogin;
import models.packets.base.AnswerStatus;
import models.packets.base.BaseNotify;
import models.resolvers.base.BaseResolver;
import org.neo4j.graphdb.Lock;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import sun.applet.Main;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class LoginResolver extends BaseResolver {

    @Override
    public void resolvePacket(NetClient netClient, Object packet) {

        RequestLogin requestLogin = (RequestLogin) packet;

        Transaction tx = MainDriver.getInstance().getDatabaseDriver().getGraphDB().beginTx();

        Lock lock = null;

        try {

            Node humanNode = MainDriver.getInstance().getDatabaseDriver().getGraphDB().getNodeById(requestLogin.userId);

            if (humanNode != null && humanNode.getProperty("node-type").toString().equals("human")
                    && humanNode.getProperty("pass-key").toString().equals(requestLogin.passKey)) {

                lock = tx.acquireWriteLock(humanNode);

                netClient.setAuthenticated(true);
                netClient.setDbHumanNodeId(humanNode.getId());

                humanNode.setProperty("is-online", true);
                humanNode.setProperty("connection-id", netClient.getConnection().getID());

                AnswerLogin answerLogin = new AnswerLogin();
                answerLogin.answerStatus = AnswerStatus.OK;
                answerLogin.packetCode = requestLogin.packetCode;
                answerLogin.userTitle = humanNode.getProperty("user-title").toString();

                answerLogin.postsCount = (int) humanNode.getProperty("posts-count");
                answerLogin.followersCount = (int) humanNode.getProperty("followers-count");
                answerLogin.followingCount = (int) humanNode.getProperty("following-count");

                netClient.getConnection().sendTCP(answerLogin);

                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        try {
                            String query = "select * from 'Updates" + netClient.getDbHumanNodeId() + "';";
                            Statement prpStmt = MainDriver.getInstance().getDatabaseDriver().getSqlDB().createStatement();
                            ResultSet resultSet = prpStmt.executeQuery(query);

                            Gson gson = new Gson();

                            while (resultSet.next()) {
                                String objType = resultSet.getString("ObjType");
                                BaseNotify baseNotify = null;
                                if (objType.equals("NewTweet")) {
                                    baseNotify = gson.fromJson(resultSet.getString("Content"), NotifyNewTweet.class);
                                } else if (objType.equals("NewFollow")) {
                                    baseNotify = gson.fromJson(resultSet.getString("Content"), NotifyNewTweet.class);
                                } else if (objType.equals("UnFollow")) {
                                    baseNotify = gson.fromJson(resultSet.getString("Content"), NotifyNewTweet.class);
                                }

                                if (baseNotify != null) {
                                    netClient.getConnection().sendTCP(baseNotify);
                                }
                            }

                            String queryDelete = "delete from 'Updates" + netClient.getDbHumanNodeId() + "'";
                            Statement prpStmtDlt = MainDriver.getInstance().getDatabaseDriver().getSqlDB().createStatement();
                            prpStmtDlt.execute(queryDelete);
                        }
                        catch (Exception ignored) {
                            ignored.printStackTrace();
                        }
                    }
                }).start();
            }
            else {

                AnswerLogin answerLogin = new AnswerLogin();
                answerLogin.answerStatus = AnswerStatus.ERROR_101;
                answerLogin.packetCode = requestLogin.packetCode;

                netClient.getConnection().sendTCP(answerLogin);
            }

            if (lock != null) {
                lock.release();
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