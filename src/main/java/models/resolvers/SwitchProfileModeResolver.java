package models.resolvers;

import drivers.MainDriver;
import models.network.NetClient;
import models.packets.AnswerSwitchProfileMode;
import models.packets.RequestSwitchProfileMode;
import models.packets.base.AnswerStatus;
import models.resolvers.base.BaseResolver;
import org.neo4j.graphdb.Lock;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

public class SwitchProfileModeResolver extends BaseResolver {

    @Override
    public void resolvePacket(NetClient netClient, Object packet) {

        RequestSwitchProfileMode requestSwitchProfileMode = (RequestSwitchProfileMode) packet;

        if (netClient.isAuthenticated()) {

            Transaction tx = MainDriver.getInstance().getDatabaseDriver().getGraphDB().beginTx();

            try {

                Node myNode = MainDriver.getInstance().getDatabaseDriver().getGraphDB().getNodeById(netClient.getDbHumanNodeId());

                Lock lock = tx.acquireWriteLock(myNode);

                boolean isProfilePrivate = (boolean) myNode.getProperty("is-private");
                isProfilePrivate = !isProfilePrivate;
                myNode.setProperty("is-private", isProfilePrivate);

                lock.release();

                AnswerSwitchProfileMode answerSwitchProfileMode = new AnswerSwitchProfileMode();
                answerSwitchProfileMode.answerStatus = AnswerStatus.OK;
                answerSwitchProfileMode.packetCode = requestSwitchProfileMode.packetCode;
                answerSwitchProfileMode.newState = isProfilePrivate;
                netClient.getConnection().sendTCP(answerSwitchProfileMode);

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

            AnswerSwitchProfileMode answerSwitchProfileMode = new AnswerSwitchProfileMode();
            answerSwitchProfileMode.answerStatus = AnswerStatus.ERROR_1800;
            answerSwitchProfileMode.packetCode = requestSwitchProfileMode.packetCode;
            netClient.getConnection().sendTCP(answerSwitchProfileMode);
        }
    }
}