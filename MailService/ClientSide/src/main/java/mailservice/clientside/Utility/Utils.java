package mailservice.clientside.Utility;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

public class Utils {
    public static boolean validateEmail(String email){
        return Pattern.matches("^(.+?)@rama.it", email);
    }

    public static String formatDate(Date date){
        SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        return dateFormatter.format(date);
    }

    public static String receiversToString(List<String> list) {
        StringBuilder s = new StringBuilder();
        for (String address: list ) {
            s.append(", ").append(address); // va ad aggiungere una virgola prima di ogni indirizzo email per separarli correttamente
        }
        return s.toString();
    }
}
