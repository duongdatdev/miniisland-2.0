package network.client;

import javax.swing.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Client class using WebSocket instead of TCP Socket
 */
public class Client {

    /**
     * Creates a new instance of Client
     */

    private WebSocketGameClient webSocketClient;
    private String hostName = "localhost";
    private int serverPort = 11111;
    private Protocol protocol;

    private static Client client;

    private Client() throws IOException {
        protocol = new Protocol();

        try {
            URI serverUri = new URI("ws://" + hostName + ":" + serverPort);
            webSocketClient = new WebSocketGameClient(serverUri);
            webSocketClient.connectBlocking();
        } catch (URISyntaxException | InterruptedException ex) {
            JOptionPane.showMessageDialog(null, "Server is not running");
            System.exit(0);
        }
    }

    public void register(String message) throws IOException {
        sendToServer(message);
    }

    public void sendToServer(String message) {
        if (message.equals("exit")) {
            System.exit(0);
        } else {
            if (webSocketClient != null && webSocketClient.isOpen()) {
                webSocketClient.sendMessage(message);
            } else {
                System.err.println("WebSocket connection is not open");
            }
        }
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    public WebSocketGameClient getWebSocketClient() {
        return webSocketClient;
    }

    public String getIP() {
        return hostName;
    }

    public static Client getGameClient() {
        if (client == null) {
            try {
                client = new Client();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return client;
    }

    public void closeAll() {
        if (webSocketClient != null) {
            webSocketClient.closeConnection();
        }
    }

    public void setMessageListener(WebSocketGameClient.MessageListener listener) {
        if (webSocketClient != null) {
            webSocketClient.setMessageListener(listener);
        }
    }
}
