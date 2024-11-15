package mailservice.clientside.Model;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;
import mailservice.clientside.Configuration.ConfigManager;

public class ClientModel {

    private String userLogged;
    private String serverHost;
    private int serverPort;
    private int fetchPeriod;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    ConfigManager configManager = ConfigManager.getInstance();

    private ClientModel() {
        this.serverHost = configManager.readProperty("Client.ServerHost");
        this.serverPort = Integer.parseInt(configManager.readProperty("Client.ServerPort"));
        this.fetchPeriod = Integer.parseInt(configManager.readProperty("Client.Fetch"));
    }

    public static ClientModel getInstance(){
        return new ClientModel();
    }

    //metodo per connettersi al server
    public boolean connectToServer() {
        try {
            this.socket = new Socket(this.serverHost, this.serverPort);
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.in = new BufferedReader(new java.io.InputStreamReader(socket.getInputStream()));
            System.out.println("Connected to server" + this.serverHost + " on port " + this.serverPort);
            return true;
        } catch (Exception e) {
            System.out.println("Error connecting to server" + e.getMessage());
            return false;
        }
    }

    //metodo per chiudere la connessione al server
    public void disconnectFromServer() {
        try {
            if(this.socket != null && !this.socket.isClosed()) {
                this.socket.close();
            }
        } catch (Exception e) {
            System.out.println("Errore nella chiusura della connessione al server"+ e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean validateEmail(String email){
        boolean checkMail = Pattern.matches("^[a-zA-Z0-9.@_%+-]+@rama.it$", email.toLowerCase());
        if (checkMail)
        {
            configManager.setProperty("Client.Mail", email);
            this.userLogged = email;
            sendLogicRequest(email);
        }
        return checkMail;
    }

    private void sendLogicRequest(String email) {
        if(out != null) {
            out.println("LOGIN " + email); //invio la richiesta di login al server
        }
    }

    public boolean sendEmail(String sender, String receiver, String object, String content) {
        if(out != null) {
            out.println("SEND " + sender + " " + receiver + " " + object + " " + content); //invio la richiesta di invio email al server
            return true;
        }
        System.out.println("Error: Not connected to the server or output stream is null");
        return false;
    }

    public static String formatDate(Date date) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        return dateFormatter.format(date);
    }


}
