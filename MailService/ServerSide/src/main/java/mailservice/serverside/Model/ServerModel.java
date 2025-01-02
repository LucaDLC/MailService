package mailservice.serverside.Model;

import mailservice.serverside.Configuration.*;
import mailservice.serverside.Controller.ServerController;
import mailservice.serverside.Log.LogType;

import java.io.*;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import mailservice.shared.*;
import mailservice.shared.enums.*;

import static mailservice.shared.enums.CommandResponse.*;
import static mailservice.shared.enums.CommandRequest.*;

public class ServerModel {
    private final int port;
    private final int timeout;

    private ServerSocket serverSocket;
    private volatile boolean running;

    private ExecutorService serverThreads;
    private final int threadsNumber;

    private ServerController controller;

    public ServerModel(ServerController serverController) {
        ConfigManager configManager = ConfigManager.getInstance();
        this.controller = serverController;
        this.port = Integer.parseInt(configManager.readProperty("Server.Port"));
        this.timeout = Integer.parseInt(configManager.readProperty("Server.Timeout"));
        this.threadsNumber = Integer.parseInt(configManager.readProperty("Server.Threads"));
    }

    public void startServer() {
        if (running) {
            controller.log(LogType.INFO, "Server is already running on port " + port);
            return;
        }
        running = true;
        serverThreads = Executors.newFixedThreadPool(threadsNumber);

        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                controller.log(LogType.INFO, "Server started on port " + port);
                while (running) {
                    Socket clientSocket = serverSocket.accept();
                    controller.log(LogType.INFO, "Client connected from: " + clientSocket.getInetAddress());
                    serverThreads.submit(() -> handleClient(clientSocket));
                }
            } catch (BindException e) {
                controller.showErrorAlert("Port " + port + " is already in use.");
            } catch (IOException e) {
                controller.log(LogType.ERROR, "Server error: " + e.getMessage());
            } finally {
                stopServer();
            }
        }).start();
    }

    public void stopServer() {
        if (!running) {
            controller.log(LogType.INFO, "Server is not running.");
            return;
        }
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            if (serverThreads != null && !serverThreads.isShutdown()) {
                serverThreads.shutdown();
                if (!serverThreads.awaitTermination(30, TimeUnit.SECONDS)) {
                    serverThreads.shutdownNow();
                }
            }
            controller.log(LogType.INFO, "Server shutdown complete.");
        } catch (IOException | InterruptedException e) {
            controller.log(LogType.ERROR, "Error while shutting down server: " + e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void handleClient(Socket clientSocket) {
        try (ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream())) {
            Request clientMessage;
            while ((clientMessage = (Request) in.readObject()) != null) {
                controller.log(LogType.SYSTEM, "Received message: " + clientMessage);
                switch (clientMessage.cmdName()) {
                    case LOGIN_CHECK -> handleLoginCheck(clientMessage.logged(), out);
                    case FETCH_EMAIL -> handleFetchEmail(clientMessage.logged(), out);
                    case SEND_EMAIL -> handleSendEmail(clientMessage.logged(), clientMessage.mail(), out);
                    case DELETE_EMAIL -> handleDeleteEmail(clientMessage.logged(), clientMessage.mail(), out);
                    default -> sendCMDResponse(out, GENERIC_ERROR);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            controller.log(LogType.ERROR, "Client disconnected or IO error: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                controller.log(LogType.ERROR, "Error closing client socket: " + e.getMessage());
            }
        }
    }

    private void handleSendEmail(String userEmail, Email mail, ObjectOutputStream out) throws IOException {
        if (!userEmail.equals(mail.getSender())) {
            sendCMDResponse(out, ILLEGAL_PARAMS);
            return;
        }
        if (!areValidEmails(mail.getReceivers())) {
            sendCMDResponse(out, ILLEGAL_PARAMS);
            return;
        }
        saveEmailToFile(mail);
        sendMail(out, SUCCESS, List.of(mail));
    }

    private void handleFetchEmail(String userEmail, ObjectOutputStream out) throws IOException {
        List<Email> emails = fetchEmails(userEmail);
        if (emails.isEmpty()) {
            sendCMDResponse(out, SUCCESS);
        } else {
            sendMail(out, SUCCESS, emails);
        }
    }

    private List<Email> fetchEmails(String userEmail) {
        File userFolder = createUserFolder(userEmail);
        File[] emailFiles = userFolder.listFiles((dir, name) -> name.startsWith("email_"));
        List<Email> emails = new ArrayList<>();
        for (File emailFile : emailFiles) {
            Email email = readEmailFromFile(emailFile);
            if (email != null) {
                emails.add(email);
            }
        }
        return emails;
    }

    private Email readEmailFromFile(File emailFile) {
        try (BufferedReader reader = new BufferedReader(new FileReader(emailFile))) {
            String line;
            String sender = "", subject = "", text = "";
            List<String> receivers = new ArrayList<>();
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Sender:")) {
                    sender = line.substring(7);
                } else if (line.startsWith("Receivers:")) {
                    receivers = Arrays.asList(line.substring(10).split(","));
                } else if (line.startsWith("Subject:")) {
                    subject = line.substring(8);
                } else if (line.startsWith("Text:")) {
                    text = line.substring(5);
                }
            }
            return new Email(sender, receivers, subject, text);
        } catch (IOException e) {
            controller.log(LogType.ERROR, "Failed to read email from text file: " + e.getMessage());
            return null;
        }
    }

    private void saveEmailToFile(Email email) {
        File userFolder = createUserFolder(email.getSender());
        String emailFileName = "email_" + email.getId() + ".txt";
        File emailFile = new File(userFolder, emailFileName);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(emailFile))) {
            writer.write(emailToString(email));
            controller.log(LogType.SYSTEM, "Email saved as text successfully: " + emailFileName);
        } catch (IOException e) {
            controller.log(LogType.ERROR, "Failed to save email to text file: " + e.getMessage());
        }
    }

    private String emailToString(Email email) {
        return "ID:" + email.getId() + "\n" +
                "Sender:" + email.getSender() + "\n" +
                "Receivers:" + String.join(",", email.getReceivers()) + "\n" +
                "Subject:" + email.getSubject() + "\n" +
                "Text:" + email.getText() + "\n" +
                "Date:" + email.getDate().toString() + "\n";
    }

    private synchronized void handleLoginCheck(String email, ObjectOutputStream out) throws IOException {
        if (!isValidEmail(email)) {
            sendCMDResponse(out, ILLEGAL_PARAMS);
            return;
        }

        createUserFolder(email);
        sendCMDResponse(out, SUCCESS);
        controller.log(LogType.SYSTEM, "Response flushed to client: Login successful.");
    }

    private synchronized void handleDeleteEmail(String requestData, Email mail, ObjectOutputStream out) throws IOException {
        String[] parts = requestData.split("\\|", 2);
        if (parts.length < 2) {
            sendCMDResponse(out, ILLEGAL_PARAMS);
            return;
        }

        String userEmail = parts[0];
        String[] emailsToDelete = parts[1].split(",");
        File emailFile = getUserEmailFile(userEmail);

        if (!emailFile.exists()) {
            sendCMDResponse(out, ILLEGAL_PARAMS);
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(emailFile));
             BufferedWriter writer = new BufferedWriter(new FileWriter(emailFile + ".tmp"))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (!shouldDelete(line, emailsToDelete)) {
                    writer.write(line + "\n");
                }
            }
        }

        emailFile.delete();
        new File(emailFile + ".tmp").renameTo(emailFile);
        sendCMDResponse(out, SUCCESS);
    }

    private synchronized File getUserEmailFile(String userEmail) {
        File userFolder = createUserFolder(userEmail);
        return new File(userFolder, "sent_emails.txt");
    }

    private File createUserFolder(String username) {
        String baseDirectory = new File("").getAbsolutePath() + File.separator + "ServerSide" + File.separator + "src" + File.separator + "main" + File.separator + "BigData";
        File folder = new File(baseDirectory, username);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        return folder;
    }

    private synchronized boolean checkFolderName(String userEmail) {
        String baseDirectory = new File("").getAbsolutePath() + File.separator + "ServerSide" + File.separator + "src" + File.separator + "main" + File.separator + "BigData";
        File userFolder = new File(baseDirectory, userEmail);
        return userFolder.exists() && userFolder.isDirectory();
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[a-zA-Z0-9._%+-]+@rama.it$");
    }

    private boolean areValidEmails(List<String> emails) {
        return emails.stream().allMatch(this::isValidEmail);
    }

    private boolean shouldDelete(String line, String[] emailsToDelete) {
        for (String email : emailsToDelete) {
            if (line.contains(email.trim())) return true;
        }
        return false;
    }

    public void sendCMDResponse(ObjectOutputStream out, CommandResponse cmdResponse) throws IOException {
        Response response = new Response(cmdResponse, null);
        out.writeObject(response);
        out.flush();
        controller.log(LogType.SYSTEM, "Response flushed to client: " + response.toString());
    }

    public void sendMail(ObjectOutputStream out, CommandResponse cmdResponse, List<Email> mail) throws IOException {
        Response response = new Response(cmdResponse, mail);
        out.writeObject(response);
        out.flush();
        controller.log(LogType.SYSTEM, "Response flushed to client: " + response.toString());
    }

    public int getPort() {
        return port;
    }

    public boolean isRunning() {
        return running;
    }
}
