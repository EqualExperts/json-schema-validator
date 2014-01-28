package uk.co.o2.json.schema;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import uk.co.o2.json.schema.ObjectSchema.Property;


class SchemaCompiler {
    private final SchemaPassThroughCache cache;
    private final JsonFactory jsonFactory;
    private final Queue<ProcessingEntry> schemasToCompile = new LinkedList<>();

    public SchemaCompiler(SchemaPassThroughCache cache, JsonFactory jsonFactory) {
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
            JsonParser parser = jsonFactory.createJsonParser(schemaLocation);
            try {
                JsonNode rawSchema = parser.readValueAsTree();
                schemasToCompile.add(new ProcessingEntry(schemaLocation, rawSchema));
            } finally {
                parser.close();
            }
        } catch (JsonParseException jpe) {
            throw new IllegalArgumentException("The schema at location " + schemaLocation.toString() + " contains invalid JSON", jpe);
        } catch (IOException ioe) {
            throw new IllegalArgumentException("Could not retrieve schema from " + schemaLocation.toString(), ioe);
        }
    }

    private JsonSchema parse(JsonNode rawSchema, URL currentSchemaLocation) {
        if (!rawSchema.isObject()) {
            throw new IllegalArgumentException("A valid json schema must be an object");
        }

        JsonNode ref = rawSchema.get("$ref");
        if (ref != null) {
            URL referencedSchemaLocation;
            try {
                referencedSchemaLocation = new URL(currentSchemaLocation, ref.textValue());
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("The schema reference is malformed", e);
            }
            scheduleSchemaForProcessing(referencedSchemaLocation);
            return new SchemaReference(cache, referencedSchemaLocation);
        }

        String type = rawSchema.get("type").asText();
        if (isSimpleTypeSchema(type)) {
            return parseSimpleTypeSchema(rawSchema);
        } else if (isObjectSchema(type)) {
            return parseObjectSchema(rawSchema, currentSchemaLocation);
        } else if (isArraySchema(type)) {
            return parseArraySchema(rawSchema, currentSchemaLocation);
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

    private SimpleTypeSchema parseSimpleTypeSchema(JsonNode rawSchema) {
        SimpleTypeSchema result = new SimpleTypeSchema();
        result.setType(SimpleType.valueOf(rawSchema.get("type").asText().toUpperCase()));

        JsonNode pattern = rawSchema.get("pattern");
        if (pattern != null) {
            result.setPattern(Pattern.compile(pattern.textValue()));
        }

        JsonNode minLength = rawSchema.get("minLength");
        if (minLength != null) {
            result.setMinLength(minLength.intValue());
        }

        JsonNode maxLength = rawSchema.get("maxLength");
        if (maxLength != null) {
            result.setMaxLength(maxLength.intValue());
        }

        JsonNode minimum = rawSchema.get("minimum");
        if (minimum != null) {
            result.setMinimum(minimum.decimalValue());
        }

        JsonNode maximum = rawSchema.get("maximum");
        if (maximum != null) {
            result.setMaximum(maximum.decimalValue());
        }

        JsonNode exclusiveMinimum = rawSchema.get("exclusiveMinimum");
        if (exclusiveMinimum != null) {
            result.setExclusiveMinimum(exclusiveMinimum.booleanValue());
        }

        JsonNode exclusiveMaximum = rawSchema.get("exclusiveMaximum");
        if (exclusiveMaximum != null) {
            result.setExclusiveMaximum(exclusiveMaximum.booleanValue());
        }

        JsonNode enumeration = rawSchema.get("enumeration");
        if (enumeration != null) {
            List<JsonNode> enumerationValues = new ArrayList<>();
            for (JsonNode node : enumeration) {
                enumerationValues.add(node);
            }
            result.setEnumeration(enumerationValues);
        }

        JsonNode format = rawSchema.get("format");
        if (format!= null) {
            result.setFormat(format.textValue());
        }
        return result;
    }

    private ArraySchema parseArraySchema(JsonNode rawSchema, URL schemaLocation) {
        ArraySchema result = new ArraySchema();
        JsonNode rawItems = rawSchema.get("items");
        if (rawItems != null) {
            result.setItems(parse(rawItems, schemaLocation));
        }
        JsonNode rawMinItems = rawSchema.get("minItems");
        if (rawMinItems != null) {
            result.setMinItems(rawMinItems.intValue());
        }
        JsonNode rawMaxItems = rawSchema.get("maxItems");
        if (rawMaxItems != null) {
            result.setMaxItems(rawMaxItems.intValue());
        }
        return result;
    }

    private ObjectSchema parseObjectSchema(JsonNode rawSchema, URL schemaLocation) {
        ObjectSchema result = new ObjectSchema();
        configureAdditionalPropertiesForObjectSchema(rawSchema.get("additionalProperties"), result, schemaLocation);
        configurePropertiesForObjectSchema(rawSchema.get("properties"), result, schemaLocation);
        return result;
    }

    private void configureAdditionalPropertiesForObjectSchema(JsonNode additionalProperties, ObjectSchema schema, URL schemaLocation) {
        if (additionalProperties == null) {
            return;
        }
        if (additionalProperties.isBoolean()) {
            JsonSchema additionalPropertiesSchema = additionalProperties.booleanValue() ? ObjectSchema.ALLOW_ALL_ADDITIONAL_PROPERTIES : ObjectSchema.FORBID_ANY_ADDITIONAL_PROPERTIES;
            schema.setAdditionalProperties(additionalPropertiesSchema);
        } else {
            schema.setAdditionalProperties(parse(additionalProperties, schemaLocation));
        }
    }

    private void configurePropertiesForObjectSchema(JsonNode rawProperties, ObjectSchema schema, URL schemaLocation) {
        if (rawProperties == null) {
            return;
        }

        for (Iterator<String> iterator = rawProperties.fieldNames(); iterator.hasNext();) {
            String fieldName = iterator.next();

            Property property = new Property();
            property.setName(fieldName);
            
            JsonNode nestedSchema = rawProperties.get(fieldName);
            property.setNestedSchema(parse(nestedSchema, schemaLocation));

            JsonNode required = nestedSchema.get("required");
            if (required != null) {
                property.setRequired(required.booleanValue());
            }
            
            schema.getProperties().add(property);
        }
    }

    private static class ProcessingEntry {
        final URL schemaLocation;
        final JsonNode rawSchema;

        ProcessingEntry(URL schemaLocation, JsonNode rawSchema) {
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