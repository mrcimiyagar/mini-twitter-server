package drivers;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import javax.swing.plaf.nimbus.State;
import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseDriver {

    public enum RelationTypes implements RelationshipType {
        FOLLOWS,
        TWEETED,
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

        String tableQuery = "create table if not exists UsersTitles (HumanId bigint, UserTitle var);";
        Statement statement = this.sqlDB.createStatement();
        statement.execute(tableQuery);
        statement.close();

        this.sqlDB.commit();

        this.graphDB = new GraphDatabaseFactory().newEmbeddedDatabase(new File(GRAPH_DB_PATH));

        /*for (int counter = 0; counter < 100; counter++) {

            Transaction tx = this.graphDB.beginTx();

            Node node = this.graphDB.createNode();
            node.setProperty("node-type", "human");

            tx.success();
            tx.close();

            System.out.println("Added " + counter);
        }*/

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                DatabaseDriver.this.graphDB.shutdown();
                DatabaseDriver.this.sqlDB.close();
            } catch (SQLException sqlExc) {
                sqlExc.printStackTrace();
            }
        }));
    }
}