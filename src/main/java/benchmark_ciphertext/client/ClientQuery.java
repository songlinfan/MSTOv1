package benchmark_ciphertext.client;

import MST.Bucket;
import MST.KeywordToLeaf;
import common.Config;
import MST.Stash;
import common.InitDataHelper;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Map;

public class ClientQuery {
    private static final String SERVER_HOST = Config.get("server_host");
    private static final int SERVER_PORT = Integer.parseInt(Config.get("server_port"));


    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("Please provide a keyword as a command-line argument.");
            return;
        }
        KeywordToLeaf keywordToLeaf = new KeywordToLeaf(Config.get("CipherKeywordToLeafFilePath"));

        String searchKeyword = args[0];
        int leaf = keywordToLeaf.getLeafOfKeyword(searchKeyword);


        try (
                Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
        ) {
            dos.writeInt(leaf);
            dos.flush();
            System.out.println("Sent trapdoor(leaf)="+leaf +" of searchKeyword=" + searchKeyword + " to server: " + leaf);
            Map<Integer, Map<Integer, byte[]>> encryptedResponse = (Map<Integer, Map<Integer, byte[]>>) ois.readObject();

            Stash stash = Stash.loadFromDisk(Config.get("CipherStashFilePath"));
            stash.parseEncryptedResponse(searchKeyword,encryptedResponse);

            System.out.println("Waiting to update and evict all received blocks...");
            stash.evict(searchKeyword,keywordToLeaf);
            Map<Integer, Map<Integer, Bucket>> updateResponse = stash.getBktArr();
            Map<Integer, Map<Integer, byte[]>> encryptedUpdateResponse = stash.getEncryptedBktArr();
            oos.writeObject(encryptedUpdateResponse);
            oos.flush();

            keywordToLeaf.saveToDisk(Config.get("CipherKeywordToLeafFilePath"));
            stash.saveToDisk(Config.get("CipherStashFilePath"));
            System.out.println("All received blocks have been updated and evicted to the server");
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }


}
