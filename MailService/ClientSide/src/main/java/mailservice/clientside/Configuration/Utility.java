package mailservice.clientside.Configuration;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;

public class Utility {

    public static boolean validateEmail(String email){
        return Pattern.matches("^[a-zA-Z0-9._%+-]+@rama.it$", email);
    }

    public static String formatDate(Date date){
        SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        return dateFormatter.format(date);
    }
}
