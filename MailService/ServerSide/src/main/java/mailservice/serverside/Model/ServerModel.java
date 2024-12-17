package mailservice.serverside.Model;

import mailservice.serverside.Configuration.ConfigManager;
import mailservice.serverside.Controller.ServerController;
import mailservice.serverside.Log.LogType;

import java.io.*;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ServerModel {
    private int port;
    private ServerSocket serverSocket;
    private volatile boolean running;
    private ServerController controller;

    public ServerModel(ServerController serverController) {
        this.controller = serverController;
        this.port = Integer.parseInt(ConfigManager.getInstance().readProperty("Server.Port"));
    }

    public void startServer() {
        synchronized (this) {
            if (running) {
                controller.log(LogType.INFO, "Server is already running on port " + port);
                return;
            }
            running = true;
        }

        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                controller.log(LogType.INFO, "Server started on port " + port);

                while (running) {
                    Socket clientSocket = serverSocket.accept();
                    controller.log(LogType.INFO, "Client connected from: " + clientSocket.getInetAddress());
                    new Thread(() -> handleClient(clientSocket)).start();
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
        synchronized (this) {
            if (!running) return;
            running = false;
        }
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                controller.log(LogType.INFO, "Server stopped on port " + port);
            }
        } catch (IOException e) {
            controller.log(LogType.ERROR, "Error stopping server: " + e.getMessage());
        }
    }

    private void handleClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))) {

            String clientMessage;
            while ((clientMessage = in.readLine()) != null) {
                controller.log(LogType.SYSTEM, "Received message: " + clientMessage);

                String[] parts = clientMessage.split("\\|", 2);
                String command = parts[0];
                String argument = parts.length > 1 ? parts[1] : "";

                switch (command) {
                    case "LOGIN_CHECK" -> handleLoginCheck(argument, out);
                    case "FETCH_EMAIL" -> handleFetchEmail(argument, out);
                    case "SEND_EMAIL" -> handleSendEmail(argument, out);
                    case "DELETE_EMAIL" -> handleDeleteEmail(argument, out);
                    default -> sendErrorResponse(out, "Unknown command.");
                }
            }
        } catch (IOException e) {
            controller.log(LogType.ERROR, "Client error: " + e.getMessage());
        }
    }

    private void handleSendEmail(String emailData, BufferedWriter out) throws IOException {
        String[] parts = emailData.split("\\|", 4);
        if (parts.length < 4) {
            sendErrorResponse(out, "Invalid email format.");
            return;
        }

        String sender = parts[0].trim();
        String[] receivers = parts[1].split(",");
        String subject = parts[2].trim();
        String content = parts[3];

        if (!isValidEmail(sender) || !areValidEmails(receivers)) {
            sendErrorResponse(out, "Invalid sender or receiver email.");
            return;
        }

        saveEmailToFolders(sender, String.format("From: %s\nTo: %s\nSubject: %s\nContent: %s\n",
                sender, String.join(",", receivers), subject, content));

        sendSuccessResponse(out, "Email sent successfully.");
    }

    private void handleFetchEmail(String userEmail, BufferedWriter out) throws IOException {
        File userFolder = createUserFolder(userEmail);
        File[] emailFiles = userFolder.listFiles((dir, name) -> name.startsWith("email_"));

        if (emailFiles == null || emailFiles.length == 0) {
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
                allEmails.append("-----\n"); // Separatore tra email
            }
        }
        sendSuccessResponse(out, allEmails.toString().trim());
    }

    private void handleLoginCheck(String email, BufferedWriter out) throws IOException {
        if (!isValidEmail(email)) {
            sendErrorResponse(out, "Invalid email format.");
            return;
        }

        createUserFolder(email);
        sendSuccessResponse(out, "Login successful.");
    }

    private void handleDeleteEmail(String requestData, BufferedWriter out) throws IOException {
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

    private void saveEmailToFolders(String username, String emailContent) {
        File userFolder = createUserFolder(username);
        String emailFileName = "email_" + System.currentTimeMillis() + ".txt";

        File emailFile = new File(userFolder, emailFileName);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(emailFile))) {
            writer.write(emailContent);
        } catch (IOException e) {
            controller.log(LogType.ERROR, "Failed to save email: " + e.getMessage());
        }
    }

    private File getUserEmailFile(String userEmail) {
        File userFolder = createUserFolder(userEmail);
        return new File(userFolder, "sent_emails.txt");
    }

    private File createUserFolder(String username) {
        File folder = new File("UserFolders", username);
        if (!folder.exists()) folder.mkdirs();
        return folder;
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[a-zA-Z0-9._%+-]+@rama.it$");
    }

    private boolean areValidEmails(String[] emails) {
        for (String email : emails) {
            if (!isValidEmail(email.trim())) return false;
        }
        return true;
    }

    private boolean shouldDelete(String line, String[] emailsToDelete) {
        for (String email : emailsToDelete) {
            if (line.contains(email.trim())) return true;
        }
        return false;
    }

    private void sendErrorResponse(BufferedWriter out, String message) throws IOException {
        out.write("ERROR|" + message + "\n");
        out.flush();
    }

    private void sendSuccessResponse(BufferedWriter out, String message) throws IOException {
        out.write("SUCCESS|" + message + "\n");
        out.flush();
    }

    public int getPort() {
        return port;
    }
}
