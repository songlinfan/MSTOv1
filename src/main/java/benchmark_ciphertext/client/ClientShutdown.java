package benchmark_ciphertext.client;

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
            dos.writeInt(-1);  // 先发 int 表示退出
            dos.flush();

            // 你可以根据实际需要选择是否还发送对象，或直接关闭连接

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
