package benchmark_ciphertext.server;

import common.Config;
import MST.EncryptedMultilayerStackedTree;
import common.InitDataHelper;


import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Map;

public class ServerResponse {
    private static final String FILE_NAME = Config.get("EMSTFilePath");
    private static final int PORT = Integer.parseInt(Config.get("server_port"));
    private static final String LOG_FILE = Config.get("CiphertextLOGFilePath");


    public static void main(String[] args) {
        System.out.println("Reading EncryptedMultilayerStackedTree into memory...");
        EncryptedMultilayerStackedTree emst = InitDataHelper.loadEMST(FILE_NAME);
        if (emst == null) {
            System.err.println("EMST not loaded. Server is exiting. Please execute Setup and ensure file exists: " + FILE_NAME);
            return;
        }

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            int timeout = Integer.parseInt(Config.get("server_timeout"));
            serverSocket.setSoTimeout(timeout * 60 * 1000);

            System.out.println("Server is running on port " + PORT + ", waiting for a search query from client...");

            while (true) {
                try (
                        Socket clientSocket = serverSocket.accept();
                        DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
                        ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream());
                        ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream())
                ) {
                    int receivedLeaf = dis.readInt();
                    if(receivedLeaf == -1)    break;

                    System.out.println("Received leaf from client: " + receivedLeaf);
                    Map<Integer, Map<Integer, byte[]>> encryptedResponse = emst.getBucketsAcrossAllLayersFromLeafToRoot(receivedLeaf);
                    System.err.printf("Size of response to be sent: %.2f MB (communication overhead)%n", InitDataHelper.getSerializedSizeInMB(encryptedResponse));

                    oos.writeObject(encryptedResponse);
                    oos.flush();
                    InitDataHelper.appendLog(LOG_FILE,"\n    Received leaf: " + receivedLeaf + "\n    Response: " + encryptedResponse.values());


                    Map<Integer, Map<Integer, byte[]>>  encryptedUpdatedResponse =
                            (Map<Integer, Map<Integer, byte[]>>) ois.readObject();
                    emst.updateBucketsAcrossAllLayersFromLeafToRoot(encryptedUpdatedResponse);
                    System.out.println("Ready for the next query...");

                } catch (SocketTimeoutException e) {
                    System.out.println("No client connected in the last " + timeout  + " minutes. Server shutting down...");
                    break;
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }

            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_NAME))) {
                System.out.println("Connection with the client has been closed. Now saving the updated EMST instance.");
                oos.writeObject(emst);
                System.out.println("Updated EMST instance has been saved to" + FILE_NAME);
            } catch (IOException e) {
                System.err.println("Failed to save updated EMST instance: " + e.getMessage());
            }
        } catch (IOException e) {
            System.err.println("Server socket error: " + e.getMessage());
        }
    }


}
