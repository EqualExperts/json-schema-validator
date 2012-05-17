package uk.co.o2.json.schema

import java.util.regex.Pattern
import org.codehaus.jackson.JsonFactory
import org.codehaus.jackson.JsonNode
import org.codehaus.jackson.JsonParser
import org.codehaus.jackson.JsonParseException

class SchemaCompiler {
    private static Map<Boolean, JsonSchema> DEFAULT_ADDITIONAL_PROPERTIES_SETTINGS = [
            (true): ObjectSchema.ALLOW_ALL_ADDITIONAL_PROPERTIES,
            (false): ObjectSchema.FORBID_ANY_ADDITIONAL_PROPERTIES
    ]

    private final SchemaPassThroughCache cache
    private final JsonFactory jsonFactory
    private final Queue<ProcessingEntry> schemasToCompile = new LinkedList<ProcessingEntry>()

    SchemaCompiler(SchemaPassThroughCache cache, JsonFactory jsonFactory) {
        this.cache = cache
        this.jsonFactory = jsonFactory
    }


    JsonSchema parse(URL schemaLocation) {

        List<ProcessedSchemaEntry> compiledSchemasToRegister = []

        scheduleSchemaForProcessing(schemaLocation)

        ProcessingEntry entry = schemasToCompile.peek()
        while (entry) {
            JsonSchema compiledSchema = parseInternal(entry.rawSchema, entry.schemaLocation)
            compiledSchemasToRegister << new ProcessedSchemaEntry(entry.schemaLocation, compiledSchema)

            schemasToCompile.poll()
            entry = schemasToCompile.peek()
        }

        compiledSchemasToRegister.each {
            cache.registerSchema(it.schemaLocation, it.compiledSchema)
        }

        cache.getSchema(schemaLocation)
    }

    private void scheduleSchemaForProcessing(URL schemaLocation) {
        if (cache.hasSchema(schemaLocation)) {
            return //schema has already been compiled before, or on another thread
        }
        if (schemasToCompile.find { it.schemaLocation.toString() == schemaLocation.toString() }) {
            return //schema is already scheduled for compilation
        }

        try {
            JsonParser parser = jsonFactory.createJsonParser(schemaLocation)
            JsonNode rawSchema = parser.readValueAsTree()
            schemasToCompile.add(new ProcessingEntry(schemaLocation, rawSchema))
        } catch (JsonParseException jpe) {
            throw new IllegalArgumentException("The schema at location ${schemaLocation} contains invalid JSON", jpe)
        } catch (IOException ioe) {
            throw new IllegalArgumentException("No schema found at location ${schemaLocation}", ioe)
        }
    }

    private JsonSchema parseInternal(JsonNode rawSchema, URL currentSchemaLocation) {
        if (!rawSchema.object) {
            throw new IllegalArgumentException("A valid json schema must be an object")
        }

        JsonNode ref = rawSchema.get('$ref')
        if (ref != null) {
            URL referencedSchemaLocation = new URL(currentSchemaLocation, ref.textValue)
            scheduleSchemaForProcessing(referencedSchemaLocation)
            return new SchemaReference(cache, referencedSchemaLocation)
        }

        String type = rawSchema.get('type').valueAsText
        if (isSimpleTypeSchema(type)) {
            return parseSimpleTypeSchema(rawSchema)
        } else if (isObjectSchema(type)) {
            return parseObjectSchema(rawSchema, currentSchemaLocation)
        } else if (isArraySchema(type)) {
            return parseArraySchema(rawSchema, currentSchemaLocation)
        }
        throw new IllegalArgumentException("Illegal schema type ${type}")
    }

    private boolean isArraySchema(String type) {
        type == 'array'
    }

    private boolean isObjectSchema(String type) {
        type == 'object'
    }

    private boolean isSimpleTypeSchema(String type) {
        SimpleType.values().find { it.name() == type.toUpperCase() } != null
    }

    private SimpleTypeSchema parseSimpleTypeSchema(JsonNode rawSchema) {
        SimpleTypeSchema result = new SimpleTypeSchema(type: SimpleType.valueOf(rawSchema.get('type').valueAsText.toUpperCase()))

        JsonNode pattern = rawSchema.get('pattern')
        if (pattern != null) {
            result.pattern = Pattern.compile(pattern.asText())
        }

        JsonNode minLength = rawSchema.get('minLength')
        if (minLength != null) {
            result.minLength = minLength.intValue
        }

        JsonNode maxLength = rawSchema.get('maxLength')
        if (maxLength != null) {
            result.maxLength = maxLength.intValue
        }

        JsonNode minimum = rawSchema.get('minimum')
        if (minimum != null) {
            result.minimum = minimum.decimalValue
        }

        JsonNode maximum = rawSchema.get('maximum')
        if (maximum != null) {
            result.maximum = maximum.decimalValue
        }

        JsonNode exclusiveMinimum = rawSchema.get('exclusiveMinimum')
        if (exclusiveMinimum != null) {
            result.exclusiveMinimum = exclusiveMinimum.booleanValue
        }

        JsonNode exclusiveMaximum = rawSchema.get('exclusiveMaximum')
        if (exclusiveMaximum != null) {
            result.exclusiveMaximum = exclusiveMaximum.booleanValue
        }

        JsonNode enumeration = rawSchema.get('enumeration')
        if (enumeration != null) {
            result.enumeration = enumeration.iterator().collect { it }
        }

        result.format = rawSchema.get('format')?.textValue
        return result
    }

    private ArraySchema parseArraySchema(JsonNode rawSchema, URL schemaLocation) {
        ArraySchema result = new ArraySchema()
        JsonNode rawItems = rawSchema.get('items')
        if (rawItems != null) {
            result.items = parseInternal(rawItems, schemaLocation)
        }
        JsonNode rawMinItems = rawSchema.get('minItems')
        if (rawMinItems != null) {
            result.minItems = rawMinItems.intValue
        }
        JsonNode rawMaxItems = rawSchema.get('maxItems')
        if (rawMaxItems != null) {
            result.maxItems = rawMaxItems.intValue
        }
        JsonNode rawUniqueItems = rawSchema.get('uniqueItems')
        if (rawUniqueItems != null) {
            result.uniqueItems = rawUniqueItems.booleanValue
        }
        return result
    }

    private ObjectSchema parseObjectSchema(JsonNode rawSchema, URL schemaLocation) {
        ObjectSchema result = new ObjectSchema()
        configureAdditionalPropertiesForObjectSchema(rawSchema.get('additionalProperties'), result, schemaLocation)
        configurePropertiesForObjectSchema(rawSchema.get('properties'), result, schemaLocation)
        return result
    }

    private void configureAdditionalPropertiesForObjectSchema(JsonNode additionalProperties, ObjectSchema schema, URL schemaLocation) {
        if (additionalProperties == null) {
            return
        }
        if (additionalProperties.boolean) {
            schema.additionalProperties = DEFAULT_ADDITIONAL_PROPERTIES_SETTINGS[additionalProperties.booleanValue]
        } else {
            schema.additionalProperties = parseInternal(additionalProperties, schemaLocation)
        }
    }

    private void configurePropertiesForObjectSchema(JsonNode rawProperties, ObjectSchema schema, URL schemaLocation) {
        if (rawProperties == null) {
            return
        }
        rawProperties.fieldNames.each { String fieldName ->
            JsonNode nestedSchema = rawProperties.get(fieldName)
            ObjectProperty property = new ObjectProperty(name: fieldName)

            JsonNode required = nestedSchema.get('required')
            if (required != null) {
                property.required = required.booleanValue
            }

            property.nestedSchema = parseInternal(nestedSchema, schemaLocation)

            schema.properties << property
        }
    }

    private static class ProcessingEntry {
        final URL schemaLocation
        final JsonNode rawSchema

        ProcessingEntry(URL schemaLocation, JsonNode rawSchema) {
            this.schemaLocation = schemaLocation
            this.rawSchema = rawSchema
        }
    }

    private static class ProcessedSchemaEntry {
        final URL schemaLocation
        final JsonSchema compiledSchema

        ProcessedSchemaEntry(URL schemaLocation, JsonSchema compiledSchema) {
            this.schemaLocation = schemaLocation
            this.compiledSchema = compiledSchema
        }
    }
}