package mailservice.serverside.Model;

import mailservice.serverside.Configuration.CommandResponse;
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
    private volatile boolean running; //volatile is used to indicate that a variable's value will be modified by different threads
    private ServerController controller;

    public ServerModel(ServerController serverController) {
        this.controller = serverController;
        this.port = Integer.parseInt(ConfigManager.getInstance().readProperty("Server.Port"));
    }


    public void startServer() {
        synchronized (this) { // Sincronizzazione per prevenire avvii multipli
            if (running) {
                controller.log(LogType.INFO,"Server is already running on port " + port);
                return; // se il server è già in esecuzione, non fare nulla
            }
            running = true; // imposta running su true per indicare che il server è in avvio
        }

       //avvia il server in un nuovo thread
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);  //crea un nuovo server socket
                controller.log(LogType.INFO,"Server started on port " + port); //aggiunge un messaggio di log per segnalare che il server è stato avviato
                System.out.println("Server started on port " + port);

                while (running) {
                    Socket clientSocket = serverSocket.accept(); //accetta una connessione da un client
                    controller.log(LogType.INFO,"Client connected from: " + clientSocket.getInetAddress()); //aggiunge un messaggio di log per segnalare che un client si è connesso
                    new Thread(() -> handleClient(clientSocket)).start(); //crea un nuovo thread per gestire il client
                }
            } catch (BindException e) {
                controller.showErrorAlert("Port " + port + " is already in use.");
                controller.log(LogType.INFO,"Port " + port + " is already in use.");
            } catch (IOException e) {
                if(running){
                    controller.showErrorAlert("Error starting server: " + e.getMessage());
                    controller.log(LogType.ERROR,"Error starting server: " + e.getMessage());
                }

            }finally{
                stopServer(); //garantisce che il server venga fermato in caso di eccezione
            }
        }).start();
    }

    public void stopServer() {
        synchronized (this) { // sincronizza per prevenire problemi di concorrenza
            if (!running) return; // se il server non è in esecuzione, non fare nulla
            running = false; // imposta running su false per segnalare la chiusura
        }

        try {
            if(serverSocket != null && !serverSocket.isClosed()) {
                System.out.println("Closing server...");
                serverSocket.close();
                controller.log(LogType.INFO,"Server stopped on port " + port);
            }
        } catch (IOException e) {
            controller.log(LogType.ERROR,"Error stoppong server: "+ e.getMessage());
        }
    }

    private void handleClient(Socket clientSocket) {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))
        ) {
            controller.log(LogType.INFO,"Client connected: " + clientSocket.getInetAddress());

            String clientMessage;

            while ((clientMessage = in.readLine()) != null) {
                // Log del comando ricevuto
                controller.log(LogType.SYSTEM,"Received message: " + clientMessage);

                String[] parts = clientMessage.split("\\|");
                String command = parts[0];
                String argument = parts.length > 1 ? parts[1] : "";

                CommandResponse response;
                switch (command) {
                    case "LOGIN_CHECK":
                        response = handleLoginCheck(argument, out);
                        break;

                    case "FETCH_EMAIL":
                        response = handleFetchEmail(argument, out);
                        break;

                    case "SEND_EMAIL":
                        response = handleSendEmail(argument, out);
                        break;

                    case "DELETE_EMAIL":
                        response = handleDeleteEmail(argument, out);
                        break;
                    case "PING":
                        response= handlePing(out);
                        break;
                    default:
                        response = CommandResponse.ILLEGAL_PARAMS;
                        controller.log(LogType.ERROR,"Unknown command received: " + command);
                        out.write(response.name() + "\n");
                        out.flush();
                }

                if (response != null) {
                    controller.log(LogType.INFO,"Response sent: " + response.name());
                }
            }
        } catch (IOException e) {
            controller.log(LogType.ERROR,"Error handling client: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
                controller.log(LogType.INFO,"Client disconnected: " + clientSocket.getInetAddress());
            } catch (IOException e) {
                controller.log(LogType.ERROR,"Error closing client socket: " + e.getMessage());
            }
        }
    }

    private CommandResponse handleSendEmail(String emailData, BufferedWriter out) throws IOException {
        controller.log(LogType.SYSTEM,"Received SEND_EMAIL request: " + emailData);

        // Dividi i dati in base al separatore "|"
        String[] parts = emailData.split("\\|", 4);
        if (parts.length < 4) {
            controller.log(LogType.ERROR,"Invalid email format: " + emailData);
            out.write(CommandResponse.ILLEGAL_PARAMS.name() + "|Invalid email format\n");
            out.flush();
            return CommandResponse.ILLEGAL_PARAMS;
        }

        // Estrai i campi dall'email
        String sender = parts[0].trim();
        String[] receivers = parts[1].split(",");
        String subject = parts[2].trim();

        // Valida il mittente
        if (!isValidEmail(sender)) {
            controller.log(LogType.ERROR,"Invalid sender email: " + sender);
            out.write(CommandResponse.ILLEGAL_PARAMS.name() + "|Invalid sender email\n");
            out.flush();
            return CommandResponse.ILLEGAL_PARAMS;
        }

        // Valida i destinatari
        for (String receiver : receivers) {
            if (!isValidEmail(receiver.trim())) {
                controller.log(LogType.ERROR,"Invalid receiver email: " + receiver);
                out.write(CommandResponse.ILLEGAL_PARAMS.name() + "|Invalid receiver email\n");
                out.flush();
                return CommandResponse.ILLEGAL_PARAMS;
            }
        }

        String content = parts[3];
        controller.log(LogType.INFO,"Email content received: " + content);

        // Email validata con successo
        controller.log(LogType.INFO,"Email validated successfully.");
        saveEmail(sender, receivers, subject, content);
        out.write(CommandResponse.SUCCESS.name() + "\n");
        out.flush();

        saveEmailToFolders(sender, content);
        return CommandResponse.SUCCESS;
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[a-zA-Z0-9._%+-]+@rama.it$");
    }

    private void saveEmail(String sender, String[] receivers, String subject, String content) {
        // Logica di salvataggio dell'email
        controller.log(LogType.INFO,"Email saved: From " + sender + " to " + String.join(",", receivers));
    }

    private CommandResponse handleDeleteEmail(String requestData, BufferedWriter out) {
        controller.log(LogType.SYSTEM,"Processing email deletion request: " + requestData);

        // Estrai username e email da eliminare
        String[] parts = requestData.split("\\|", 2); // Dividi la stringa in username e email
        if (parts.length < 2) {
            controller.log(LogType.ERROR,"Invalid request format for email deletion.");
            try {
                out.write(CommandResponse.ILLEGAL_PARAMS.name() + "|Invalid request format\n");
                out.flush();
            } catch (IOException e) {
                controller.log(LogType.ERROR,"Error sending response: " + e.getMessage());
            }
            return CommandResponse.ILLEGAL_PARAMS;
        }

        String username = parts[0];
        String[] emailsToDelete = parts[1].split(",");

        File userFolder = new File("user_folders" + File.separator + username);
        if (!userFolder.exists() || !userFolder.isDirectory()) {
            controller.log(LogType.ERROR,"Folder for user " + username + " does not exist.");
            try {
                out.write(CommandResponse.FAILURE.name() + "|No folder found for user: " + username + "\n");
                out.flush();
            } catch (IOException e) {
                controller.log(LogType.ERROR,"Error sending response: " + e.getMessage());
            }
            return CommandResponse.FAILURE;
        }

        File sentEmailsFile = new File(userFolder, "sent_emails.txt");
        if (!sentEmailsFile.exists() || !sentEmailsFile.isFile()) {
            controller.log(LogType.SYSTEM,"No email file found for user: " + username);
            try {
                out.write(CommandResponse.FAILURE.name() + "|No emails found for user: " + username + "\n");
                out.flush();
            } catch (IOException e) {
                controller.log(LogType.ERROR,"Error sending response: " + e.getMessage());
            }
            return CommandResponse.FAILURE;
        }

        // Filtra le email e crea un nuovo file senza quelle selezionate
        try {
            File tempFile = new File(userFolder, "sent_emails.txt");
            try (BufferedReader reader = new BufferedReader(new FileReader(sentEmailsFile));
                 BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {

                String line;
                boolean emailDeleted = false;
                while ((line = reader.readLine()) != null) {
                    if (!shouldDelete(line, emailsToDelete)) {
                        writer.write(line);
                        writer.newLine();
                    } else {
                        emailDeleted = true;
                        controller.log(LogType.INFO,"Email deleted for user " + username + ": " + line);
                    }
                }

                if (!emailDeleted) {
                    controller.log(LogType.INFO,"No matching emails found for deletion.");
                    out.write(CommandResponse.FAILURE.name() + "|No matching emails found for deletion\n");
                    out.flush();
                    return CommandResponse.FAILURE;
                }
            }

            // Sostituisci il vecchio file con quello aggiornato
            if (!sentEmailsFile.delete() || !tempFile.renameTo(sentEmailsFile)) {
                controller.log(LogType.ERROR,"Error updating email file for user: " + username);
                out.write(CommandResponse.FAILURE.name() + "|Failed to update email file\n");
                out.flush();
                return CommandResponse.FAILURE;
            }

            controller.log(LogType.INFO,"Selected emails deleted successfully for user: " + username);
            out.write(CommandResponse.SUCCESS.name() + "|Selected emails deleted successfully\n");
            out.flush();
            return CommandResponse.SUCCESS;
        } catch (IOException e) {
            controller.log(LogType.ERROR,"Error processing email deletion for user: " + username + ": " + e.getMessage());
            try {
                out.write(CommandResponse.FAILURE.name() + "|Error processing email deletion\n");
                out.flush();
            } catch (IOException ex) {
                controller.log(LogType.ERROR,"Error sending response: " + ex.getMessage());
            }
            return CommandResponse.FAILURE;
        }
    }

    //Verifica se un'email deve essere eliminata in base al contenuto.
    private boolean shouldDelete(String email, String[] emailsToDelete) {
        for (String emailToDelete : emailsToDelete) {
            if (email.trim().equalsIgnoreCase(emailToDelete.trim())) {
                return true;
            }
        }
        return false;
    }

    private CommandResponse handlePing(BufferedWriter out) {
        try {
            controller.log(LogType.SYSTEM,"[DEBUG] Received PING request. Connection is alive.");
            out.write(CommandResponse.SUCCESS.name() + "\n");
            out.flush();
            return CommandResponse.SUCCESS;
        } catch (IOException e) {
            controller.log(LogType.SYSTEM,"[ERROR] Error handling PING request: " + e.getMessage());
            return CommandResponse.FAILURE;
        }
    }


    private CommandResponse handleLoginCheck(String email, BufferedWriter out) throws IOException {
        controller.log(LogType.SYSTEM,"Checking login for email: " + email);

        // Estrai il nome utente dalla email
        String username = email.split("@")[0];

        // Crea la cartella dell'utente
        createUserFolder(username);

        // Simula una validazione dell'email
        if (email.matches("^[a-zA-Z0-9._%+-]+@rama.it$")) {
            out.write(CommandResponse.SUCCESS.name() + "\n");
            out.flush();
            controller.log(LogType.INFO,"Login successful for: " + email);
            return CommandResponse.SUCCESS;
        } else {
            out.write(CommandResponse.FAILURE.name() + "\n");
            out.flush();
            controller.log(LogType.ERROR,"Invalid email: " + email);
            return CommandResponse.FAILURE;
        }
    }

    private CommandResponse handleFetchEmail(String userEmail, BufferedWriter writer) {
        try {
            String emails = fetchEmailsForUser(userEmail);
            writer.write("SUCCESS|" + emails + "\n"); // Risposta sempre valida
            writer.flush();
            return CommandResponse.SUCCESS;
        } catch (IOException e) {
            System.err.println("[ERROR] Error sending email payload: " + e.getMessage());
            return CommandResponse.FAILURE;
        }
    }

    public String fetchEmailsForUser(String userEmail) {
        File userFolder = new File("user_folders", userEmail);
        File emailFile = new File(userFolder, "sent_emails.txt");

        if (!emailFile.exists()) {
            return "No emails found for user: " + userEmail;
        }

        try (Stream<String> stream = Files.lines(emailFile.toPath())) {
            String emails = stream.collect(Collectors.joining(","));
            if (emails.isEmpty()) {
                return "No emails found for user: " + userEmail;
            }
            return "SUCCESS|" + emails;
        } catch (IOException e) {
            return "Error retrieving emails for user: " + userEmail;
        }
    }

    private void saveEmailToFolders(String username, String emailContent) {
        File userFolder = createUserFolder(username);
        File emailFile = new File(userFolder, "sent_emails.txt");

        try (FileWriter writer = new FileWriter(emailFile, true)) {
            writer.write(emailContent + "\n");
            writer.flush(); // Assicurati che i dati siano scritti su disco
            System.out.println("[DEBUG] Email saved: " + emailContent);
        } catch (IOException e) {
            System.err.println("[ERROR] Failed to save email for user: " + username + " - " + e.getMessage());
        }
    }

    private File createUserFolder(String username) {
        // Assicurati che il nome della cartella includa sempre "@rama.it"
        if (!username.endsWith("@rama.it")) {
            username += "@rama.it";
        }

        String baseDirectory = System.getProperty("user.dir") + File.separator + "UserFolders";
        File baseDir = new File(baseDirectory, username);

        if (!baseDir.exists()) {
            baseDir.mkdirs();
            controller.log(LogType.SYSTEM,"User folder created for: " + username + " at: " + baseDir.getAbsolutePath());
        }

        File userFolder = new File(baseDir, "sent_emails.txt");
        if (!userFolder.exists()) {
            try {
                userFolder.createNewFile();
                controller.log(LogType.INFO,"Created sent_emails.txt for: " + username);
            } catch (IOException e) {
                controller.log(LogType.ERROR,"Failed to create email file for user: " + username);
            }
        }

        return userFolder;
    }

    public int getPort() {
        return port;
    }
}
