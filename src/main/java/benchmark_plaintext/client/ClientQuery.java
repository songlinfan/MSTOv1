package benchmark_plaintext.client;

import MST.Bucket;
import MST.KeywordToLeaf;
import MST.Stash;
import common.Config;
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
        KeywordToLeaf keywordToLeaf = new KeywordToLeaf(Config.get("PlainKeywordToLeafFilePath"));

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

            System.out.println("Sent trapdoor(leaf)="+leaf +" of keyword=" + searchKeyword + " to server: " + leaf);
            Map<Integer, Map<Integer, Bucket>> response = (Map<Integer, Map<Integer, Bucket>>) ois.readObject();

            Stash stash = Stash.loadFromDisk("PlainStashFilePath");
            stash.parseResponse(searchKeyword,response);

            System.out.println("Waiting to update and evict all received blocks...");
            stash.evict(searchKeyword,keywordToLeaf);
            Map<Integer, Map<Integer, Bucket>> updateResponse = stash.getBktArr();
            oos.writeObject(updateResponse);
            oos.flush();

            stash.saveToDisk(Config.get("PlainStashFilePath"));
            keywordToLeaf.modifyKeywordToLeaf(Config.get("PlainKeywordToLeafFilePath"),searchKeyword,leaf);

            System.out.println("All received blocks have been updated and evicted to the server");
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }


}
