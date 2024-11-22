package mailservice.clientside.Model;

import mailservice.clientside.Utility.Utils;
import java.util.*;

public class Email {
    private int id;
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
        return id == email.id && isToRead == email.isToRead && sender.equals(email.sender) && receivers.equals(email.receivers) && subject.equals(email.subject) && text.equals(email.text) && date.equals(email.date);
    }

    @Override
    public String toString() { return sender + " - " + subject; }
    public  String dateToString() {
        return (this.date == null) ? "" : Utils.formatDate(this.date);
    }

    @Override
    public int compareTo(Email email) {
        return email.getDate().compareTo(this.date);
    }
}
