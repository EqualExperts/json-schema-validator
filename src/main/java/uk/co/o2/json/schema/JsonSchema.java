package uk.co.o2.json.schema;

import javax.json.JsonValue;
import java.util.List;

public interface JsonSchema {
    List<ErrorMessage> validate(JsonValue jsonDocumentToValidate);
}