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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import mailservice.clientside.Configuration.*;
import mailservice.clientside.Controller.MainController;
import mailservice.shared.*;
import mailservice.shared.enums.*;

import static mailservice.shared.Email.generateEmptyEmail;
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

    private static final int threadsNumber = 2;
    private ScheduledExecutorService operationPool;
    private ObservableList<Email> emailList;
    private static ClientModel instance;


    private ClientModel() {
        ConfigManager configManager = ConfigManager.getInstance();

        try {
            userLogged = configManager.readProperty("Client.Mail");
            serverHost = configManager.readProperty("Client.ServerHost");
            serverPort = Integer.parseInt(configManager.readProperty("Client.ServerPort"));
            fetchPeriod = Integer.parseInt(configManager.readProperty("Client.Fetch"));
            operationPool = Executors.newScheduledThreadPool(threadsNumber);
            emailList = FXCollections.observableArrayList();
        } catch (IllegalArgumentException e){
            System.err.println("[ERROR] Error in user.properties file");
        }
    }



    public static synchronized ClientModel getInstance() {
        if (instance == null || !instance.wrapLoginCheck()) {
            instance = new ClientModel();
        }
        return instance;
    }


    public String getUserEmail() {
        return userLogged;
    }

    public ObservableList<Email> getEmailList() {
        return emailList;
    }


    public void logout() {
        userLogged = null;
        if (operationPool != null) {
            operationPool.shutdown();
            try {
                if (!operationPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    System.err.println("[ERROR] Forcefully shutting down operation pool...");
                    operationPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                System.err.println("[ERROR] Interrupted during pool termination.");
                operationPool.shutdownNow();
            }
        }
        disconnectFromServer();
        System.out.println("[INFO] Client Process Terminated Successfully.");
    }


    private boolean connectToServer() {
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


    private void disconnectFromServer() {
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
        boolean result = sendCMD(LOGIN_CHECK, null);
        if(!result){
            instance = null;
        }
        return result;
    }


    private CommandResponse receiveMessage() {
        //return CommandResponse.SUCCESS;
        if (socket == null || socket.isClosed()) {
            System.err.println("[ERROR] Connection not established. Cannot receive messages.");
            return null;
        }
        try {
            Response cmdResponse;
            Object inResponse = in.readObject(); // Legge un oggetto dallo stream
            if (inResponse instanceof Response) {
                cmdResponse = (Response) inResponse;
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


    private void fetchEmails(boolean fullForceFetch) {
        List<Email> emails = new ArrayList<>(); // Lista vuota di default
        try {
            if (!connectToServer()) {
                System.err.println("[ERROR] Unable to connect to server.");
                return; // Ritorna subito la lista vuota se non riesce a connettersi
            }
            if(fullForceFetch){
                out.writeObject(new Request(userLogged, FETCH_EMAIL, generateEmptyEmail()));
            }
            else {
                out.writeObject(new Request(userLogged, FETCH_EMAIL, null));
            }
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

        List<Email> finalEmails = emails;
        Platform.runLater(() -> {
            emailList.setAll(finalEmails); // Aggiorna la lista osservabile
        });
    }

    public void startPeriodicFetch() {
        fetchEmails(true);
        operationPool.scheduleAtFixedRate(() -> fetchEmails(false), 5, fetchPeriod, TimeUnit.SECONDS);
    }

}

