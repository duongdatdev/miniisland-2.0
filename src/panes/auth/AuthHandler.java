package panes.auth;


import network.client.Client;
import network.client.WebSocketGameClient;
import panes.auth.signIn.SignInModel;

import javax.swing.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class AuthHandler extends Thread {
    private WebSocketGameClient webSocketClient;
    private boolean isRunning = true;
    private final Runnable onSuccess;

    public AuthHandler(Runnable onSuccess) {
        this.onSuccess = onSuccess;

        try {
            String ip = Client.getGameClient().getIP();
            int port = Client.getGameClient().getPort();
            URI serverUri = new URI("ws://" + ip + ":" + port);
            webSocketClient = new WebSocketGameClient(serverUri);
            webSocketClient.connectBlocking();
            
            // Set up message listener
            webSocketClient.setMessageListener(new WebSocketGameClient.MessageListener() {
                @Override
                public void onMessageReceived(String message) {
                    processResponse(message);
                }
            });
        } catch (IOException | URISyntaxException | InterruptedException ex) {
            JOptionPane.showMessageDialog(null, "Server is not running");
            System.exit(0);
        }
    }

    @Override
    public void run() {
        // WebSocket handles messages through listener
        while (isRunning) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                isRunning = false;
            }
        }
    }

    private void processResponse(String response) {
        if (response.startsWith("Login")) {

            String[] parts = response.split(",", 3);

            String status = parts[1];
            String msg = parts[2];

            SignInModel signInModel = SignInModel.getInstance();
            if (status.equals("Success")) {
                // System.out.println("Login Success");

                closeAll();

                SignInModel.getInstance().setSignedIn(true);

            } else {
                // System.out.println("Login Failed");
                signInModel.setSignedIn(false);

                signInModel.setMsg(msg);

                JOptionPane.showMessageDialog(null, msg);
            }
            isRunning = false;
            onSuccess.run();
        } else if (response.startsWith("Register")) {
            String[] parts = response.split(",", 3);

            String status = parts[1];
            String msg = parts[2];

            if (status.equals("Success")) {
                // System.out.println("Register Success");
                JOptionPane.showMessageDialog(null, msg);
                closeAll();
            } else {
                // System.out.println("Register Failed");
                JOptionPane.showMessageDialog(null, msg);
            }
            isRunning = false;
            onSuccess.run();
        }
    }

    public void sendToServer(String message) {
        if (message.equals("exit")) {
            System.exit(0);
        } else {
            if (webSocketClient != null && webSocketClient.isOpen()) {
                webSocketClient.sendMessage(message);
            }
        }
    }

    private void closeAll() {
        if (webSocketClient != null) {
            webSocketClient.closeConnection();
        }
    }

    public void stopHandler() {
        isRunning = false;
        closeAll();
    }
}
