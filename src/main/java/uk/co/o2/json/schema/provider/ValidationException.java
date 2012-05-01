package uk.co.o2.json.schema.provider;

import java.util.HashMap;
import java.util.Map;

public class ValidationException extends RuntimeException {

    private Map<String, String> errors;

    public ValidationException(Map<String, String> errors) {
        this.errors = errors;
    }
    
    public ValidationException(String key, String value) {
        errors = new HashMap<String, String>();
        errors.put(key, value);
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
