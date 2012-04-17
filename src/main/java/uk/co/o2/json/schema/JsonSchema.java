package uk.co.o2.json.schema;

import org.codehaus.jackson.JsonNode;

import java.util.List;

public interface JsonSchema {
    List<ErrorMessage> validate(JsonNode jsonDocumentToValidate);
}