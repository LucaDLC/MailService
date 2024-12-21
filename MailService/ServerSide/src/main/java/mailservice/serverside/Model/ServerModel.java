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

        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                controller.log(LogType.INFO, "Server started on port " + port);

                while (running) {
                    Socket clientSocket = serverSocket.accept();
                    controller.log(LogType.INFO, "Client connected from: " + clientSocket.getInetAddress());
                    serverThreads.submit(handleClient(clientSocket));
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
        if (!running)
        {
            controller.log(LogType.INFO, "Server stopped on port " + port);
            return;
        }
        running = false;

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                controller.log(LogType.INFO, "Server stopped on port " + port);
            }
            if (serverThreads != null && !serverThreads.isShutdown()) {
                serverThreads.shutdown(); // Avvia lo shutdown dei thread esistenti
                if (!serverThreads.awaitTermination(30, TimeUnit.SECONDS)) {
                    controller.log(LogType.ERROR, "Forcing thread pool shutdown...");
                    serverThreads.shutdownNow(); // Forza lo shutdown se i thread non terminano in tempo
                }
            }
        } catch (IOException e) {
            controller.log(LogType.ERROR, "Error stopping server: " + e.getMessage());
        } catch (InterruptedException e) {
            controller.log(LogType.ERROR, "Error stopping server threads: " + e.getMessage());
            Thread.currentThread().interrupt();
        } finally {
            // Cleanup finale
            serverSocket = null;
            controller.log(LogType.INFO, "Socket inhibited.");
        }
    }

    private Runnable handleClient(Socket clientSocket) {
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        try {
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            in = new ObjectInputStream(clientSocket.getInputStream());
            Request clientMessage;
            while ((clientMessage = (Request) in.readObject()) != null && clientMessage instanceof Request) {
                controller.log(LogType.SYSTEM, "Received message: " + clientMessage);

                switch (clientMessage.cmdName()) {
                    case LOGIN_CHECK -> handleLoginCheck(clientMessage.logged(), out);
                    case FETCH_EMAIL -> handleFetchEmail(clientMessage.logged(), out);
                    case SEND_EMAIL -> handleSendEmail(clientMessage.logged(),clientMessage.mail(), out);
                    case DELETE_EMAIL -> handleDeleteEmail(clientMessage.logged(),clientMessage.mail(), out);
                    default -> sendErrorResponse(out, "Unknown command.");
                }
            }
        } catch (IOException e) {
            controller.log(LogType.ERROR, "Client error: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            controller.log(LogType.ERROR, "Command Request error: " + e.getMessage());
        }
        Response response = new Response(CommandResponse.GENERIC_ERROR,null);
        try {
            out.writeObject(response);
            out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }

        return null;
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
        sendSuccessResponse(out, "Email sent successfully.");
        controller.log(LogType.SYSTEM, "Response sent to client.");
    }

    private synchronized void handleFetchEmail(String userEmail, ObjectOutputStream out) throws IOException {
        controller.log(LogType.SYSTEM, "Fetching emails for user: " + userEmail);

        File userFolder = createUserFolder(userEmail);
        File[] emailFiles = userFolder.listFiles((dir, name) -> name.startsWith("email_"));

        if (emailFiles == null || emailFiles.length == 0) {
            controller.log(LogType.SYSTEM, "No emails found for user: " + userEmail);
            sendSuccessResponse(out, "No emails found.");
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

        sendSuccessResponse(out, allEmails.toString().trim());
    }

    private synchronized void handleLoginCheck(String email, ObjectOutputStream out) throws IOException {
        if (!isValidEmail(email)) {
            sendErrorResponse(out, "Invalid email format.");
            return;
        }

        createUserFolder(email);
        sendSuccessResponse(out, "Login successful.");
        controller.log(LogType.SYSTEM, "Response flushed to client: Login successful.");
    }

    private synchronized void handleDeleteEmail(String requestData, Email mail, ObjectOutputStream out) throws IOException {
        String[] parts = requestData.split("\\|", 2);
        if (parts.length < 2) {
            sendErrorResponse(out, "Invalid request format.");
            return;
        }

        String userEmail = parts[0];
        String[] emailsToDelete = parts[1].split(",");
        File emailFile = getUserEmailFile(userEmail);

        if (!emailFile.exists()) {
            sendErrorResponse(out, "No emails found.");
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
        sendSuccessResponse(out, "Emails deleted successfully.");
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

    private void sendErrorResponse(ObjectOutputStream out, String message) throws IOException {
        out.writeObject("ERROR|" + message + "\n");
        out.flush();
        controller.log(LogType.SYSTEM, "Response flushed to client: " + message);
    }

    private void sendSuccessResponse(ObjectOutputStream out, String message) throws IOException {
        out.writeObject("SUCCESS|" + message + "\n");
        out.flush();
        controller.log(LogType.SYSTEM, "Response flushed to client: " + message);
    }

    public int getPort() {
        return port;
    }

    public boolean isRunning() {
        return running;
    }
}
