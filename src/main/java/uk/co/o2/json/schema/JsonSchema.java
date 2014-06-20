package uk.co.o2.json.schema;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public interface JsonSchema {
    List<ErrorMessage> validate(JsonNode jsonDocumentToValidate);
    String getDescription();
    boolean isAcceptableType(JsonNode jsonDocument);
}