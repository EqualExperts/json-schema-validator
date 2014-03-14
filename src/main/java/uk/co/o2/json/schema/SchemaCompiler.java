package uk.co.o2.json.schema;

import uk.co.o2.json.schema.ObjectSchema.Property;

import javax.json.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;

class SchemaCompiler {
    private final SchemaPassThroughCache cache;
    private final JsonReaderFactory jsonFactory;
    private final Queue<ProcessingEntry> schemasToCompile = new LinkedList<>();

    public SchemaCompiler(SchemaPassThroughCache cache, JsonReaderFactory jsonFactory) {
        this.cache = cache;
        this.jsonFactory = jsonFactory;
    }

    public JsonSchema parse(URL schemaLocation) {

        List<ProcessedSchemaEntry> compiledSchemasToRegister = new ArrayList<>();

        scheduleSchemaForProcessing(schemaLocation);

        ProcessingEntry entry = schemasToCompile.peek();
        while (entry != null) {
            JsonSchema compiledSchema = parse(entry.rawSchema, entry.schemaLocation);
            compiledSchemasToRegister.add(new ProcessedSchemaEntry(entry.schemaLocation, compiledSchema));

            schemasToCompile.poll();
            entry = schemasToCompile.peek();
        }

        compiledSchemasToRegister.forEach(it -> cache.registerSchema(it.schemaLocation, it.compiledSchema));

        return cache.getSchema(schemaLocation);
    }

    private void scheduleSchemaForProcessing(URL schemaLocation) {
        if (cache.hasSchema(schemaLocation)) {
            return; //schema has already been compiled before, or on another thread
        }

        for (ProcessingEntry it : schemasToCompile) {
            if (it.schemaLocation.toString().equals(schemaLocation.toString())) {
                return; //schema is already scheduled for compilation
            }
        }

        try {
            try (JsonReader parser = jsonFactory.createReader(schemaLocation.openStream())) {
                JsonObject rawSchema = parser.readObject();
                schemasToCompile.add(new ProcessingEntry(schemaLocation, rawSchema));
            }
        } catch (JsonException jpe) {
            throw new IllegalArgumentException("The schema at location " + schemaLocation.toString() + " contains invalid JSON", jpe);
        } catch (IOException ioe) {
            throw new IllegalArgumentException("Could not retrieve schema from " + schemaLocation.toString(), ioe);
        }
    }

    private JsonSchema parse(JsonValue rawSchema, URL currentSchemaLocation) {
        if (rawSchema.getValueType() != JsonValue.ValueType.OBJECT) {
            throw new IllegalArgumentException("A valid json schema must be an object");
        }

        JsonObject jsonSchema = (JsonObject) rawSchema;
        JsonValue ref = jsonSchema.get("$ref");
        if (ref != null) {
            URL referencedSchemaLocation;
            try {
                referencedSchemaLocation = new URL(currentSchemaLocation, ((JsonString)ref).getString());
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("The schema reference is malformed", e);
            }
            scheduleSchemaForProcessing(referencedSchemaLocation);
            return new SchemaReference(cache, referencedSchemaLocation);
        }

        String type = jsonSchema.getString("type");
        if (isSimpleTypeSchema(type)) {
            return parseSimpleTypeSchema(jsonSchema);
        } else if (isObjectSchema(type)) {
            return parseObjectSchema(jsonSchema, currentSchemaLocation);
        } else if (isArraySchema(type)) {
            return parseArraySchema(jsonSchema, currentSchemaLocation);
        }
        throw new IllegalArgumentException("Illegal schema type " + type);
    }

    private boolean isArraySchema(String type) {
        return "array".equals(type);
    }

    private boolean isObjectSchema(String type) {
        return "object".equals(type);
    }

    private boolean isSimpleTypeSchema(String type) {
        for (SimpleType it : SimpleType.values()) {
            if (it.name().equals(type.toUpperCase())) {
                return true;
            }
        }
        return false;
    }

    private SimpleTypeSchema parseSimpleTypeSchema(JsonObject rawSchema) {
        SimpleTypeSchema result = new SimpleTypeSchema();
        result.setType(SimpleType.valueOf(rawSchema.getString("type").toUpperCase()));

        getJsonString(rawSchema, "pattern").<Pattern>map(Pattern::compile).ifPresent(result::setPattern);
        getJsonInteger(rawSchema, "minLength").ifPresent(result::setMinLength);
        getJsonInteger(rawSchema, "maxLength").ifPresent(result::setMaxLength);
        getJsonBigDecimal(rawSchema, "minimum").ifPresent(result::setMinimum);
        getJsonBigDecimal(rawSchema, "maximum").ifPresent(result::setMaximum);
        getJsonBoolean(rawSchema, "exclusiveMinimum").ifPresent(result::setExclusiveMinimum);
        getJsonBoolean(rawSchema, "exclusiveMaximum").ifPresent(result::setExclusiveMaximum);
        getJsonArray(rawSchema, "enumeration").<ArrayList>map(ArrayList::new).ifPresent(result::setEnumeration);
        getJsonString(rawSchema, "format").ifPresent(result::setFormat);

        return result;
    }

    private static Optional<String> getJsonString(JsonObject schema, String fieldName) {
        return getJsonField(schema::getJsonString, fieldName).map(JsonString::getString);
    }

    private static Optional<JsonArray> getJsonArray(JsonObject schema, String fieldName) {
        return getJsonField(schema::getJsonArray, fieldName);
    }

    private static Optional<BigDecimal> getJsonBigDecimal(JsonObject schema, String fieldName) {
        return getJsonNumber(schema, fieldName).map(JsonNumber::bigDecimalValue);
    }

    private static Optional<Integer> getJsonInteger(JsonObject schema, String fieldName) {
        return getJsonNumber(schema, fieldName).map(JsonNumber::intValue);
    }

    private static Optional<JsonNumber> getJsonNumber(JsonObject schema, String fieldName) {
        return getJsonField(schema::getJsonNumber, fieldName);
    }

    private static Optional<Boolean> getJsonBoolean(JsonObject schema, String fieldName) {
        if (!schema.containsKey(fieldName)) {
            return Optional.empty();
        }
        return Optional.of(schema.getBoolean(fieldName));
    }

    private static <T> Optional<T> getJsonField(Function<String, T> method, String fieldName) {
        return Optional.ofNullable(method.apply(fieldName));
    }

    private ArraySchema parseArraySchema(JsonObject rawSchema, URL schemaLocation) {
        ArraySchema result = new ArraySchema();
        JsonValue rawItems = rawSchema.get("items");
        if (rawItems != null) {
            result.setItems(parse(rawItems, schemaLocation));
        }
        JsonNumber rawMinItems = rawSchema.getJsonNumber("minItems");
        if (rawMinItems != null) {
            result.setMinItems(rawMinItems.intValue());
        }
        JsonNumber rawMaxItems = rawSchema.getJsonNumber("maxItems");
        if (rawMaxItems != null) {
            result.setMaxItems(rawMaxItems.intValue());
        }
        return result;
    }

    private ObjectSchema parseObjectSchema(JsonObject rawSchema, URL schemaLocation) {
        ObjectSchema result = new ObjectSchema();
        configureAdditionalPropertiesForObjectSchema(rawSchema.get("additionalProperties"), result, schemaLocation);
        configurePropertiesForObjectSchema(rawSchema.getJsonObject("properties"), result, schemaLocation);
        return result;
    }

    private void configureAdditionalPropertiesForObjectSchema(JsonValue additionalProperties, ObjectSchema schema, URL schemaLocation) {
        if (additionalProperties == null) {
            return;
        }
        if (additionalProperties.getValueType() == JsonValue.ValueType.TRUE || additionalProperties.getValueType() == JsonValue.ValueType.FALSE) {
            JsonSchema additionalPropertiesSchema = additionalProperties.getValueType() == JsonValue.ValueType.TRUE ? ObjectSchema.ALLOW_ALL_ADDITIONAL_PROPERTIES : ObjectSchema.FORBID_ANY_ADDITIONAL_PROPERTIES;
            schema.setAdditionalProperties(additionalPropertiesSchema);
        } else {
            schema.setAdditionalProperties(parse(additionalProperties, schemaLocation));
        }
    }

    private void configurePropertiesForObjectSchema(JsonObject rawProperties, ObjectSchema schema, URL schemaLocation) {
        if (rawProperties == null) {
            return;
        }

        for (String fieldName : rawProperties.keySet()) {

            Property property = new Property();
            property.setName(fieldName);
            
            JsonObject nestedSchema = rawProperties.getJsonObject(fieldName);
            property.setNestedSchema(parse(nestedSchema, schemaLocation));

            JsonValue required = nestedSchema.get("required");
            if (required != null) {
                property.setRequired(required.getValueType() == JsonValue.ValueType.TRUE);
            }
            
            schema.getProperties().add(property);
        }
    }

    private static class ProcessingEntry {
        final URL schemaLocation;
        final JsonObject rawSchema;

        ProcessingEntry(URL schemaLocation, JsonObject rawSchema) {
            this.schemaLocation = schemaLocation;
            this.rawSchema = rawSchema;
        }
    }

    private static class ProcessedSchemaEntry {
        final URL schemaLocation;
        final JsonSchema compiledSchema;

        ProcessedSchemaEntry(URL schemaLocation, JsonSchema compiledSchema) {
            this.schemaLocation = schemaLocation;
            this.compiledSchema = compiledSchema;
        }
    }
}