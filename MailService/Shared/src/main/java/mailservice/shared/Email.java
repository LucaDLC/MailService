package mailservice.shared;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;

public class Email implements Serializable {
    private String id;
    private final String sender;
    private final List<String> receivers;
    private final String subject;
    private final String text;
    private Date date;
    private boolean isToRead;

    public Email(String sender, List<String> receivers, String subject, String text){
        this.sender = sender;
        this.subject = subject;
        this.text = text;
        this.receivers = new ArrayList<>(receivers);
        this.date = new Date();
        this.isToRead = false;
        this.id = UUID.randomUUID().toString(); // Genera un UUID al posto di usare hashCode
    }
    public static Email generateEmptyEmail(){
        Email email = new Email("", List.of(""), "",
                "");
        email.date = null;
        email.id = String.valueOf(email.hashCode());

        return email;
    }

    public String getId() {
        return id;
    }
    public void setId(String id) {this.id = id;}

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
    public Date getDate() { return date; }
    public boolean isToRead() {
        return isToRead;
    }

    public void setToRead(boolean b){
        this.isToRead = b;
    }

    public static boolean isEmpty(Email email){
        return email.getSender().equals("") && email.getReceivers().equals("") && email.getSubject().equals("") && email.getText().equals("");
    }

    @Override
    public int hashCode() {
        return Objects.hash(sender, receivers, subject, text, date, isToRead);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Email email = (Email) obj;
        return id.equals(email.id);
    }

    @Override
    public String toString() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String formattedDate = (date != null) ? dateFormat.format(date) : "No Date";
        return sender + " - " + subject + " (" + formattedDate + ")";
    }

}