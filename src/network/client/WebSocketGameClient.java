package network.client;

import javax.swing.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

/**
 * WebSocket-based client for Mini Island 2D game
 * Replaces TCP Socket with WebSocket communication
 */
public class WebSocketGameClient extends WebSocketClient {

    private Protocol protocol;
    private CountDownLatch latch;
    private String lastReceivedMessage;
    private MessageListener messageListener;

    public interface MessageListener {
        void onMessageReceived(String message);
    }

    public WebSocketGameClient(URI serverUri) throws IOException {
        super(serverUri);
        protocol = new Protocol();
        latch = new CountDownLatch(1);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("Connected to server");
    }

    @Override
    public void onMessage(String message) {
        // System.out.println("Received: " + message);
        lastReceivedMessage = message;
        
        // Notify listener if set
        if (messageListener != null) {
            messageListener.onMessageReceived(message);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("Connection closed: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("WebSocket error: " + ex.getMessage());
        ex.printStackTrace();
    }

    public void setMessageListener(MessageListener listener) {
        this.messageListener = listener;
    }

    public void sendMessage(String message) {
        if (isOpen()) {
            send(message);
        } else {
            System.err.println("Cannot send message, connection is not open");
        }
    }

    public String getLastReceivedMessage() {
        return lastReceivedMessage;
    }

    public Protocol getGameProtocol() {
        return protocol;
    }

    public void closeConnection() {
        close();
    }
}
