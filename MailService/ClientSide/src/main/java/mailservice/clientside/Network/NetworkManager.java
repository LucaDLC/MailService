package mailservice.clientside.Network;

import mailservice.clientside.Configuration.CommandRequest;
import mailservice.clientside.Configuration.CommandResponse;
import mailservice.clientside.Configuration.ConfigManager;
import mailservice.clientside.Configuration.Email;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;

import static mailservice.clientside.Configuration.CommandResponse.*;
import static mailservice.clientside.Configuration.CommandRequest.*;

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
    public boolean sendMessage(CommandRequest commandName, String arg) {
        if(out != null && commandName.equals(LOGIN_CHECK)) {
            out.println(LOGIN_CHECK + "|" + arg); //invio la richiesta di login al server
            if (receiveMessage().equals(SUCCESS)) {
                System.out.println("User logged in");
                return true;
            }
            else{
                System.out.println("User not logged in " + receiveMessage());
                return false;
            }
        }
        if(out != null && commandName.equals(FETCH_EMAIL)) {
            out.println(FETCH_EMAIL + "|" + arg); //invio la richiesta di login al server
            if (receiveMessage().equals(SUCCESS)) {
                System.out.println("User logged in");
                return true;
            }
            else{
                System.out.println("User not logged in " + receiveMessage());
                return false;
            }
        }
        return false;
    }

    // Metodo per ricevere i messaggi dal server
    public CommandResponse receiveMessage() {
        /*try {
            return in.readLine(); // Legge una linea dal server
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }*/
        return SUCCESS;
    }

    public boolean sendEmail(List<String> receivers, String object, String content) {
        try {
            if (connectToServer()) {
                String sender = configManager.readProperty("Client.Mail");
                Email email = new Email(sender, receivers, object, content);

                // Serialize the Email object (assuming Email class has a suitable toString method or use a JSON library)
                String serializedEmail = email.toString();

                // Send the serialized Email object
                out.println(serializedEmail);
                System.out.println("Email sent: " + serializedEmail);return true;
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
