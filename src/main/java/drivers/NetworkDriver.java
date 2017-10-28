package drivers;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import models.memory.Human;
import models.memory.Tweet;
import models.network.NetClient;
import models.packets.*;
import models.packets.base.AnswerStatus;
import models.packets.base.BaseNotify;
import org.neo4j.graphdb.Lock;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;

public class NetworkDriver {

    public static final int tcpPort = 24500;
    public static final int udpPort = 24501;

    private Hashtable<Integer, NetClient> netClients;

    public NetworkDriver() throws IOException {

        this.netClients = new Hashtable<>();

        Server server = new Server();
        server.getKryo().register(Human.class);
        server.getKryo().register(Tweet.class);
        server.getKryo().register(NotifyUnFollow.class);
        server.getKryo().register(NotifyNewFollow.class);
        server.getKryo().register(NotifyNewTweet.class);
        server.getKryo().register(BaseNotify.class);
        server.getKryo().register(AnswerStatus.class);
        server.getKryo().register(RequestRegister.class);
        server.getKryo().register(RequestLogin.class);
        server.getKryo().register(RequestFollow.class);
        server.getKryo().register(RequestUnFollow.class);
        server.getKryo().register(RequestPostTweet.class);
        server.getKryo().register(RequestGetTweets.class);
        server.getKryo().register(RequestSearchUserTitle.class);
        server.getKryo().register(RequestGetHumanById.class);
        server.getKryo().register(RequestGetFollowers.class);
        server.getKryo().register(RequestGetFollowings.class);
        server.getKryo().register(AnswerRequest.class);
        server.getKryo().register(AnswerRegister.class);
        server.getKryo().register(AnswerLogin.class);
        server.getKryo().register(AnswerFollow.class);
        server.getKryo().register(AnswerUnFollow.class);
        server.getKryo().register(AnswerPostTweet.class);
        server.getKryo().register(AnswerGetTweets.class);
        server.getKryo().register(AnswerSearchUserTitle.class);
        server.getKryo().register(AnswerGetHumanById.class);
        server.getKryo().register(AnswerGetFollowers.class);
        server.getKryo().register(AnswerGetFollowings.class);
        server.getKryo().register(ArrayList.class);

        server.start();

        server.addListener(new Listener() {

            @Override
            public void connected(Connection connection) {

                super.connected(connection);

                NetClient netClient = new NetClient(connection);

                NetworkDriver.this.netClients.put(connection.getID(), netClient);

                System.out.println("KasperLogger : new client connected : " + connection.getID());
            }

            @Override
            public void disconnected(Connection connection) {

                super.disconnected(connection);

                NetClient netClient = NetworkDriver.this.netClients.get(connection.getID());

                if (netClient != null && netClient.isAuthenticated()) {

                    Transaction tx = MainDriver.getInstance().getDatabaseDriver().getGraphDB().beginTx();

                    try {
                        Node humanNode = MainDriver.getInstance().getDatabaseDriver().getGraphDB().getNodeById(netClient.getDbHumanNodeId());

                        Lock lock = tx.acquireWriteLock(humanNode);

                        humanNode.setProperty("is-online", false);
                        humanNode.setProperty("connection-id", -1);

                        if (lock != null) {
                            lock.release();
                        }

                        tx.success();
                    } catch (Exception ignored) {
                        tx.failure();
                    } finally {
                        tx.close();
                    }
                }

                NetworkDriver.this.netClients.remove(connection.getID());

                System.out.println("KasperLogger : client disconnected. : " + connection.getID());
            }

            @Override
            public void received(Connection connection, Object o) {

                super.received(connection, o);

                System.out.println("KasperLogger : packet received : " + o.toString());

                MainDriver.getInstance().getPacketRouter().directPacket(netClients.get(connection.getID()), o);
            }
        });

        server.bind(tcpPort, udpPort);
    }

    public Connection getConnectionById(int connectionId) {

        NetClient netClient = this.netClients.get(connectionId);

        if (netClient != null) {

            return netClient.getConnection();
        }

        return null;
    }
}