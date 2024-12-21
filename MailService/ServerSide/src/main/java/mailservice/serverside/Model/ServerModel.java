package mailservice.serverside.Model;

import mailservice.serverside.Configuration.*;
import mailservice.serverside.Controller.ServerController;
import mailservice.serverside.Log.LogType;

import java.io.*;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
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

        // Inizializza il thread pool
        serverThreads = Executors.newFixedThreadPool(threadsNumber);

        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                controller.log(LogType.INFO, "Server started on port " + port);

                while (running) {
                    Socket clientSocket = serverSocket.accept();
                    controller.log(LogType.INFO, "Client connected from: " + clientSocket.getInetAddress());

                    // Invio di un task Runnable al thread pool
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
                controller.log(LogType.INFO, "Server socket closed.");
            }

            if (serverThreads != null && !serverThreads.isShutdown()) {
                serverThreads.shutdown(); // Avvia lo shutdown dei thread esistenti
                if (!serverThreads.awaitTermination(30, TimeUnit.SECONDS)) {
                    controller.log(LogType.ERROR, "Forcing thread pool shutdown...");
                    serverThreads.shutdownNow(); // Forza lo shutdown
                }
            }
        } catch (IOException e) {
            controller.log(LogType.ERROR, "Error closing server socket: " + e.getMessage());
        } catch (InterruptedException e) {
            controller.log(LogType.ERROR, "Thread pool shutdown interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        } finally {
            serverThreads = null;
            serverSocket = null;
            controller.log(LogType.INFO, "Server shutdown complete.");
        }
    }

    private void handleClient(Socket clientSocket) {
        try (
                ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream())
        ) {
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
        } catch (IOException e) {
            controller.log(LogType.ERROR, "Client disconnected or IO error: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            controller.log(LogType.ERROR, "Invalid command received: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                controller.log(LogType.ERROR, "Error closing client socket: " + e.getMessage());
            }
        }
    }


    private synchronized void handleSendEmail(String emailData, Email mail, ObjectOutputStream out) throws IOException {
        controller.log(LogType.SYSTEM, "Processing SEND_EMAIL command...");
        if(!(emailData.equals(mail.getSender()))){
            controller.log(LogType.ERROR,"Not syncronized email sender with session.");
            return;
        }
        if(!areValidEmails(mail.getReceivers())){
            controller.log(LogType.ERROR, "Invalid receiver email.");
            return;
        }

        controller.log(LogType.SYSTEM, "Flushing response to client...");
        sendCMDResponse(out, SUCCESS);
        controller.log(LogType.SYSTEM, "Response sent to client.");
    }

    private synchronized void handleFetchEmail(String userEmail, ObjectOutputStream out) throws IOException {
        controller.log(LogType.SYSTEM, "Fetching emails for user: " + userEmail);

        File userFolder = createUserFolder(userEmail);
        File[] emailFiles = userFolder.listFiles((dir, name) -> name.startsWith("email_"));

        if (emailFiles == null || emailFiles.length == 0) {
            controller.log(LogType.SYSTEM, "No emails found for user: " + userEmail);
            sendCMDResponse(out, SUCCESS);
            return;
        }

        StringBuilder allEmails = new StringBuilder();
        for (File emailFile : emailFiles) {
            try (BufferedReader reader = new BufferedReader(new FileReader(emailFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    allEmails.append(line).append("\n");
                }
            } catch (IOException e) {
                controller.log(LogType.ERROR, "Error reading email file: " + emailFile.getName() + " - " + e.getMessage());
            }
        }

        controller.log(LogType.SYSTEM, "Emails fetched successfully for user: " + userEmail);

        sendCMDResponse(out, SUCCESS);
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

    private synchronized void saveEmailToFolders(String username, String emailContent) {
        File userFolder = createUserFolder(username);
        String emailFileName = "email_" + System.currentTimeMillis() + ".txt";

        File emailFile = new File(userFolder, emailFileName);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(emailFile))) {
            writer.write(emailContent);
        } catch (IOException e) {
            controller.log(LogType.ERROR, "Failed to save email: " + e.getMessage());
        }
    }

    private synchronized File getUserEmailFile(String userEmail) {
        File userFolder = createUserFolder(userEmail);
        return new File(userFolder, "sent_emails.txt");
    }

    private synchronized File createUserFolder(String username) {
        File folder = new File("UserFolders", username);
        if (!folder.exists()) folder.mkdirs();
        return folder;
    }

    private synchronized boolean checkFolderName(String userEmail) {
        String baseDirectory = new File("").getAbsolutePath() + File.separator + "ServerSide" + File.separator + "src" + File.separator + "main" + File.separator + "BigData";
        File userFolder = new File(baseDirectory, userEmail);
        // Restituisce true se la cartella esiste ed è una directory, altrimenti false
        return userFolder.exists() && userFolder.isDirectory();
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[a-zA-Z0-9._%+-]+@rama.it$");
    }

    private boolean areValidEmails(List<String> emails) {
        for (String email : emails) {
            if(!checkFolderName(email)) return false;
        }
        return true;
    }

    private boolean shouldDelete(String line, String[] emailsToDelete) {
        for (String email : emailsToDelete) {
            if (line.contains(email.trim())) return true;
        }
        return false;
    }

    public void sendCMDResponse(ObjectOutputStream out, CommandResponse cmdResponse) throws IOException {
        Response response = new Response(cmdResponse,null);
        out.writeObject(response);
        out.flush();
        controller.log(LogType.SYSTEM, "Response flushed to client: " + response.toString());
    }

    public void sendMail(ObjectOutputStream out, CommandResponse cmdResponse, List<Email> mail) throws IOException {
        Response response = new Response(cmdResponse,mail);
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
