package benchmark_plaintext.server;

import MST.Bucket;
import common.Config;
import MST.MultilayerStackedTree;
import common.InitDataHelper;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Map;

public class ServerResponse {
    private static final String FILE_NAME = Config.get("MSTFilePath");
    private static final int PORT = Integer.parseInt(Config.get("server_port"));
    private static final String LOG_FILE = Config.get("PlaintextLOGFilePath");

    public static void main(String[] args) {
        System.out.println("Reading MultilayerStackedTree into memory...");
        MultilayerStackedTree mst = InitDataHelper.loadMLT(FILE_NAME);
        if (mst == null) {
            System.err.println("MST not loaded, server exiting.");
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
                    Map<Integer, Map<Integer, Bucket>> response = mst.ResponseToSearchQuery(receivedLeaf);
                    System.err.printf("Size of response to be sent: %.2f MB (communication overhead)%n", InitDataHelper.getSerializedSizeInMB(response));
                    oos.writeObject(response);
                    oos.flush();
                    InitDataHelper.appendLog(LOG_FILE,"\n    Received leaf: " + receivedLeaf + "\n    Response: " + response.values());
                    System.out.println("Communication log has been recorded in " + LOG_FILE + ".");


                    Map<Integer, Map<Integer, Bucket>> updatedResponse =
                            (Map<Integer, Map<Integer, Bucket>>) ois.readObject();
                    mst.Replace(updatedResponse);
                    System.out.println("Ready for the next query...");
                } catch (SocketTimeoutException e) {
                    System.out.println("No client connected in the last " + timeout  + " minutes. Server shutting down...");
                    break;
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }

            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_NAME))) {
                System.out.println("Connection with the client has been closed. Now saving the updated MST instance.");
                oos.writeObject(mst);
                System.out.println("Updated MST instance has been saved " + FILE_NAME);
            } catch (IOException e) {
                System.err.println("Failed to save updated MST instance: " + e.getMessage());
            }

        } catch (IOException e) {
            System.err.println("Server socket error: " + e.getMessage());
        }
    }




}
