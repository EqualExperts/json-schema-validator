package uk.co.o2.json.schema;

import org.codehaus.jackson.JsonNode;

import java.net.URL;
import java.util.List;

class SchemaReference implements JsonSchema {
    private final SchemaPassThroughCache registry;
    private final URL schemaLocation;

    SchemaReference(SchemaPassThroughCache registry, URL schemaLocation) {
        this.registry = registry;
        this.schemaLocation = schemaLocation;
    }

    @Override
    public List<ErrorMessage> validate(JsonNode jsonDocumentToValidate) {
        return registry.getSchema(schemaLocation).validate(jsonDocumentToValidate);
    }

    SchemaPassThroughCache getRegistry() {
        return registry;
    }

    URL getSchemaLocation() {
        return schemaLocation;
    }
}
