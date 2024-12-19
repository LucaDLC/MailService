package mailservice.clientside;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import mailservice.clientside.Configuration.ConfigManager;
import mailservice.clientside.Model.ClientModel;

import java.io.IOException;

public class ClientApp extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("Login.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("ClientSide - Login");
        stage.setScene(scene);
        stage.show();

        stage.setOnCloseRequest(event -> stop());
    }

    @Override
    public void stop() {
        System.out.println("[INFO] Application is stopping...");
        ClientModel.getInstance().disconnectFromServer();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
