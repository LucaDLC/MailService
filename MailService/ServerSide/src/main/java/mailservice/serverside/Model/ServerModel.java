package mailservice.serverside.Model;

import mailservice.serverside.Configuration.ConfigManager;
import mailservice.serverside.Controller.ServerController;

import java.io.*;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;

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
                controller.log("Server is already running on port " + port);
                return; // se il server è già in esecuzione, non fare nulla
            }
            running = true; // imposta running su true per indicare che il server è in avvio
        }

       //avvia il server in un nuovo thread
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);  //crea un nuovo server socket
                controller.log("Server started on port " + port); //aggiunge un messaggio di log per segnalare che il server è stato avviato
                System.out.println("Server started on port " + port);

                while (running) {
                    Socket clientSocket = serverSocket.accept(); //accetta una connessione da un client
                    controller.log("Client connected from: " + clientSocket.getInetAddress()); //aggiunge un messaggio di log per segnalare che un client si è connesso
                    new Thread(() -> handleClient(clientSocket)).start(); //crea un nuovo thread per gestire il client
                }
            } catch (BindException e) {
                controller.showErrorAlert("Port " + port + " is already in use.");
                controller.log("Port " + port + " is already in use.");
                e.printStackTrace();
            } catch (IOException e) {
                controller.showErrorAlert("Error starting server: " + e.getMessage());
                controller.log("Error starting server: " + e.getMessage());
                e.printStackTrace();
            }finally{
                stopServer(); //garantisce che il server venga fermato in caso di eccezione
            }
        }).start();
    }

    public void stopServer() {
        synchronized (this) { // sincronizza per prevenire problemi di concorrenza
            if (!running) {
                return; // se il server non è in esecuzione, non fare nulla
            }
            running = false; // imposta running su false per segnalare la chiusura
        }

        try {
            if(serverSocket != null && !serverSocket.isClosed()) {
                System.out.println("Closing server...");
                serverSocket.close();
                controller.log("Server stopped on port " + port);
            }
        } catch (IOException e) {
            System.out.println("Error stopping server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {    //invia messaggi al client

            String clientMessage;
            while ((clientMessage = in.readLine()) != null) {
                //aggiungiamo log ogni volta che un client invia un messaggio
                controller.log("Received message from client: " + clientMessage);
                //invia una risposta al client
                out.println("Server received message: " + clientMessage);
            }
        } catch (IOException e) {
            controller.showErrorAlert("Error handling client: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
                controller.log("Client disconnected"+ clientSocket.getInetAddress());
            } catch (IOException e) {
                controller.showErrorAlert("Error closing client socket: " + e.getMessage());
                e.printStackTrace();
            }
        }

    }

    public int getPort() {
        return port;
    }
}
