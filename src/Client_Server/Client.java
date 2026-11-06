package Client_Server;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client {

    private Socket socket;
    private BufferedReader bufferedReader;
    private PrintWriter printWriter;
    private String username;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public Client(Socket socket,String username) {
        try {
            this.socket = socket;
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.printWriter = new PrintWriter(socket.getOutputStream(), true);
            this.username = username;
        }catch (IOException e){
            closeEverything(socket,bufferedReader, printWriter);
        }
    }

    public void sendMessage() {
        try {
            printWriter.println(username);

            Scanner scanner = new Scanner(System.in);
            while (socket.isConnected()) {
                String messageToSend = scanner.nextLine();
                String[] tokens = messageToSend.split("\\s+", 3);

                if (tokens.length > 0) {
                    String flag = tokens[0];
                    switch (flag) {
                        case "-u":
                        case "-m":
                        case "-e":
                            if (tokens.length == 3) {
                                printWriter.println(messageToSend);
                            } else {
                                System.out.println("Invalid message format. Use the correct format for the flag.");
                                continue;
                            }
                            break;
                        case "-b":
                        case "-l":
                            printWriter.println(messageToSend);
                            break;
                        case "-q":
                            printWriter.println(messageToSend);
                            closeEverything(socket, bufferedReader, printWriter);
                            System.exit(0);
                            return;
                        default:
                            printWriter.println(username + ": " + messageToSend);
                            break;
                    }
                }
            }
        } catch (Exception e) {
            closeEverything(socket, bufferedReader, printWriter);
        }
    }

    public void ListenMessage() {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                String messageFromGroupChat;
                while (socket.isConnected()) {
                    try {
                        messageFromGroupChat = bufferedReader.readLine();
                        System.out.println(messageFromGroupChat);
                    } catch (IOException e) {
                        closeEverything(socket, bufferedReader, printWriter);
                        break;
                    }
                }
            }
        });
    }

    public void closeEverything(Socket socket,BufferedReader bufferedReader,PrintWriter printWriter){
        try{
            if(socket != null){
                socket.close();
            }
            if(bufferedReader != null){
                bufferedReader.close();
            }
            if(printWriter != null){
                printWriter.close();
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        ConfigMaker config = new ConfigMaker("src/Client_Server/Client.config");
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter your username: ");
        String username = scanner.nextLine();

        connectToServer(config, username);
    }

    private static void connectToServer(ConfigMaker config, String username) throws IOException {
        try {
            Socket socket = new Socket("localhost", config.getClientPort());
            Client client = new Client(socket, username);
            client.ListenMessage();
            client.sendMessage();
        } catch (IOException e) {
            System.out.println("Error: Unable to connect to the server. Please try again later.");
            throw e;
        }
    }

}
