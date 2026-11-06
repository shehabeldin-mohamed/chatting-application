package Client_Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private int port;
    private ServerSocket serverSocket;
    private String serverName;
    private List<String> bannedPhrases;
    private boolean running;
    private ExecutorService executorService;

    public Server(String path) throws IOException {
        ConfigMaker config = new ConfigMaker(path);
        this.port = config.getServerPort();
        this.serverSocket = new ServerSocket(port);
        this.serverName = config.getServerName();
        this.bannedPhrases = config.getBannedPhrases();
        running = true;
    }

    public void startServer() {
        System.out.println("Server " + serverName + " is running on port: " + serverSocket.getLocalPort());
        executorService = Executors.newCachedThreadPool();
        try {
            while (running) {
                Socket socket = serverSocket.accept();
                System.out.println("A new Client has connected!");
                executorService.execute(() -> {
                    ClientHandler clientHandler = new ClientHandler(socket, bannedPhrases);
                    clientHandler.run();
                });
            }
        } catch (IOException e) {
            closeServerSocket();
            running = false;
        }
    }

    public void closeServerSocket() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
            if (executorService != null) {
                executorService.shutdown();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        Server server = new Server("src/Client_Server/Server.config");
        server.startServer();
    }
}
