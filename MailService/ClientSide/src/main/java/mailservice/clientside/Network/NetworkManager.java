package mailservice.clientside.Network;

import mailservice.clientside.Configuration.ConfigManager;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.Objects;

public class NetworkManager {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private String serverHost;
    private int serverPort;

    private static NetworkManager instance;

    //istanza del ConfigManager per recuperarw le configurazioni
    ConfigManager configManager = ConfigManager.getInstance();

    private NetworkManager() {
        this.serverHost = configManager.readProperty("Client.ServerHost");
        this.serverPort = Integer.parseInt(configManager.readProperty("Client.ServerPort"));
    }

    //metodo per ottenere l'istanza singleton del NetworkManager
    public static NetworkManager getInstance(){
        if(instance ==null )
            instance = new NetworkManager();
        return instance;
    }

    //connessione al server
    public boolean connectToServer() {
        try {
            socket = new Socket(serverHost, serverPort);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("Connected to server" + serverHost + " on port " + serverPort);
            return true;
        } catch (SocketException ignored) {
            System.out.println("Socket error while connecting to server" + ignored.getMessage());
            disconnectFromServer();
            ignored.printStackTrace();
            return false;
        } catch (IOException e) {
            System.out.println("Error connecting to server" + e.getMessage());
            disconnectFromServer();
            e.printStackTrace();
            return false; //errore generale di I/O
        }
    }

    //disconnessione
    public void disconnectFromServer() {
        try {
            if(socket != null && !socket.isClosed()) {
                //chiude le risorse
                out.close();
                in.close();
                socket.close();
                System.out.println("Disconnected from server");
            }
        } catch (IOException e) {
            System.out.println("Error closing connection to server"+ e.getMessage());
            e.printStackTrace();
        }
    }

    // Metodo per inviare i messaggi
    public void sendMessage(String commandName, String arg) {
        if(out != null && Objects.equals(commandName, "CheckMail")) {
            out.println("USER_LOGIN " + arg); //invio la richiesta di login al server
        }
        if(out != null && Objects.equals(commandName, "Fetch")) {
            out.println("FETCH " + arg); //invio la richiesta di login al server
        }
    }

    // Metodo per ricevere i messaggi dal server
    public String receiveMessage() {
        try {
            return in.readLine(); // Legge una linea dal server
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean sendEmail(String sender, String receiver, String object, String content) {
        try {
            if (connectToServer()) {
                out.println("SEND_EMAIL From " + sender + " to " + receiver + " object " + object + " content " + content); // invio la richiesta di invio email al server
                System.out.println("Email sent from " + sender + " to " + receiver + " with object " + object);
                return true;
            } else {
                System.out.println("Error: Not connected to the server");
                return false;
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            return false;
        } finally {
            disconnectFromServer();
        }
    }

}
