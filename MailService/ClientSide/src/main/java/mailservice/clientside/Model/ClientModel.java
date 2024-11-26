package mailservice.clientside.Model;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Objects;
import java.util.regex.Pattern;

import mailservice.clientside.Configuration.ConfigManager;
import mailservice.clientside.Network.NetworkManager;

public class ClientModel {

    private String userLogged;
    private String serverHost;
    private int serverPort;
    private int fetchPeriod;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private static ClientModel instance;

    ConfigManager configManager = ConfigManager.getInstance();

    //creiamo un'istanza del NetworkManager per la connessione al server
    private NetworkManager networkManager = NetworkManager.getInstance();

    private ClientModel() {
        this.serverHost = configManager.readProperty("Client.ServerHost");
        this.serverPort = Integer.parseInt(configManager.readProperty("Client.ServerPort"));
        this.fetchPeriod = Integer.parseInt(configManager.readProperty("Client.Fetch"));
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
            sendLogicRequest("CheckMail",email);
        }
        return checkMail;
    }

    private void sendLogicRequest(String commandName, String arg) {  //usiamolo anche per ii comandi di sistema
        if(out != null && Objects.equals(commandName, "CheckMail")) {
            out.println("USER_LOGIN " + arg); //invio la richiesta di login al server
        }
        if(out != null && Objects.equals(commandName, "Fetch")) {
            out.println("FETCH " + arg); //invio la richiesta di login al server
        }
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

    // Metodo per il fetch delle email
    public String[] fetchEmails() {

        String[] emails = new String[10];

        if (networkManager.connectToServer()) {
            sendLogicRequest("Fetch", userLogged); // Invia la richiesta di fetch delle email
            try {
                String response;
                int i = 0;
                while ((response = networkManager.receiveMessage()) != null) {
                    emails[i] = response; // Gestisci la risposta del server
                    i++;
                    // Gestisci la risposta del server
                    System.out.println("Received: " + response); // Esempio di gestione della risposta
                }
            } catch (Exception e) {
                e.printStackTrace();
            }finally {
                closeConnection();
            }
        }
        return emails;
    }

    public void closeConnection() {
        networkManager.disconnectFromServer();
    }
}
