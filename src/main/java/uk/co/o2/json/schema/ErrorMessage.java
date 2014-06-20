package uk.co.o2.json.schema;

import java.util.Arrays;
import java.util.List;

public class ErrorMessage {
    static List<ErrorMessage> singleError(String location, String formatString, Object... args) {
        return Arrays.asList(new ErrorMessage(location, String.format(formatString, args)));
    }

    private final String location;
    private final String message;

    public ErrorMessage(String location, String message) {
        this.location = location;
        this.message = message;
    }

    public ErrorMessage(String locationPrefix, ErrorMessage nestedMessage) {
        String separator = nestedMessage.isSeparatorNecessary() ? "." : "";
        this.location = locationPrefix + separator + nestedMessage.location;
        this.message = nestedMessage.message;
    }

    private boolean isSeparatorNecessary() {
        return (!"".equals(location)) && (!location.startsWith("["));
    }

    public String getLocation() {
        return location;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return String.format("%s: %s", location, message);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ErrorMessage)) {
            return false;
        }
        ErrorMessage other = (ErrorMessage) obj;
        return toString().equals(other.toString());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }
}