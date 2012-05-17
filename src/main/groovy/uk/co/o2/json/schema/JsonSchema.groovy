package uk.co.o2.json.schema

import org.codehaus.jackson.JsonNode

interface JsonSchema {
    List<ErrorMessage> validate(JsonNode jsonDocumentToValidate)
}