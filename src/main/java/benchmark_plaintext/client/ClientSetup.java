package benchmark_plaintext.client;

import common.Config;
import common.InitDataHelper;
import MST.MultilayerStackedTree;

import java.io.*;
import java.net.Socket;
import java.util.Map;

public class ClientSetup {
    private static String SERVER_HOST = Config.get("server_host");
    private static int SERVER_PORT = Integer.parseInt(Config.get("server_port"));
    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream())) {

            int numOfKeywords = InitDataHelper.getNumOfKeywords();
            if (numOfKeywords == -1) {
                System.err.println("Error: Invalid number of keywords. The value is either less than 0 or exceeds the number of keywords in the dataset.");
                System.exit(1);
            }

            System.out.println("Loading dataset...");
            Map<Integer, Integer>  chunkCounts = InitDataHelper.getChunkCounts(numOfKeywords);

            MultilayerStackedTree mst = new MultilayerStackedTree(chunkCounts);
            System.out.println("DBInfo: Number of keywords = " + numOfKeywords + ", Number of UUIDs = " + InitDataHelper.getNumOfUUIDs());
            System.out.println("MultilayerStackedTree has been initialized, waiting for the dataset to be inserted into it...");
            mst.insertDataSetIntoMST(numOfKeywords,false);
            mst.padding();

            System.out.println("Sending the constructed MultilayerStackedTree to the server... ");
            oos.writeObject(mst);
            oos.flush();
            System.out.println("The MultilayerStackedTree has been sent to the server.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}