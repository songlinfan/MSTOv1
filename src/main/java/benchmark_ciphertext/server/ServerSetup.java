package benchmark_ciphertext.server;

import common.Config;
import MST.EncryptedMultilayerStackedTree;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerSetup {

    public static void main(String[] args) {
        int PORT = Integer.parseInt(Config.get("server_port"));
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started, waiting for client connections to retrieve Encrypted MultilayerStackedTree instance...");

            try (
                    Socket clientSocket = serverSocket.accept();
                    ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream())
            ) {
                EncryptedMultilayerStackedTree emst  = (EncryptedMultilayerStackedTree)ois.readObject();
                saveToDisk(emst);
            } catch (ClassNotFoundException e) {
                System.err.println("Class not found exception: " + e.getMessage());
            }

        } catch (IOException e) {
            System.err.println("Server exception: " + e.getMessage());
        }
    }

    public static void saveToDisk(EncryptedMultilayerStackedTree emst){
        try (FileOutputStream fos = new FileOutputStream(Config.get("EMSTFilePath"));
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(emst);
            System.out.println("Encrypted MultilayerStackedTree instance has been successfully received and saved to "+ Config.get("EMSTFilePath"));
        } catch (IOException e) {
            System.err.println("Failed to save EMST: " + e.getMessage());
            e.printStackTrace();
        }
    }

}