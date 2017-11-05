package models.resolvers;

import drivers.MainDriver;
import models.network.NetClient;
import models.packets.AnswerRegister;
import models.packets.RequestRegister;
import models.packets.base.AnswerStatus;
import models.resolvers.base.BaseResolver;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.unsafe.impl.batchimport.stats.Stat;
import utilities.KeyUtils;

import java.sql.PreparedStatement;
import java.sql.Statement;

public class RegisterResolver extends BaseResolver {

    @Override
    public void resolvePacket(NetClient netClient, Object packet) {

        RequestRegister requestRegister = (RequestRegister) packet;

        Transaction tx = MainDriver.getInstance().getDatabaseDriver().getGraphDB().beginTx();

        try {

            Node myNode = MainDriver.getInstance().getDatabaseDriver().getGraphDB().createNode();

            long userId = myNode.getId();
            String passKey = KeyUtils.makeKey64();

            System.out.println(userId + " " + passKey);

            myNode.setProperty("node-type", "human");
            myNode.setProperty("pass-key", passKey);
            myNode.setProperty("user-title", requestRegister.userTitle);
            myNode.setProperty("is-online", false);
            myNode.setProperty("posts-count", 0);
            myNode.setProperty("followers-count", 0);
            myNode.setProperty("following-count", 0);
            myNode.setProperty("is-private", false);
            myNode.setProperty("connection-id", -1);

            String sqlQuery0 = "insert into UsersTitles (HumanId, UserTitle, UserBio) values (?, ?, ?);";
            PreparedStatement prpStmt = MainDriver.getInstance().getDatabaseDriver().getSqlDB().prepareStatement(sqlQuery0);
            prpStmt.setLong(1, userId);
            prpStmt.setString(2, requestRegister.userTitle);
            prpStmt.setString(3, "");
            prpStmt.executeUpdate();
            prpStmt.close();

            String sqlQuery = "create table if not exists 'Tweets" + userId + "' (TweetId integer primary key autoincrement, PageId bigint, AuthorId bigint, ParentId integer, Content var, Time bigint, NodeId bigint, LikesCount bigint);";
            Statement statement = MainDriver.getInstance().getDatabaseDriver().getSqlDB().createStatement();
            statement.execute(sqlQuery);
            statement.close();

            String sqlQuery2 = "create table if not exists 'Updates" + userId + "' (Content var, ObjType var);";
            Statement statement2 = MainDriver.getInstance().getDatabaseDriver().getSqlDB().createStatement();
            statement2.execute(sqlQuery2);
            statement2.close();

            MainDriver.getInstance().getDatabaseDriver().getSqlDB().commit();

            AnswerRegister answerRegister = new AnswerRegister();
            answerRegister.packetCode = requestRegister.packetCode;
            answerRegister.answerStatus = AnswerStatus.OK;
            answerRegister.userId = userId;
            answerRegister.passKey = passKey;
            netClient.getConnection().sendTCP(answerRegister);

            tx.success();

        } catch (Exception ignored) {
            ignored.printStackTrace();
            tx.failure();
        } finally {
            tx.close();
        }
    }
}