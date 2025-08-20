package benchmark_plaintext.client;

import common.Config;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientShutdown {
    public static void main(String[] args) {
        String SERVER_HOST = Config.get("server_host");
        int SERVER_PORT = Integer.parseInt(Config.get("server_port"));

        try (
                Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream())
        ) {
            dos.writeInt(-1);
            dos.flush();



        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
