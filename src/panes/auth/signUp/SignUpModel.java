package panes.auth.signUp;

import network.client.Client;
import panes.auth.AuthMsg;
import panes.auth.AuthHandler;

public class SignUpModel extends AuthMsg {
    private String username;
    private String email;
    private String password;
    private String confirmPassword;
    private Client client;

    public SignUpModel() {
        client = Client.getGameClient();
    }

    /**
     * Creates a new account
     */
    public void signUp(Runnable onSuccess) {

        try {
            if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                javax.swing.JOptionPane.showMessageDialog(null, "Please fill in all fields!");
                return;
            }
            if (!password.equals(confirmPassword)) {
                javax.swing.JOptionPane.showMessageDialog(null, "Passwords do not match");
                return;
            }
            String loginRequest = "Register," + username + "," + confirmPassword + "," + email;

            AuthHandler authHandler = new AuthHandler(onSuccess);
            authHandler.start();

            // Send login request to server
            authHandler.sendToServer(loginRequest);

        } catch (Exception e) {
            // Handle IOException
            e.printStackTrace();
        }

    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
