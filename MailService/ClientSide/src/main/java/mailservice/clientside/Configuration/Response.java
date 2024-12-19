package mailservice.clientside.Configuration;

import java.io.Serializable;
import java.util.List;

    public record Response(CommandResponse responseName,
                             List<Email> args) implements Serializable {
    @Override
    public String toString() {
        return "Response{ " +
                "Command=" + responseName +
                ", Argument=" + args +
                " }";
    }
}
