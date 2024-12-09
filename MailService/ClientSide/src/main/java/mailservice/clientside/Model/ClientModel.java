package mailservice.clientside.Model;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Objects;
import java.util.regex.Pattern;

import mailservice.clientside.Configuration.CommandRequest;
import mailservice.clientside.Configuration.CommandResponse;
import mailservice.clientside.Configuration.ConfigManager;
import mailservice.clientside.Network.NetworkManager;
import static mailservice.clientside.Configuration.CommandRequest.*;
import static mailservice.clientside.Configuration.CommandResponse.*;


public class ClientModel {

    private String userLogged;

    private static ClientModel instance;

    ConfigManager configManager = ConfigManager.getInstance();

    //creiamo un'istanza del NetworkManager per la connessione al server
    private NetworkManager networkManager = NetworkManager.getInstance();

    private ClientModel() {
    }

    public static ClientModel getInstance(){
        if(instance ==null )
            instance = new ClientModel();
        return instance;
    }

    public boolean validateEmail(String email){
        boolean checkMail = Pattern.matches("^[a-zA-Z0-9.@_%+-]+@rama.it$", email.toLowerCase());
        if (checkMail && networkManager.sendMessage(LOGIN_CHECK,email)){
            configManager.setProperty("Client.Mail", email);
            this.userLogged = email;
            return true;
        }
        else {
            return false;
        }
    }

    public String[] fetchEmails() {
        String userEmail = configManager.readProperty("Client.Mail").trim();
        if (userEmail.isEmpty()) {
            System.out.println("[ERROR] User email not set.");
            return new String[] { "User email not set" };
        }

        if (!networkManager.connectToServer()) {
            System.out.println("[ERROR] Not connected to server.");
            return new String[] { "Connection error" };
        }

        boolean success = networkManager.sendMessage(CommandRequest.FETCH_EMAIL, userEmail);
        if (!success) {
            System.out.println("[ERROR] Failed to send FETCH_EMAIL command.");
            return new String[] { "Fetch email failed" };
        }

        String payload = networkManager.getLastPayload();
        if (payload == null || payload.isEmpty() || payload.equals("No emails found for user: " + userEmail)) {
            System.out.println("[INFO] No emails found for user: " + userEmail);
            return new String[] { "No emails found" };
        }

        System.out.println("[DEBUG] Payload received: " + payload);
        return payload.split("\n");
    }

    public void logout() {
        userLogged = null; // Reset dell'utente loggato
        configManager.setProperty("Client.Mail", ""); // Rimuove l'email salvata
        networkManager.clearSessionData(); // Pulisce i dati della sessione nel NetworkManager
    }

}
