package mailservice.clientside.Network;

import mailservice.clientside.Configuration.CommandRequest;
import mailservice.clientside.Configuration.CommandResponse;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.List;

public class NetworkManager {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private static final int SOCKET_TIMEOUT = 30000; // Timeout di 30 secondi
    private static NetworkManager instance;

    private String lastPayload; // Variabile per memorizzare l'ultimo payload ricevuto

    private NetworkManager() {}

    public static NetworkManager getInstance() {
        if (instance == null) {
            instance = new NetworkManager();
        }
        return instance;
    }

    public boolean connectToServer() {
        if (socket != null && !socket.isClosed()) {
            System.out.println("[DEBUG] Already connected to server.");
            return true;
        }

        // Pulizia dei vecchi dati o variabili di stato
        clearSessionData();
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress("127.0.0.1", 42069), SOCKET_TIMEOUT);
            socket.setSoTimeout(SOCKET_TIMEOUT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("[INFO] Connected to server.");
            return true;
        } catch (IOException e) {
            System.out.println("[ERROR] Unable to connect to server: " + e.getMessage());
            return false;
        }
    }

    // Metodo per resettare i dati della sessione
    public void clearSessionData() {
        lastPayload = null;  // Esempio: azzera i dati ricevuti
        System.out.println("[DEBUG] Cleared session data.");
    }

    public synchronized void disconnectFromServer() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            System.out.println("[INFO] Disconnected from server.");
        } catch (IOException e) {
            System.out.println("[ERROR] Error disconnecting: " + e.getMessage());
        } finally {
            socket = null;
            out = null;
            in = null;
        }
    }

    public boolean sendMessage(CommandRequest command, String data) {
        if (!connectToServer()) {
            System.out.println("[ERROR] Not connected to the server.");
            return false;
        }
        if (out == null) {
            System.out.println("[ERROR] Output stream is null. Cannot send message.");
            return false;
        }

        try {
            out.write(command.name() + "|" + data + "\n");
            out.flush();
            return true;
        } catch (Exception e) {
            System.out.println("[ERROR] Exception while sending message: " + e.getMessage());
            return false;
        }
    }

    public CommandResponse receiveMessage() {
        try {
            if (in == null) {
                System.out.println("[ERROR] Input stream is null.");
                return CommandResponse.FAILURE;
            }

            String response = in.readLine();
            if (response == null) {
                System.out.println("[ERROR] Empty response from server.");
                return CommandResponse.FAILURE;
            }
            System.out.println("[DEBUG] Response received: " + response);

            String[] parts = response.split("\\|", 2);
            lastPayload = (parts.length > 1) ? parts[1] : null; // Salva il payload se presente
            return CommandResponse.valueOf(parts[0].trim());
        } catch (IOException e) {
            System.out.println("[ERROR] Error reading response: " + e.getMessage());
            return CommandResponse.FAILURE;
        }
    }

    // Metodo per ottenere l'ultimo payload
    public String getLastPayload() {
        return lastPayload;
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected();
    }

    // Metodo per eseguire l'escaping del contenuto HTML
    private String escapeHtml(String content) {
        return content.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    public boolean sendEmail(String sender, List<String> receivers, String subject, String content) {
        if (!connectToServer()) {
            System.out.println("[ERROR] Not connected to the server. Cannot send email.");
            return false;
        }

        try {
            // Concatena i destinatari separati da virgole
            String receiverList = String.join(",", receivers);

            // Esegui l'escaping del contenuto HTML
            String escapedContent = escapeHtml(content);

            // Formatta i dati dell'email
            String emailData = sender + "|" + receiverList + "|" + subject + "|" + escapedContent;

            // Log dettagliato per il debug
            System.out.println("[DEBUG] Sending email data: " + emailData);

            // Invia il comando SEND_EMAIL al server
            boolean success = sendMessage(CommandRequest.SEND_EMAIL, emailData);

            if (success) {
                // Attendi la risposta dal server
                CommandResponse response = receiveMessage();

                if (response == CommandResponse.SUCCESS) {
                    System.out.println("[INFO] Email sent successfully.");
                    return true;
                } else {
                    System.out.println("[ERROR] Server failed to send email. Response: " + response);
                    return false;
                }
            } else {
                System.out.println("[ERROR] Failed to send email to server.");
                return false;
            }
        } catch (Exception e) {
            System.out.println("[ERROR] Exception while sending email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
