package mailservice.clientside.Controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class MainController {
    //collegamento con la GUI tramite l'annotazione @FXML
    @FXML   // questa annotazione indica che la variabile o il metodo è associato a un file .fxml
    private Label welcomeText; //Label è un componente grafico che visualizza un testo non modificabile

    //gestione del bottone tramite l'annotazione @FXML
    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("Welcome to JavaFX Application!"); //quando il bottone viene premuto, il testo della Label welcometext viene cambiato in "Welcome to JavaFX Application!"
    } //metodo che viene chiamato quando si preme il bottone
}