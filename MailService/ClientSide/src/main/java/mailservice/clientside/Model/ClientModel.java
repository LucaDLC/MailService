package mailservice.clientside.Model;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Objects;
import java.util.regex.Pattern;

import mailservice.clientside.Configuration.ConfigManager;
import mailservice.clientside.Network.NetworkManager;
import mailservice.stdlib.CommandRequest;
import mailservice.stdlib.CommandResponse;

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
        if (checkMail)
        {
            configManager.setProperty("Client.Mail", email);
            this.userLogged = email;
            NetworkManager.getInstance().sendMessage("CheckMail",email);
        }
        return checkMail;
    }

    public boolean sendEmail(String sender, String receiver, String object, String content) {
        if(out != null) {
            out.println("SEND_EMAIL From " + sender + " to " + receiver + " object " + object + " content " + content); //invio la richiesta di invio email al server
            System.out.println("Email sent from " + sender + " to " + receiver + " with object " + object);
            return true;
        }
        System.out.println("Error: Not connected to the server or output stream is null");
        return false;
    }

    public String[] fetchEmails(String userLogged) {

        String[] emails = new String[10];

        if (NetworkManager.getInstance().connectToServer()) {
            NetworkManager.getInstance().sendMessage("Fetch", userLogged); // Invia la richiesta di fetch delle email
            try {
                String response;
                int i = 0;
                while ((response = NetworkManager.getInstance().receiveMessage()) != null) {
                    emails[i] = response; // Gestisci la risposta del server
                    i++;
                    // Gestisci la risposta del server
                    System.out.println("Received: " + response); // Esempio di gestione della risposta
                }
            } catch (Exception e) {
                e.printStackTrace();
            }finally {
                NetworkManager.getInstance().disconnectFromServer();
            }
        }
        return emails;
    }

}
