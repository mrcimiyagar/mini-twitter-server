package drivers;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.*;
import java.util.*;

public class DatabaseDriver {

    private final TreeMap<Long, HashSet<Long>> sortedTweets;

    public TreeMap<Long, HashSet<Long>> getSortedTweets() {
        return sortedTweets;
    }

    public enum RelationTypes implements RelationshipType {
        REQUESTED,
        FOLLOWED,
        TWEETED,
        LIKED,
    }

    private static final String PARENT_DB_PATH = "C:/TTSP_DB";
    private static final String SQL_DB_PATH = PARENT_DB_PATH + "/SqlDatabase.sqlite";
    private static final String GRAPH_DB_PATH = PARENT_DB_PATH + "/GraphDatabase";

    private GraphDatabaseService graphDB;
    public GraphDatabaseService getGraphDB() {
        return this.graphDB;
    }

    private Connection sqlDB;
    public Connection getSqlDB() {
        return this.sqlDB;
    }

    public DatabaseDriver() throws ClassNotFoundException, SQLException, FileNotFoundException {

        File file = new File(PARENT_DB_PATH);

        if (!file.exists()) {
            if (!file.mkdirs()) {
                throw new FileNotFoundException();
            }
        }

        this.sqlDB = DriverManager.getConnection("jdbc:sqlite:" + SQL_DB_PATH);
        this.sqlDB.setAutoCommit(false);

        String tableQuery = "create table if not exists UsersTitles (HumanId bigint, UserTitle var, UserBio var);";
        Statement statement = this.sqlDB.createStatement();
        statement.execute(tableQuery);
        statement.close();

        this.sqlDB.commit();

        this.graphDB = new GraphDatabaseFactory().newEmbeddedDatabase(new File(GRAPH_DB_PATH));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                DatabaseDriver.this.graphDB.shutdown();
                DatabaseDriver.this.sqlDB.close();
            } catch (SQLException sqlExc) {
                sqlExc.printStackTrace();
            }
        }));

        this.sortedTweets = new TreeMap<>(Collections.reverseOrder());

        String queryGetUsers = "select * from UsersTitles";
        PreparedStatement prpStmtGetUsers = this.sqlDB.prepareStatement(queryGetUsers);
        ResultSet rsGU = prpStmtGetUsers.executeQuery();

        while (rsGU.next()) {

            long humanId = rsGU.getLong("HumanId");

            String queryGetTweets = "select * from 'Tweets" + humanId + "' where ParentId = ?";
            PreparedStatement prpStmtGetTweets = this.sqlDB.prepareStatement(queryGetTweets);
            prpStmtGetTweets.setInt(1, -1);
            ResultSet rsGT = prpStmtGetTweets.executeQuery();

            while (rsGT.next()) {

                if (rsGT.getLong("LikesCount") > 0) {

                    long tweetNodeId = rsGT.getLong("NodeId");
                    long likesCount = rsGT.getLong("LikesCount");

                    if (this.sortedTweets.containsKey(likesCount)) {
                        this.sortedTweets.get(likesCount).add(tweetNodeId);
                    } else {
                        HashSet<Long> tweetCapSet = new HashSet<>();
                        tweetCapSet.add(tweetNodeId);
                        this.sortedTweets.put(likesCount, tweetCapSet);
                    }
                }
            }

            rsGT.close();
            prpStmtGetTweets.close();
        }

        rsGU.close();
        prpStmtGetUsers.close();
    }

    public void updateTweetRank(Long tweetNodeId, long oldLC, long newLC) {

        synchronized (this.sortedTweets) {

            if (this.sortedTweets.containsKey(oldLC)) {
                this.sortedTweets.get(oldLC).remove(tweetNodeId);
            }

            if (newLC > 0) {
                if (this.sortedTweets.containsKey(newLC)) {
                    this.sortedTweets.get(newLC).add(tweetNodeId);
                } else {
                    HashSet<Long> tweetCapSet = new HashSet<>();
                    tweetCapSet.add(tweetNodeId);
                    this.sortedTweets.put(newLC, tweetCapSet);
                }
            }
        }
    }

    public void deleteTweetRank(Long tweetNodeId, long lc) {
        synchronized (this.sortedTweets) {
            if (this.sortedTweets.containsKey(lc)) {
                this.sortedTweets.get(lc).remove(tweetNodeId);
            }
        }
    }

    public ArrayList<Long> getTopTweets() {

        ArrayList<Long> tweetsNodeIds = new ArrayList<>();

        int counter = 0;

        boolean done = false;

        synchronized (this.sortedTweets) {

            for (Map.Entry<Long, HashSet<Long>> entry : this.sortedTweets.entrySet()) {

                for (Long tweetNodeId : entry.getValue()) {

                    tweetsNodeIds.add(tweetNodeId);
                    counter++;

                    if (counter >= 10) {
                        done = true;
                        break;
                    }
                }

                if (done) {
                    break;
                }
            }
        }

        return tweetsNodeIds;
    }
}