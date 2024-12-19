package mailservice.clientside.Network;

import mailservice.clientside.Configuration.CommandRequest;
import mailservice.clientside.Configuration.ConfigManager;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

public class NetworkManager {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private static final int SOCKET_TIMEOUT = 30000; // Timeout di 5 secondi
    private static NetworkManager instance;

    private NetworkManager() {}

    public static NetworkManager getInstance() {
        if (instance == null) {
            instance = new NetworkManager();
        }
        return instance;
    }

    public boolean connectToServer() {
        try {
            if (socket == null || socket.isClosed()) {
                socket = new Socket();
                socket.connect(new InetSocketAddress(ConfigManager.getInstance().readProperty("Client.ServerHost"), Integer.parseInt(ConfigManager.getInstance().readProperty("Client.ServerPort"))), SOCKET_TIMEOUT);
                socket.setSoTimeout(SOCKET_TIMEOUT);
                in = new ObjectInputStream(socket.getInputStream());
                out = new ObjectOutputStream(socket.getOutputStream());
                System.out.println("[INFO] Connected to server.");
                return true;
            }
            else {
                System.out.println("[INFO] Already connected to server.");
                return true;
            }

        } catch (IOException e) {
            System.err.println("[ERROR] Unable to connect to server: " + e.getMessage());
            return false;
        }
    }

    public void disconnectFromServer() {
        try {
            if (socket != null && !socket.isClosed()) {
                in.close();
                out.close();
                socket.close();
            }
            System.out.println("[INFO] Disconnected from server.");
        } catch (IOException e) {
            System.err.println("[ERROR] Error disconnecting: " + e.getMessage());
        }
    }

    public boolean sendCMD(CommandRequest command, String data) {
        if (!connectToServer()) {
            System.err.println("[ERROR] Unable to connect to server.");
            return false;
        }
        try {
            out.println(command.name() + "|" + data);
            out.flush();
            String response = receiveMessage(); // Legge la risposta

            if (response != null && response.startsWith("SUCCESS")) {
                System.out.println("[INFO] Command executed successfully: " + response);
                return true;
            } else if (response == null) {
                System.err.println("[ERROR] Response is null. Timeout occurred.");
            } else {
                System.err.println("[ERROR] Command failed. Response: " + response);
            }
            return false;
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to send message: " + e.getMessage());
            return false;
        } finally {
            disconnectFromServer();
        }
    }

    public String receiveMessage() {
        if (socket == null || socket.isClosed()) {
            System.err.println("[ERROR] Connection not established. Cannot receive messages.");
            return null;
        }

        try {
            String response = in.readLine();
            System.out.println("[DEBUG] Raw server response: " + response);
            return response;
        } catch (IOException e) {
            System.err.println("[ERROR] Exception while reading server response: " + e.getMessage());
            return null;
        }
    }

    public boolean sendEmail(String sender, List<String> receivers, String subject, String content) {
        if (!connectToServer()) {
            System.err.println("[ERROR] Unable to connect to server.");
            return false;
        }
        try {
            String emailData = sender + "|" + String.join(",", receivers) + "|" + subject + "|" + content;
            out.println(CommandRequest.SEND_EMAIL.name() + "|" + emailData);
            out.flush();

            String response = receiveMessage(); // Legge la risposta del server
            if (response != null && response.startsWith("SUCCESS")) {
                System.out.println("[INFO] Email sent successfully.");
                return true;
            } else if (response == null) {
                System.err.println("[ERROR] No response received. Timeout occurred.");
            } else {
                System.err.println("[ERROR] Failed to send email. Server response: " + response);
            }
            return false;
        } catch (Exception e) {
            System.err.println("[ERROR] Exception while sending email: " + e.getMessage());
            return false;
        }
    }
}
