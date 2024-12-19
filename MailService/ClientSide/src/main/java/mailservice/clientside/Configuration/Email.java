package mailservice.clientside.Configuration;

import java.text.SimpleDateFormat;
import java.util.*;

public class Email {
    private final String sender;
    private final List<String> receivers;
    private final String subject;
    private final String text;
    private Date date;

    public Email(String sender, List<String> receivers, String subject, String text) {
        this.sender = sender;
        this.receivers = receivers;
        this.subject = subject;
        this.text = text;
        this.date = new Date();  // Imposta la data corrente all'invio
    }

    public String getSender() {
        return sender;
    }

    public List<String> getReceivers() {
        return receivers;
    }

    public String getSubject() {
        return subject;
    }

    public String getText() {
        return text;
    }

    public String getDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(date);
    }

    @Override
    public String toString() {
        return subject;  // Per visualizzare il soggetto nell'interfaccia utente della lista
    }
}
