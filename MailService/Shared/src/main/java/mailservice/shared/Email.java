package mailservice.shared;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;

public class Email implements Serializable {
    private int id;
    private final String sender;
    private final List<String> receivers;
    private final String subject;
    private final String text;
    private String date;
    private boolean isToRead;

    public Email(String sender, List<String> receivers, String subject, String text){
        this.sender = sender;
        this.subject = subject;
        this.text = text;
        this.receivers = new ArrayList<>(receivers);
        this.date = newDate(null);
        this.isToRead = false;
        this.id = this.hashCode();
    }

    public Email(String sender, List<String> receivers, String subject, String text, String data){
        this.sender = sender;
        this.subject = subject;
        this.text = text;
        this.receivers = new ArrayList<>(receivers);
        this.date = newDate(data);
        this.isToRead = false;
        this.id = this.hashCode();
    }

    public static Email generateEmptyEmail(){
        Email email = new Email("", List.of(""), "",
                "");
        email.date = null;
        email.id = email.hashCode();

        return email;
    }

    public int getId() {
        return id;
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
    public String getDate() { return date; }
    public boolean isToRead() {
        return isToRead;
    }

    public void setToRead(boolean b){
        this.isToRead = b;
    }

    public static boolean isEmpty(Email email){
        return email.getSender().equals("") && email.getReceivers().equals("") && email.getSubject().equals("") && email.getText().equals("");
    }

    private static String newDate(String passedDate) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        if (passedDate == null){
            return dateFormatter.format(new Date());
        }
        else {
            return passedDate;
        }
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
        return id == email.id && isToRead == email.isToRead && sender.equals(email.sender) && receivers.equals(email.receivers) && subject.equals(email.subject) && text.equals(email.text) && date.equals(email.date);
    }

    @Override
    public String toString() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        String formattedDate = (date != null) ? dateFormat.format(date) : "No Date";
        return sender + " - " + subject + " (" + formattedDate + ")";
    }


}