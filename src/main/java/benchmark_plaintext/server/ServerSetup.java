package benchmark_plaintext.server;

import MST.EncryptedMultilayerStackedTree;
import common.Config;
import MST.MultilayerStackedTree;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerSetup {

    public static void main(String[] args) {
        int PORT = Integer.parseInt(Config.get("server_port"));
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started, waiting for client connections to retrieve MultilayerStackedTree instance...");

            try (
                    Socket clientSocket = serverSocket.accept();
                    ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream())
            ) {
                MultilayerStackedTree mst = (MultilayerStackedTree) ois.readObject();
                System.out.println("Successfully received MultilayerStackedTree instance. Preparing to write it to file mst.ser.");
                saveToDisk(mst);

            } catch (ClassNotFoundException e) {
                System.err.println("Class not found exception: " + e.getMessage());
            }
        } catch (IOException e) {
            System.err.println("Server exception: " + e.getMessage());
        }
    }

    public static void saveToDisk(MultilayerStackedTree mst){
        try (FileOutputStream fos = new FileOutputStream(Config.get("MSTFilePath"));
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(mst);
            System.out.println("MultilayerStackedTree instance has been successfully received and saved to " + Config.get("MSTFilePath"));
        } catch (IOException e) {
            System.err.println("Failed to save MST: " + e.getMessage());
            e.printStackTrace();
        }
    }
}