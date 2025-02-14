package mailservice.clientside.Model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import mailservice.clientside.ClientApp;
import mailservice.clientside.Configuration.*;
import mailservice.shared.*;
import mailservice.shared.enums.*;

import static mailservice.shared.Email.generateEmptyEmail;
import static mailservice.shared.enums.CommandRequest.*;
import static mailservice.shared.enums.CommandResponse.*;
import static mailservice.shared.enums.LogType.*;


public class ClientModel {

    private String userLogged;
    private String serverHost;
    private int serverPort;
    private int fetchPeriod;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private static final int SOCKET_TIMEOUT = 3000; // Timeout di 3 secondi
    private final BooleanProperty isServerReachable = new SimpleBooleanProperty(false);

    private ObservableList<Email> emailList;
    private static ClientModel instance;


    private ClientModel() {
        ConfigManager configManager = ConfigManager.getInstance();

        try {
            serverHost = configManager.readProperty("Client.ServerHost");
            serverPort = Integer.parseInt(configManager.readProperty("Client.ServerPort"));
            fetchPeriod = Integer.parseInt(configManager.readProperty("Client.Fetch"));
            emailList = FXCollections.observableArrayList();
        } catch (IllegalArgumentException e){
            log(ERROR,"Error in user.properties file");
        }
    }



    public static synchronized ClientModel getInstance() {
        if (instance == null) {
            instance = new ClientModel();
        }
        return instance;
    }


    public String getUserEmail() {
        return userLogged;
    }


    public int getFetchPeriod() {
        return fetchPeriod;
    }


    public ObservableList<Email> getEmailList() {
        return emailList;
    }


    public BooleanProperty isServerReachable() {
        return isServerReachable;
    }


    public void logout () {
        ClientApp.stopPeriodicFetch();
        disconnectFromServer();
        userLogged = null;
        instance = null;
    }


    private synchronized boolean connectToServer() {
        try {
            if (socket == null || socket.isClosed()) {
                socket = new Socket();
                socket.connect(new InetSocketAddress(serverHost, serverPort), SOCKET_TIMEOUT);
                socket.setSoTimeout(SOCKET_TIMEOUT);
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());
                log(INFO,"Connected to server.");
                isServerReachable.set(true);
                return true;
            }
            else {
                log(INFO,"Already connected to server.");
                isServerReachable.set(true);
                return false;
            }

        } catch (IOException e) {
            log(ERROR,"Unable to connect to server: " + e.getMessage());
            isServerReachable.set(false);
            return false;
        }

    }


    private void disconnectFromServer() {
        if (socket != null && !socket.isClosed()) {
            try {
                in.close();
                out.close();
                socket.close();
                log(INFO,"Disconnected from server.");
            } catch (IOException e) {
                log(ERROR,"Error disconnecting: " + e.getMessage());
            }
        }

    }


    private boolean sendCMD(CommandRequest command, Email dataMail) {
        if (!connectToServer()) {
            log(ERROR,"Unable to connect to server.");
            return false;
        }
        Request request = new Request(userLogged, command, dataMail);
        try {
            log(SYSTEM,"Raw client request: " + request);
            out.writeObject(request);
            out.flush();
            CommandResponse response = receiveMessage(); // Legge la risposta

            if (response != null && response.equals(SUCCESS)) {
                log(INFO,"Command executed successfully: " + response);
                return true;
            } else {
                log(ERROR,"Response is NULL or not SUCCESS: " + response);
                return false;
            }
        } catch (Exception e) {
            log(ERROR,"Failed to send message: " + e.getMessage());
            return false;
        } finally {
            disconnectFromServer();
        }
    }


    public boolean wrapDeleteEmail(Email delmail){
        return sendCMD(DELETE_EMAIL, delmail);
    }


    public Map.Entry<Boolean, Boolean> wrapLoginCheck(String loginMail){
        if (userLogged == null){
            userLogged = loginMail;
        }
        boolean result = sendCMD(LOGIN_CHECK, null);
        if(!result){
            userLogged = null;
            instance = null;
        }
        else {
            ClientApp.startPeriodicFetch();
        }
        return new AbstractMap.SimpleEntry<>(isServerReachable.get(), result);
    }


    private CommandResponse receiveMessage() {
        //return SUCCESS;
        if (socket == null || socket.isClosed()) {
            log(ERROR,"Connection not established. Cannot receive messages.");
            return GENERIC_ERROR;
        }
        try {
            Response cmdResponse;
            Object inResponse = in.readObject(); // Legge un oggetto dallo stream
            if (inResponse instanceof Response) {
                cmdResponse = (Response) inResponse;
                log(SYSTEM," Raw server response: " + cmdResponse);

                return cmdResponse.responseName();
            } else {
                log(ERROR,"Unexpected response type: " + inResponse.getClass());
                return GENERIC_ERROR;
            }
        } catch (IOException e) {
            log(ERROR,"Exception while reading server response: " + e.getMessage());
            return GENERIC_ERROR;
        }
        catch (ClassNotFoundException e) {
            log(ERROR,"Class not found while reading server response: " + e.getMessage());
            return GENERIC_ERROR;
        }

    }


    public boolean sendEmail(List<String> receivers, String subject, String content) {
        if (receivers == null || receivers.isEmpty()) {
            log(ERROR,"No recipients provided for the email.");
            return false;
        }

        for (String recipientSplit : receivers) {
            if (!ConfigManager.getInstance().validateEmail(recipientSplit)) {
                log(ERROR,"Invalid email: " + recipientSplit);
                return false;
            }
        }

        if (!connectToServer()) {
            log(ERROR,"Unable to connect to server.");
            return false;
        }
        Email emailData = new Email(userLogged, receivers, subject, content);
        Request request = new Request(userLogged, SEND_EMAIL, emailData);
        try {
            log(SYSTEM,"Raw client request: " + request);
            out.writeObject(request);
            out.flush();
            CommandResponse response = receiveMessage();
            if (response != null && response.equals(SUCCESS)) {
                log(INFO,"Mail sent successfully: " + response);
                return true;
            } else if (response != null && response.equals(ILLEGAL_PARAMS)) {
                log(ERROR,"Receivers does not exist in the server " + response);
                return false;
            } else {
                log(ERROR,"Response is NULL or not SUCCESS: " + response);
                return false;
            }
        } catch (Exception e) {
            log(ERROR,"Failed to send mail: " + e.getMessage());
            return false;
        } finally {
            disconnectFromServer();
        }
    }


    public void fetchEmails(boolean fullForceFetch) {
        List<Email> emails = new ArrayList<>(); // Lista vuota di default
        try {
            if (!connectToServer()) {
                log(ERROR,"Unable to connect to server.");
                return; // Ritorna subito la lista vuota se non riesce a connettersi
            }
            if(fullForceFetch){
                Request request = new Request(userLogged, FETCH_EMAIL, generateEmptyEmail());
                log(SYSTEM,"Raw client request: " + request);
                out.writeObject(request);
            }
            else {
                Request request = new Request(userLogged, FETCH_EMAIL, null);
                log(SYSTEM,"Raw client request: " + request);
                out.writeObject(request);
            }
            out.flush();
            Response response = (Response) in.readObject(); // Expecting a Response object
            if (response != null && response.responseName() == SUCCESS) {
                emails = response.args();  // Accessing the list directly
                if (emails == null) {
                    emails = new ArrayList<>();  // Ensure non-null list
                }
            } else {
                log(ERROR,"Invalid or empty response received.");
            }
        } catch (IOException | ClassNotFoundException e) {
            log(ERROR,"Error fetching emails: " + e.getMessage());
        } finally {
            disconnectFromServer();
        }

        if (fullForceFetch) {
            List<Email> finalEmails = emails;
            Platform.runLater(() -> {
                emailList.setAll(finalEmails); // Aggiorna la lista osservabile
            });
        }
        else {
            List<Email> finalEmails = emails;
            Platform.runLater(() -> {
                emailList.addAll(finalEmails); // Aggiorna la lista osservabile
            });
        }

        Platform.runLater(() -> {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

            // Ordinamento in ordine decrescente
            Comparator<Email> comparator = Comparator.comparing(
                    (Email email) -> LocalDateTime.parse(email.getDate(), formatter)
            ).reversed();

            FXCollections.sort(emailList, comparator);
        });

    }

    public static void log(LogType type, String message){
        String formattedMessage = String.format("[%s] %s", type.name(), message);
        if(type.equals(LogType.ERROR)) {
            System.err.println(formattedMessage);
        }
        else{
        System.out.println(formattedMessage);
        }
    }

}

