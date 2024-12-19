package mailservice.clientside.Model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import mailservice.clientside.Configuration.*;

import static mailservice.clientside.Configuration.CommandRequest.*;


public class ClientModel {

    private String userLogged;
    private String serverHost;
    private int serverPort;
    private int fetchPeriod;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private static final int SOCKET_TIMEOUT = 8000; // Timeout di 8 secondi

    private static final int threadsNumber = 5;
    private ExecutorService operationPool;


    private ClientModel() {
        ConfigManager configManager = ConfigManager.getInstance();

        try {
            userLogged = configManager.readProperty("Client.Mail");
            serverHost = configManager.readProperty("Client.ServerHost");
            serverPort = Integer.parseInt(configManager.readProperty("Client.ServerPort"));
            fetchPeriod = Integer.parseInt(configManager.readProperty("Client.Fetch"));
            operationPool = Executors.newFixedThreadPool(threadsNumber);
        } catch (IllegalArgumentException e){
            System.err.println("[ERROR] Error in user.properties file");
        }
    }



    public static ClientModel getInstance() {
        return new ClientModel();
    }


    public String getUserEmail() {
        return userLogged;
    }


    public void logout() {
        userLogged = null;
        operationPool.shutdown();
    }


    public boolean connectToServer() {
        try {
            if (socket == null || socket.isClosed()) {
                socket = new Socket();
                socket.connect(new InetSocketAddress(serverHost, serverPort), SOCKET_TIMEOUT);
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


    public boolean sendCMD(CommandRequest command) {
        if (!connectToServer()) {
            System.err.println("[ERROR] Unable to connect to server.");
            return false;
        }
        Request request = new Request(userLogged, command, null);
        try {
            out.writeObject(request);
            out.flush();
            Thread.sleep(250);
            CommandResponse response = receiveMessage(); // Legge la risposta

            if (response != null && response.equals("SUCCESS")) {
                System.out.println("[INFO] Command executed successfully: " + response);
                return true;
            } else {
                System.err.println("[ERROR] Response is NULL or not SUCCESS: " + response);
                return false;
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to send message: " + e.getMessage());
            return false;
        } finally {
            disconnectFromServer();
        }
    }


    public CommandResponse receiveMessage() {
        if (socket == null || socket.isClosed()) {
            System.err.println("[ERROR] Connection not established. Cannot receive messages.");
            return null;
        }
        try {
            Object response = in.readObject(); // Legge un oggetto dallo stream
            if (response instanceof CommandResponse) {
                CommandResponse cmdResponse = (CommandResponse) response;
                System.out.println("[DEBUG] Raw server response: " + cmdResponse);
                return cmdResponse;
            } else {
                System.err.println("[ERROR] Unexpected response type: " + response.getClass());
                return null;
            }
        } catch (IOException e) {
            System.err.println("[ERROR] Exception while reading server response: " + e.getMessage());
            return null;
        }
        catch (ClassNotFoundException e) {
            System.err.println("[ERROR] Class not found while reading server response: " + e.getMessage());
            return null;
        }

    }


    public boolean sendEmail(List<String> receivers, String subject, String content) {
        ConfigManager configManager = ConfigManager.getInstance();
        if (receivers.stream().anyMatch(receiver -> !configManager.validateEmail(receiver))) {
            System.err.println("[ERROR] One or more receiver emails are invalid.");
            return false;
        }

        if (!connectToServer()) {
            System.err.println("[ERROR] Unable to connect to server.");
            return false;
        }
        try {
            Email emailData = new Email(userLogged, receivers, subject, content);
            Request request = new Request(userLogged, SEND_EMAIL, emailData);
            out.writeObject(request);
            out.flush();
            Thread.sleep(250);
            CommandResponse response = receiveMessage();
            if (response != null && response.equals("SUCCESS")) {
                System.out.println("[INFO] Mail sent successfully: " + response);
                return true;
            } else if(response != null && response.equals("ILLEGAL_PARAMS")) {
                System.err.println("[ERROR] Receivers does not exist in the server " + response);
                return false;
            }
            else {
                System.err.println("[ERROR] Response is NULL or not SUCCESS: " + response);
                return false;
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to send mail: " + e.getMessage());
            return false;
        } finally {
            disconnectFromServer();
        }
    }


    public List<Email> fetchEmails() {
        List<Email> emails = Collections.emptyList(); // Lista vuota di default

        try {
            if (!connectToServer()) {
                System.err.println("[ERROR] Unable to connect to server.");
                return emails; // Ritorna subito la lista vuota se non riesce a connettersi
            }

            out.writeObject(new Request(userLogged, FETCH_EMAIL, null));
            out.flush();
            Thread.sleep(250);
            Object response = in.readObject(); // Legge direttamente la risposta dal server

            if (response instanceof List) {
                emails = (List<Email>) response; // Cast sicuro, controllato dal tipo
            } else {
                System.err.println("[ERROR] Invalid response type received.");
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[ERROR] Error fetching emails: " + e.getMessage());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            disconnectFromServer();
        }

        return emails;
    }

}

