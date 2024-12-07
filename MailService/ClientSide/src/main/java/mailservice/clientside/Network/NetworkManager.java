package mailservice.clientside.Network;

import mailservice.clientside.Configuration.CommandRequest;
import mailservice.clientside.Configuration.CommandResponse;
import mailservice.clientside.Configuration.ConfigManager;
import mailservice.clientside.Configuration.Email;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.List;

public class NetworkManager {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private final String serverHost;
    private final int serverPort;

    private static final int SOCKET_TIMEOUT = 20000; // Timeout di 20 secondi
    private static NetworkManager instance;

    private String lastPayload; //per memorizzare l'ultimo payload ricevuto

    // Istanza del ConfigManager per recuperare le configurazioni
    private final ConfigManager configManager = ConfigManager.getInstance();

    private NetworkManager() {
        this.serverHost = configManager.readProperty("Client.ServerHost");
        this.serverPort = Integer.parseInt(configManager.readProperty("Client.ServerPort"));
    }

    // Metodo per ottenere l'istanza singleton del NetworkManager
    public static NetworkManager getInstance() {
        if (instance == null) {
            instance = new NetworkManager();
        }
        return instance;
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    // Connessione al server
    public synchronized boolean connectToServer() {
        if (isConnected()) {
            System.out.println("Already connected to server.");
            return true;
        }

        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(serverHost, serverPort), SOCKET_TIMEOUT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("Connected to server " + serverHost + " on port " + serverPort);
            return true;
        } catch (SocketTimeoutException e) {
            System.out.println("Error: Connection to server timed out.");
        } catch (SocketException e) {
            System.out.println("Socket error while connecting to server: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Error connecting to server: " + e.getMessage());
            e.printStackTrace();
        }

        disconnectFromServer();
        return false;
    }

    // Disconnessione dal server
    public synchronized void disconnectFromServer() {
        if (socket != null) {
            try {
                if (out != null) out.close();
                if (in != null) in.close();
                socket.close();
                System.out.println("Disconnected from server");
            } catch (IOException e) {
                System.out.println("Error while disconnecting: " + e.getMessage());
                e.printStackTrace();
            } finally {
                socket = null;
                out = null;
                in = null;
            }
        }
    }

    // Metodo per inviare i messaggi
    public synchronized boolean sendMessage(CommandRequest commandName, String arg) {
        if (!isConnected() && !connectToServer()) {
            System.out.println("Unable to connect to server. Message not sent.");
            return false;
        }

        try {
            out.println(commandName + "|" + arg);
            out.flush(); // Assicura che il messaggio venga inviato immediatamente
            return true;
        } catch (Exception e) {
            System.out.println("Error sending message: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Metodo per ricevere i messaggi dal server
    public synchronized CommandResponse receiveMessage() {
        try {
            socket.setSoTimeout(SOCKET_TIMEOUT);
            String response = in.readLine(); // Legge una linea dal server
            if (response != null) {
                System.out.println("Response received: " + response);
                String[] parts = response.split("\\|", 2); // Divide il messaggio in "comando" e "dati aggiuntivi"
                String commandPart = parts[0];

                if (parts.length > 1) {
                    lastPayload = parts[1]; // Salva il payload per ulteriore elaborazione
                    System.out.println("Payload received: " + lastPayload);
                } else {
                    lastPayload = null;
                }

                return CommandResponse.valueOf(commandPart.trim());
            } else {
                System.out.println("Error: Server sent an empty response.");
                return CommandResponse.FAILURE;
            }
        } catch (SocketTimeoutException e) {
            System.out.println("Error: Server did not respond within the timeout period.");
            return CommandResponse.FAILURE;
        } catch (IOException e) {
            System.out.println("Error: I/O exception while receiving message: " + e.getMessage());
            return CommandResponse.FAILURE;
        }
    }

    public String getLastPayload() {
        return lastPayload;
    }


    public boolean sendEmail(String sender, List<String> receivers, String subject, String content) {
        if (!connectToServer()) {
            System.out.println("Error: Not connected to the server");
            return false;
        }

        try {
            // Concatena i destinatari separati da virgole
            String receiverList = String.join(",", receivers);

            // Formatta i dati dell'email nel formato: "sender|receiver1,receiver2|subject|content"
            String emailData = sender + "|" + receiverList + "|" + subject + "|" + content;

            // Invia il comando SEND_EMAIL al server
            boolean success = sendMessage(CommandRequest.SEND_EMAIL, emailData);
            if (success) {
                // Aspetta la risposta del server
                CommandResponse response = receiveMessage();
                if (response == CommandResponse.SUCCESS) {
                    System.out.println("Email sent successfully to: " + receivers);
                    return true;
                } else {
                    System.out.println("Server failed to send email. Response: " + response);
                    return false;
                }
            } else {
                System.out.println("Failed to send email to: " + receivers);
                return false;
            }
        } catch (Exception e) {
            System.out.println("Error sending email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

}
