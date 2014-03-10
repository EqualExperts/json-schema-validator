package uk.co.o2.json.schema;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;

import uk.co.o2.json.schema.ObjectSchema.Property;

import javax.json.*;


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

        for (ProcessedSchemaEntry schemaToRegister : compiledSchemasToRegister) {
            cache.registerSchema(schemaToRegister.schemaLocation, schemaToRegister.compiledSchema);
        }

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

        JsonString pattern = rawSchema.getJsonString("pattern");
        if (pattern != null) {
            result.setPattern(Pattern.compile(pattern.getString()));
        }

        JsonNumber minLength = rawSchema.getJsonNumber("minLength");
        if (minLength != null) {
            result.setMinLength(minLength.intValue());
        }

        JsonNumber maxLength = rawSchema.getJsonNumber("maxLength");
        if (maxLength != null) {
            result.setMaxLength(maxLength.intValue());
        }

        JsonNumber minimum = rawSchema.getJsonNumber("minimum");
        if (minimum != null) {
            result.setMinimum(minimum.bigDecimalValue());
        }

        JsonNumber maximum = rawSchema.getJsonNumber("maximum");
        if (maximum != null) {
            result.setMaximum(maximum.bigDecimalValue());
        }

        if (rawSchema.containsKey("exclusiveMinimum")) {
            result.setExclusiveMinimum(rawSchema.getBoolean("exclusiveMinimum"));
        }

        if (rawSchema.containsKey("exclusiveMaximum")) {
            result.setExclusiveMaximum(rawSchema.getBoolean("exclusiveMaximum"));
        }

        JsonArray enumeration = rawSchema.getJsonArray("enumeration");
        if (enumeration != null) {
            List<JsonValue> enumerationValues = new ArrayList<>();
            for (JsonValue node : enumeration) {
                enumerationValues.add(node);
            }
            result.setEnumeration(enumerationValues);
        }

        JsonString format = rawSchema.getJsonString("format");
        if (format!= null) {
            result.setFormat(format.getString());
        }
        return result;
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