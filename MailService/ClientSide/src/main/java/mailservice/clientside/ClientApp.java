package mailservice.clientside; //package che contiene le classi per il client

import javafx.application.Application; //classe base per tutte le applicazioni JavaFX
import javafx.application.Platform; //permette di accedere a metodi di utilità per le applicazioni JavaFX
import javafx.fxml.FXMLLoader; //carica file FXML, che descrivono l'interfaccia utente di un'applicazione JavaFX
import javafx.scene.Scene; //contenitore per tutti i contenuti di un'interfaccia utente JavaFX
import javafx.stage.Stage; //rappresenta la finestra principale di un'applicazione JavaFX


import java.io.IOException;
import java.util.concurrent.ExecutorService; //interfaccia che fornisce metodi per gestire un pool di thread
import java.util.concurrent.Executors; //classe che fornisce metodi per creare pool di thread
import java.util.concurrent.ScheduledExecutorService; //interfaccia che estende ExecutorService e fornisce metodi per eseguire attività in modo periodico
import java.util.concurrent.TimeUnit; //classe che fornisce metodi per convertire il tempo da un'unità a un'altra

public class ClientApp extends Application {


    private static ScheduledExecutorService fetchEmails; //pool di thread per il fetch delle email
    private static ExecutorService GUI;

    @Override
    public void start(Stage stage) throws IOException {
        //start(Stage stage) è il metodo principale di un'applicazione JavaFX, che viene chiamato quando l'applicazione viene avviata
        FXMLLoader fxmlLoader = new FXMLLoader(ClientApp.class.getResource("/mailservice/clientside/Login.fxml")); //crea un oggetto per carica il file FXML
        Scene scene = new Scene(fxmlLoader.load()); //crea la scena con il contenuto del file FXML
        stage.setTitle("ClientSide - Login"); //imposta il titolo della finestra
        stage.setScene(scene); //associa la scena(l'interfaccia utente) alla finestra
        stage.show();

        // Initialize service
        fetchEmails = Executors.newScheduledThreadPool(1); //crea un pool di thread con un solo thread che esegue attività in modo periodico
        fetchEmails.scheduleAtFixedRate(() -> {
        }, 0, 1, TimeUnit.MINUTES); //esegue l'attività ogni minuto
    }

    @Override
    public void stop() {
        fetchEmails.shutdown();
        GUI.shutdown();
        try {
            if (!fetchEmails.awaitTermination(3, TimeUnit.SECONDS)) {
                fetchEmails.shutdownNow(); //se entro 3 secondi non termina, interrompe l'esecuzione dei thread
            }
        } catch (InterruptedException e) { //se awaitTermination viene interrotto
            fetchEmails.shutdownNow();
        }
        try {
            if (!GUI.awaitTermination(3, TimeUnit.SECONDS)) {
                GUI.shutdownNow();
            }
        } catch (InterruptedException e) {
            GUI.shutdownNow();
        }
    }

    public static void main(String[] args) {
        GUI = Executors.newSingleThreadExecutor();
        GUI.execute(Application::launch);
    } //avvia l'applicazione
}



//MANCA STRUTTURA DI FETCH EMAILS