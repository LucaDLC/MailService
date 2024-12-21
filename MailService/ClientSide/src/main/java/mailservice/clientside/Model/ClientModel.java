package mailservice.clientside.Model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.application.Platform;
import mailservice.clientside.Configuration.*;
import mailservice.clientside.Controller.MainController;
import mailservice.shared.*;
import mailservice.shared.enums.*;

import static mailservice.shared.enums.CommandRequest.*;
import static mailservice.shared.enums.CommandResponse.*;



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
    private static ClientModel instance;

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
        if (instance == null) {
            synchronized (ClientModel.class) {
                if (instance == null) {
                    try {
                        instance = new ClientModel();
                        System.out.println("[INFO] ClientModel instance created.");
                    } catch (Exception e) {
                        System.err.println("[ERROR] Failed to initialize ClientModel: " + e.getMessage());
                        instance = null;
                    }
                }
            }
        }
        return instance;
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
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());
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
        if (socket != null && !socket.isClosed()) {
            try {
                in.close();
                out.close();
                socket.close();
                System.out.println("[INFO] Disconnected from server.");
            } catch (IOException e) {
                System.err.println("[ERROR] Error disconnecting: " + e.getMessage());
            }
        }

    }


    private boolean sendCMD(CommandRequest command, Email dataMail) {
        if (!connectToServer()) {
            System.err.println("[ERROR] Unable to connect to server.");
            return false;
        }
        Request request = new Request(userLogged, command, dataMail);
        try {
            out.writeObject(request);
            out.flush();
            Thread.sleep(250);
            CommandResponse response = receiveMessage(); // Legge la risposta

            if (response != null && response.equals(CommandResponse.SUCCESS)) {
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


    public boolean wrapDeleteEmail(Email delmail){
        return sendCMD(DELETE_EMAIL, delmail);
    }


    public boolean wrapLoginCheck(){
        return sendCMD(LOGIN_CHECK, null);
    }


    private CommandResponse receiveMessage() {
        //return CommandResponse.SUCCESS;
        if (socket == null || socket.isClosed()) {
            System.err.println("[ERROR] Connection not established. Cannot receive messages.");
            return null;
        }
        try {
            Object inResponse = in.readObject(); // Legge un oggetto dallo stream
            if (inResponse instanceof Response) {
                Response cmdResponse = (Response) inResponse;
                System.out.println("[DEBUG] Raw server response: " + cmdResponse);

                return cmdResponse.responseName();
            } else {
                System.err.println("[ERROR] Unexpected response type: " + inResponse.getClass());
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
        if (receivers == null || receivers.isEmpty()) {
            System.err.println("[ERROR] No recipients provided for the email.");
            return false;
        }

        for (String recipientSplit : receivers) {
            String trimmedRecipient = recipientSplit.trim();
            if (!ConfigManager.getInstance().validateEmail(trimmedRecipient)) {
                System.err.println("[ERROR] Invalid email insert during Compose phase: " + trimmedRecipient);
                return false;
            }
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
            if (response != null && response.equals(CommandResponse.SUCCESS)) {
                System.out.println("[INFO] Mail sent successfully: " + response);
                Platform.runLater(() -> {
                    MainController mainController = new MainController();
                    mainController.refreshEmails(); // Refresh safely on the JavaFX thread
                });
                return true;
            } else if(response != null && response.equals(CommandResponse.ILLEGAL_PARAMS)) {
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
        List<Email> emails = new ArrayList<>(); // Lista vuota di default

        try {
            if (!connectToServer()) {
                System.err.println("[ERROR] Unable to connect to server.");
                return emails; // Ritorna subito la lista vuota se non riesce a connettersi
            }

            out.writeObject(new Request(userLogged, FETCH_EMAIL, null));
            out.flush();

            Thread.sleep(250);

            Response response = (Response) in.readObject(); // Expecting a Response object
            if (response != null && response.responseName() == CommandResponse.SUCCESS) {
                emails = response.args();  // Accessing the list directly
                if (emails == null) {
                    emails = new ArrayList<>();  // Ensure non-null list
                }
            } else {
                System.err.println("[ERROR] Invalid or empty response received.");
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

