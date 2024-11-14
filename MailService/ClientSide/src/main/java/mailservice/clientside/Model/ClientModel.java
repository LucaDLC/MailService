package mailservice.clientside.Model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;
import mailservice.clientside.Configuration.ConfigManager;

public class ClientModel {

    private String userLogged;
    private String serverHost;
    private int serverPort;
    private int fetchPeriod;

    private ClientModel() {
        ConfigManager configManager = ConfigManager.getInstance();
        this.serverHost = configManager.readProperty("Client.ServerHost");
        this.serverPort = Integer.parseInt(configManager.readProperty("Client.ServerPort"));
        this.fetchPeriod = Integer.parseInt(configManager.readProperty("Client.Fetch"));
    }

    public static ClientModel getInstance(){
        return new ClientModel();
    }

    public boolean validateEmail(String email){
        boolean checkMail = Pattern.matches("^[a-zA-Z0-9._%+-]+@rama.it$", email);
        if (checkMail)
        {
            this.userLogged = email;
            //NECESSARIO AGGIUNGERE APERTURA COMUNICAZIONE AL SERVER COMUNICANDO L'EMAIL
        }
        return checkMail;
    }

    public static String formatDate(Date date) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        return dateFormatter.format(date);
    }


}
