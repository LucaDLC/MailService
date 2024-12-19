package mailservice.clientside;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import mailservice.clientside.Configuration.ConfigManager;
import mailservice.clientside.Model.ClientModel;

import java.io.IOException;

public class ClientApp extends Application {
    ClientModel.NetworkManager networkManager = ClientModel.NetworkManager.getInstance();

    @Override
    public void start(Stage stage) throws IOException {
        resetUserPreferences();
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("Login.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("ClientSide - Login");
        stage.setScene(scene);
        stage.show();

        stage.setOnCloseRequest(event -> stop());
    }

    private void resetUserPreferences() {
        ConfigManager.getInstance().setProperty("Client.Mail", "");
        ClientModel.getInstance().logout();
    }

    @Override
    public void stop() {
        System.out.println("[INFO] Application is stopping...");
        networkManager.disconnectFromServer();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
