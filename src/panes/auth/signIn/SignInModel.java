package panes.auth.signIn;

import network.client.Client;
import network.client.Protocol;
import panes.auth.AuthMsg;
import panes.auth.AuthHandler;

public class SignInModel extends AuthMsg {
    private int id;
    private String username;
    private String password;
    private String email;
    private Client client;
    private boolean isSignedIn = false;

    private Protocol protocol = new Protocol();

    private static SignInModel instance;


    public static SignInModel getInstance() {
        if (instance == null) {
            instance = new SignInModel();
        }
        return instance;
    }

    private SignInModel() {
        client = Client.getGameClient();
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isSignedIn() {
        return isSignedIn;
    }

    public void setSignedIn(boolean signedIn) {
        isSignedIn = signedIn;
    }

    public void signIn(String username, String password, Runnable onSuccess) {
        if (username.isEmpty() || password.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(null, "Please fill in all fields!");
            return;
        }
        try {
            String loginRequest = "Login," + username + "," + password;

            AuthHandler authHandler = new AuthHandler(onSuccess);
            authHandler.start();


            // Send login request to server
            authHandler.sendToServer(loginRequest);

        } catch (Exception e) {
            // Handle IOException
            e.printStackTrace();
            return;
        }

    }


    public boolean signUp() {
        return true;
    }
}
