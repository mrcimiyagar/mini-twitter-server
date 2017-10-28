package drivers;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Scanner;

public class MainDriver {

    private static MainDriver instance;
    public static MainDriver getInstance() {
        return MainDriver.instance;
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException, SQLException {

        MainDriver.instance = new MainDriver();
    }

    // ****

    private DatabaseDriver databaseDriver;
    private PacketRouter packetRouter;
    private NetworkDriver networkDriver;

    public DatabaseDriver getDatabaseDriver() {
        return this.databaseDriver;
    }

    public PacketRouter getPacketRouter() {
        return this.packetRouter;
    }

    public NetworkDriver getNetworkDriver() {
        return this.networkDriver;
    }

    private MainDriver() throws IOException, ClassNotFoundException, SQLException {

        instance = this;

        System.out.println("Server machine started.");

        this.databaseDriver = new DatabaseDriver();

        System.out.println("Database driver started.");

        this.packetRouter = new PacketRouter();

        System.out.println("Packet router started.");

        this.networkDriver = new NetworkDriver();

        System.out.println("Network driver started.");

        System.out.println("Server machine is online now.");

        while (true) {

            String command = new Scanner(System.in).nextLine();

            if (command.equals("down")) {
                System.exit(0);
            }
        }
    }
}