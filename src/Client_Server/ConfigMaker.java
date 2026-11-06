package Client_Server;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class ConfigMaker {
    String path;
    int serverPort;
    int clientPort;
    List<String> bannedPhrases;
    String serverName;

    public ConfigMaker(String path) {
        this.path = path;
        this.bannedPhrases = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("=", 2);
                if (parts.length < 2) continue;
                String key = parts[0].trim();
                String value = parts[1].trim();

                if (path.contains("Server.config")) {
                    switch (key) {
                        case "port":
                            serverPort = Integer.parseInt(value);
                            break;
                        case "name":
                            serverName = value;
                            break;
                        case "banned_phrases":
                            String[] phrases = value.split(",");
                            for (String phrase : phrases) {
                                bannedPhrases.add(phrase.trim());
                            }
                            break;
                    }
                } else if (path.contains("Client.config")) {
                    if (key.equals("client_port")) {
                        clientPort = Integer.parseInt(value);
                    } else if (key.equals("server_port")) {
                        serverPort = Integer.parseInt(value);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getServerPort() {
        return serverPort;
    }
    public int getClientPort() {
        return clientPort;
    }
    public String getServerName() {
        return serverName;
    }
    public List<String> getBannedPhrases() {
        return bannedPhrases;
    }
}

