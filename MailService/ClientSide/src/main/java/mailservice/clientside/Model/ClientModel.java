package mailservice.clientside.Model;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import mailservice.clientside.Configuration.ConfigManager;
import mailservice.clientside.Configuration.Email;
import mailservice.clientside.Network.NetworkManager;
import static mailservice.clientside.Configuration.CommandRequest.*;


public class ClientModel {

    private String userLogged;

    private static ClientModel instance;

    ConfigManager configManager = ConfigManager.getInstance();

    //creiamo un'istanza del NetworkManager per la connessione al server
    private final NetworkManager networkManager = NetworkManager.getInstance();

    private ClientModel() {
    }

    public static ClientModel getInstance() {
        if (instance == null)
            instance = new ClientModel();
        return instance;
    }

    public boolean validateEmail(String email) {
        boolean checkMail = Pattern.matches("^[a-zA-Z0-9.@_%+-]+@rama.it$", email.toLowerCase());
        return checkMail;
    }

    public boolean existingEmail(String email) {
        if (networkManager.sendCMD(LOGIN_CHECK, email)) {
            configManager.setProperty("Client.Mail", email);
            this.userLogged = email;
            return true;
        } else {
            return false;
        }
    }

    public List<Email> fetchEmails() {
        String response = networkManager.receiveMessage();
        System.out.println("[DEBUG] Server response: " + response);
        if (response == null || response.trim().isEmpty() || response.equals("No emails found.")) {
            return List.of();  // Ritorna una lista vuota se non ci sono email
        }

        return Arrays.stream(response.split("-----"))  // Divider usato sul server
                .map(emailData -> {
                    String[] parts = emailData.split("\n");
                    if (parts.length >= 4) {
                        String sender = parts[0].replace("From: ", "").trim();
                        String receivers = parts[1].replace("To: ", "").trim();
                        String subject = parts[2].replace("Subject: ", "").trim();
                        String content = parts[3].replace("Content: ", "").trim();
                        return new Email(sender, List.of(receivers.split(",")), subject, content);
                    }
                    return null;
                })
                .filter(Objects::nonNull)  // Rimuove eventuali email null
                .collect(Collectors.toList());
    }

    public String getUserEmail() {
        return userLogged;
    }

    public void logout() {
        userLogged = null;
    }
}
