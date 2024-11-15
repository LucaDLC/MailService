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
    private boolean running;
    private ServerController controller;

    public ServerModel(ServerController serverController) {
        this.controller = serverController;
        this.port = Integer.parseInt(ConfigManager.getInstance().readProperty("Server.Port"));
    }

    public static ServerModel getInstance(ServerController serverController){
        return new ServerModel(serverController);
    }
    public void startServer() {
        if (running) {
            controller.log("Server is already running on port " + port);
            return; // Server is already running
        }

        new Thread(() -> {
            try {
                while (true) {
                    try {
                        serverSocket = new ServerSocket(port);
                        break;  // Successfully bound, exit loop
                    } catch (BindException e) {
                        // If the port is already in use, try a different one
                        port++;
                        controller.log("Port " + (port - 1) + " is already in use. Trying port " + port);
                        ConfigManager.getInstance().setProperty("Server.Port", String.valueOf(port));
                    }
                }
                running = true;
                controller.log("Server started on port " + port);
                System.out.println("Server started on port " + port);

                while (running) {
                    Socket clientSocket = serverSocket.accept();
                    controller.log("Client connected from: " + clientSocket.getInetAddress());
                    new Thread(() -> handleClient(clientSocket)).start();
                }
            } catch (IOException e) {
                controller.showErrorAlert("Error starting server: " + e.getMessage());
                controller.log("Error starting server: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }


    public void stopServer() {
        try {
            running = false;
            if(serverSocket != null && !serverSocket.isClosed()) {
                System.out.println("Closing server...");
                serverSocket.close();
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
