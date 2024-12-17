package mailservice.clientside.Network;

import mailservice.clientside.Configuration.CommandRequest;
import mailservice.clientside.Configuration.ConfigManager;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

public class NetworkManager {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private static final int SOCKET_TIMEOUT = 30000; // Timeout di 30 secondi
    private static NetworkManager instance;

    private NetworkManager() {}

    public static NetworkManager getInstance() {
        if (instance == null) {
            instance = new NetworkManager();
        }
        return instance;
    }

    public synchronized boolean connectToServer() {
        int retries = 3;
        while (retries > 0) {
            try {
                if (socket != null && !socket.isClosed()) {
                    System.out.println("[DEBUG] Already connected to server.");
                    return true;
                }

                socket = new Socket();
                socket.connect(new InetSocketAddress(ConfigManager.getInstance().readProperty("Client.ServerHost"),
                        Integer.parseInt(ConfigManager.getInstance().readProperty("Client.ServerPort"))), SOCKET_TIMEOUT);
                socket.setSoTimeout(SOCKET_TIMEOUT);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                System.out.println("[INFO] Connected to server.");
                return true;
            } catch (IOException e) {
                retries--;
                System.err.println("[ERROR] Retry connection (" + retries + " attempts left): " + e.getMessage());
            }
        }
        System.err.println("[ERROR] Unable to connect to server after retries.");
        return false;
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
        if (socket == null || socket.isClosed()) {
            System.err.println("[ERROR] Connection to server lost.");
            connectToServer();
        }
        try {
            out.write(command.name() + "|" + data + "\n");
            out.flush();
            Thread.sleep(2000);
            if (in != null && in.ready()) {
                String response = in.readLine();
                if ("true".equalsIgnoreCase(response.trim())) {
                    System.out.println("[INFO] Server response is positive.");
                    return true;
                }else {
                    System.err.println("[ERROR] Server response is negative.");
                    return false;
                }
            } else { // Server did not respond
                System.err.println("[ERROR] Server did not respond.");
                return false;
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to send message: " + e.getMessage());
            return false;
        }
        finally {
            disconnectFromServer();
        }
    }

    public String receiveMessage() {
        if (in == null) {
            System.err.println("[ERROR] BufferedReader is null. Connection might not be established.");
            return null;
        }
        else {
            try {
                return in.readLine();
            } catch (IOException e) {
                System.out.println("[ERROR] Error reading response: " + e.getMessage());
                return null;
            }
        }
    }

    public boolean sendEmail(String sender, List<String> receivers, String subject, String content) {
        if (!connectToServer()) {
            System.out.println("[ERROR] Not connected to the server. Cannot send email.");
            return false;
        }

        try {
            String receiverList = String.join(",", receivers);
            String emailData = sender + "|" + receiverList + "|" + subject + "|" + content;

            System.out.println("[DEBUG] Sending email data: " + emailData);
            if (sendMessage(CommandRequest.SEND_EMAIL, emailData)) {
                String response = receiveMessage();
                if (response != null && response.trim().startsWith("SUCCESS")) {
                    System.out.println("[INFO] Email sent successfully.");
                    return true;
                } else {
                    System.out.println("[ERROR] Failed to send email. Server response: " + response);
                    return false;
                }
            } else {
                System.out.println("[ERROR] Failed to send email command to server.");
                return false;
            }
        } catch (Exception e) {
            System.out.println("[ERROR] Exception while sending email: " + e.getMessage());
            return false;
        }
    }
}
