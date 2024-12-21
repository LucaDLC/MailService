package mailservice.shared;

import mailservice.shared.enums.CommandRequest;

import java.io.Serializable;

public record Request(String logged, CommandRequest cmdName,
                            Email mail) implements Serializable {

    @Override
    public String toString() {
        return "Request{ " +
                "User=" + logged +
                ", Command=" + cmdName.toString() +
                ((mail != null) ?
                        ", Mail =" + mail.getId() :
                        "") +
                " }";
    }
}
