package Client_Server;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClientHandler implements Runnable {

    public static final List<ClientHandler> clientHandlers = new CopyOnWriteArrayList<>();
    private Socket socket;
    private BufferedReader bufferedReader;
    private PrintWriter printWriter;
    private String Username;
    private List<String> bannedPhrases;

    private boolean isPrivateMessage;
    private boolean isMultipleUsersMessage;
    private boolean isExceptUsersMessage;
    private boolean isBannedPhrasesQuery;



    public ClientHandler(Socket socket, List<String> bannedPhrases) {
        try {
            this.socket = socket;
            this.printWriter = new PrintWriter(socket.getOutputStream(), true);
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.bannedPhrases = bannedPhrases;

            this.Username = bufferedReader.readLine();
            clientHandlers.add(this);

            sendWelcomeMessage();
            sendTutorialMessage();
            sendConnectedUsers();
            broadcastMessage("SERVER: " + Username + " has entered the chat!");
        } catch (IOException e) {
            closeEverything(socket, bufferedReader, printWriter);
        }
    }


    @Override
    public void run() {
        String messageFromClient;

        while (socket.isConnected()) {
            try {
                messageFromClient = bufferedReader.readLine();

                if (messageFromClient != null) {
                    resetFlags();
                    String[] tokens = messageFromClient.split("\\s+", 3);

                    if (tokens.length > 0) {
                        switch (tokens[0]) {
                            case "-u":
                                isPrivateMessage = true;
                                if (tokens.length == 3) {
                                    handlePrivateMessage(tokens[1], tokens[2]);
                                } else {
                                    notifyClient("Invalid private message format. Use -u username message.");
                                }
                                break;
                            case "-m":
                                isMultipleUsersMessage = true;
                                if (tokens.length == 3) {
                                    handleMultipleUsersMessage(tokens[1], tokens[2]);
                                } else {
                                    notifyClient("Invalid multiple users message format. Use -m user1,user2,... message.");
                                }
                                break;
                            case "-e":
                                isExceptUsersMessage = true;
                                if (tokens.length == 3) {
                                    handleExceptUsersMessage(tokens[1], tokens[2]);
                                } else {
                                    notifyClient("Invalid except users message format. Use -e user1,user2,... message.");
                                }
                                break;
                            case "-b":
                                isBannedPhrasesQuery = true;
                                sendBannedPhrases();
                                break;
                            case "-l":
                                sendConnectedUsers();
                                break;
                            case "-q":
                                closeEverything(socket, bufferedReader, printWriter);
                                return;
                            default:
                                if (containsBannedPhrase(messageFromClient)) {
                                    List<String> detectedPhrases = getBannedPhrasesFromMessage(messageFromClient);
                                    String bannedPhrasesMessage = String.join(", ", detectedPhrases);
                                    notifyClient("Your Message Can't be sent. You have used a banned phrase(s): " + bannedPhrasesMessage);
                                } else {
                                    broadcastMessage(messageFromClient);
                                }
                                break;
                        }
                    }
                }
            } catch (IOException e) {
                closeEverything(socket, bufferedReader, printWriter);
                break;
            }
        }
    }

    // Messages and commands
    private void sendTutorialMessage() {
        try {
            printWriter.println("Type -l to receive list of users");
            printWriter.println("Type -b to get a list of banned phrases (If you use them your message will get blocked)");
            printWriter.println("Type -u Username Message to send a message to a specific user");
            printWriter.println("Type -m Username1,Username2.... Message to send a message to multiple users");
            printWriter.println("Type -e Username Message to send a message to all except this user");
            printWriter.println("Type -q to leave the chat");
        } catch (Exception e) {
            closeEverything(socket, bufferedReader, printWriter);
        }
    }

    private void sendWelcomeMessage() {
        try {
            printWriter.println("Welcome to the chat, " + Username + "!");
        } catch (Exception e) {
            closeEverything(socket, bufferedReader, printWriter);
        }
    }

    private void broadcastMessage(String messageToSend) {
        for (ClientHandler clientHandler : clientHandlers) {
            try {
                if (!clientHandler.Username.equals(Username)) {
                    clientHandler.printWriter.println(messageToSend);
                }
            } catch (Exception e) {
                closeEverything(socket, bufferedReader, printWriter);
            }
        }
    }

    private void sendConnectedUsers() {
        try {
            if (clientHandlers.size() == 1) {
                printWriter.println("You are the first user");
            } else {
                StringBuilder connectedUsers = new StringBuilder("Connected users: ");
                for (ClientHandler clientHandler : clientHandlers) {
                    connectedUsers.append(clientHandler.Username).append(", ");
                }
                if (connectedUsers.length() > 0) {
                    connectedUsers.setLength(connectedUsers.length() - 2);
                }
                printWriter.println(connectedUsers.toString());
            }
        } catch (Exception e) {
            closeEverything(socket, bufferedReader, printWriter);
        }
    }

    private void handleMultipleUsersMessage(String targetUsernames, String message) {
        if (containsBannedPhrase(message)) {
            List<String> detectedPhrases = getBannedPhrasesFromMessage(message);
            String bannedPhrasesMessage = String.join(", ", detectedPhrases);
            notifyClient("Your Message Can't be sent. You have used a banned phrase(s): " + bannedPhrasesMessage);
            return;
        }

        String[] usernames = targetUsernames.split(",");
        List<ClientHandler> targetClients = new ArrayList<>();
        for (String username : usernames) {
            for (ClientHandler clientHandler : clientHandlers) {
                if (clientHandler.Username.equals(username)) {
                    targetClients.add(clientHandler);
                    break;
                }
            }
        }

        for (ClientHandler targetClient : targetClients) {
            try {
                targetClient.printWriter.println("Message from " + Username + ": " + message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void handleExceptUsersMessage(String exceptUsernames, String message) {
        if (containsBannedPhrase(message)) {
            List<String> detectedPhrases = getBannedPhrasesFromMessage(message);
            String bannedPhrasesMessage = String.join(", ", detectedPhrases);
            notifyClient("Your Message Can't be sent. You have used a banned phrase(s): " + bannedPhrasesMessage);
            return;
        }

        String[] usernames = exceptUsernames.split(",");
        Set<String> exceptUsers = new HashSet<>(Arrays.asList(usernames));
        exceptUsers.add(Username);

        for (ClientHandler clientHandler : clientHandlers) {
            if (!exceptUsers.contains(clientHandler.Username)) {
                try {
                    clientHandler.printWriter.println("Message from " + Username + ": " + message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void handlePrivateMessage(String targetUsername, String privateMessage) {
        if (containsBannedPhrase(privateMessage)) {
            List<String> detectedPhrases = getBannedPhrasesFromMessage(privateMessage);
            String bannedPhrasesMessage = String.join(", ", detectedPhrases);
            notifyClient("Your Message Can't be sent. You have used a banned phrase(s): " + bannedPhrasesMessage);
            return;
        }

        ClientHandler targetClient = null;
        for (ClientHandler clientHandler : clientHandlers) {
            if (clientHandler.Username.equals(targetUsername)) {
                targetClient = clientHandler;
                break;
            }
        }

        if (targetClient != null) {
            try {
                targetClient.printWriter.println("message from " + Username + ": " + privateMessage);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            notifyClient("User " + targetUsername + " not found.");
        }
    }

    // Banned Phrases and rest of the methods
    private void sendBannedPhrases() {
        try {
            printWriter.println("Banned phrases: " + String.join(", ", bannedPhrases));
        } catch (Exception e) {
            closeEverything(socket, bufferedReader, printWriter);
        }
    }

    private boolean containsBannedPhrase(String message) {
        List<String> detectedPhrases = new ArrayList<>();
        for (String phrase : bannedPhrases) {
            if (message.contains(phrase)) {
                detectedPhrases.add(phrase);
            }
        }
        if (!detectedPhrases.isEmpty()) {
            System.out.println("Banned phrase(s) detected from " + Username + ": " + String.join(", ", detectedPhrases));
            return true;
        }
        return false;
    }

    private List<String> getBannedPhrasesFromMessage(String message) {
        List<String> detectedPhrases = new ArrayList<>();
        for (String phrase : bannedPhrases) {
            if (message.contains(phrase)) {
                detectedPhrases.add(phrase);
            }
        }
        return detectedPhrases;
    }

    private void resetFlags() {
        isPrivateMessage = false;
        isMultipleUsersMessage = false;
        isExceptUsersMessage = false;
        isBannedPhrasesQuery = false;
    }

    private void notifyClient(String message) {
        try {
            printWriter.println(message);
        } catch (Exception e) {
            closeEverything(socket, bufferedReader, printWriter);
        }
    }

    public void removeClientHandler() {
        clientHandlers.remove(this);
        broadcastMessage("SERVER: " + Username + " has left the chat!");
    }

    public void closeEverything(Socket socket, BufferedReader bufferedReader, PrintWriter printWriter) { // closes connection and streams
        removeClientHandler();
        try {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (printWriter != null) {
                printWriter.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}



