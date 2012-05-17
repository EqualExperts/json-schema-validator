package uk.co.o2.json.schema

import org.codehaus.jackson.JsonNode

class SchemaReference implements JsonSchema {
    final SchemaPassThroughCache registry
    final URL schemaLocation

    SchemaReference(SchemaPassThroughCache registry, URL schemaLocation) {
        this.registry = registry
        this.schemaLocation = schemaLocation
    }

    @Override
    List<ErrorMessage> validate(JsonNode jsonDocumentToValidate) {
        registry.getSchema(schemaLocation).validate(jsonDocumentToValidate)
    }
}
