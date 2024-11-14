package mailservice.clientside.Model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;

public class ClientModel {

    private String userLogged;
    private String serverHost;
    private int serverPort;
    private int fetchPeriod;

    public boolean validateEmail(String email){
        boolean checkMail = Pattern.matches("^[a-zA-Z0-9._%+-]+@rama.it$", email);
        if (checkMail)
        {
            this.userLogged = email;
        }
        return checkMail;
    }

    public static String formatDate(Date date) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        return dateFormatter.format(date);
    }


}
