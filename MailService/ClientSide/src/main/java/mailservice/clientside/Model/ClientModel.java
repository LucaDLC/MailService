package mailservice.clientside.Model;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import mailservice.clientside.Configuration.ConfigManager;
import mailservice.clientside.Controller.MainController;

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
            e.printStackTrace();;
            return false;
        }
    }

    //metodo per chiudere la connessione al server
    public void disconnectFromServer() {
        try {
            if(this.socket != null && !this.socket.isClosed()) {
                this.socket.close();
                System.out.println("Disconnected from server");
            }
        } catch (Exception e) {
            System.out.println("Error closing connection to server"+ e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean validateEmail(String email){
        boolean checkMail = Pattern.matches("^[a-zA-Z0-9.@_%+-]+@rama.it$", email.toLowerCase());
        if (checkMail)
        {
            configManager.setProperty("Client.Mail", email);
            this.userLogged = email;
            CartellaCreazione(email);
            sendLogicRequest(email);
        }
        return checkMail;
    }

    private void sendLogicRequest(String email) {
        if(out != null) {
            out.println("USER_LOGIN " + email); //invio la richiesta di login al server
            System.out.println("Login request sent for email: " + email);
        }
    }

    //metodo per ottenere le email dal server
    public String[] fetchEmails(){
        startReconnection(); //controlla se la connessione è attiva, altrimenti prova a riconnettersi
        if(this.socket == null || this.socket.isClosed()) {
            System.out.println("Socket is closed. cannot fetch emails");
            return new String[0];
        }

        try{
            out.println("FETCH"); //chiede al server di inviare le email
            String response;
            List<String> emails =new ArrayList<>();
            while((response = in.readLine()) != null) {
                if ("END".equals(response)) {
                    break;
                }
                String[] parts = response.split(";");
                if(parts.length >= 3) {
                    String sender = parts[0];
                    String subject = parts[1];
                    String date = parts[2];
                    String emailPreview = ("From: "+ sender + " | Subject: " + subject + " | Date " + date);
                    emails.add(emailPreview); //aggiungo l'email alla lista
                }
            }

            return emails.toArray(new String[0]);
        }catch(IOException e){
            System.out.println("Error fetching email: "+e.getMessage());
            e.printStackTrace();
            return new String[0];
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

    //riconnessione automatica al server in caso di disconnessione
    public void startReconnection(){
        if(this.socket == null || this.socket.isClosed()){
            System.out.println("Socket is closed. Attempting reconnection...");
            while(!connectToServer()){
                try{
                    Thread.sleep(5000); //attendi 5 secondi prima di tentare di riconnettersi
                }catch(InterruptedException e){
                    System.out.println("Error in reconnection thread: "+e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    //metodo principale per gestire i cicli di fetch delle email
    public void startFetchingEmails(MainController controller){
        Thread fetchThread = new Thread(() ->{
            while(true){
                fetchEmails();
                try{
                    Thread.sleep(fetchPeriod * 1000); //attendi per il periodo di fetch
                }catch(InterruptedException e){
                    System.out.println("Error in fetching emails: "+e.getMessage());
                    e.printStackTrace();
                }
            }
        });
        fetchThread.setDaemon(true); //permette al thread di terminate quando il programma principale termina
        fetchThread.start();
    }
    private void CartellaCreazione (String email) {

            // Percorso in cui cercare la cartella
        String path = new File("").getAbsolutePath() + File.separator + "ClientSide" + File.separator + "src" + File.separator + "main" + File.separator + "java" + File.separator + "mailservice" + File.separator + "clientside" + File.separator + "Customers" + File.separator + email;
        File cartella = new File(path);

        if (!cartella.exists()) {
                // Se la cartella non esiste, la crea
            if (cartella.mkdir()) {
                System.out.println("Cartella creata con successo: " + email);
            } else {
                System.out.println("Errore nella creazione della cartella.");
            }
        } else {
            System.out.println("La cartella esiste già: " + email);
        }

    }

}
