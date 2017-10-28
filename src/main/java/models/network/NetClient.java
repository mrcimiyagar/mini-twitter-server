package models.network;

import com.esotericsoftware.kryonet.Connection;

public class NetClient {

    private Connection connection;
    private boolean authenticated;
    private long dbHumanNodeId;

    public NetClient(Connection connection) {
        this.connection = connection;
        this.authenticated = false;
        this.dbHumanNodeId = -1;
    }

    public Connection getConnection() {
        return this.connection;
    }

    public boolean isAuthenticated() {
        return this.authenticated;
    }

    public long getDbHumanNodeId() {
        return this.dbHumanNodeId;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public void setDbHumanNodeId(long dbHumanNodeId) {
        this.dbHumanNodeId = dbHumanNodeId;
    }
}