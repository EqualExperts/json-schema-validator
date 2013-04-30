package uk.co.o2.json.schema;

import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.fasterxml.jackson.core.JsonFactory;

public class SchemaPassThroughCache {

    final ConcurrentMap<String, JsonSchema> registeredSchemas = new ConcurrentHashMap<String, JsonSchema>();
    private SchemaCompilerFactory schemaCompilerFactory;

    public SchemaPassThroughCache(JsonFactory factory) {
        this.schemaCompilerFactory = new SchemaCompilerFactory(this, factory);
    }

    /*
        Uses setter injection only because of the circular reference
     */
    void setSchemaCompilerFactory(SchemaCompilerFactory schemaCompilerFactory) {
        this.schemaCompilerFactory = schemaCompilerFactory;
    }

    public JsonSchema getSchema(URL schemaLocation) {
        if (hasSchema(schemaLocation)) {
            return registeredSchemas.get(schemaLocation.toString());
        }
        return schemaCompilerFactory.create().parse(schemaLocation);
    }

    public boolean hasSchema(URL schemaLocation) {
        return registeredSchemas.containsKey(schemaLocation.toString());
    }

    void registerSchema(URL schemaLocation, JsonSchema schema) {
        registeredSchemas.putIfAbsent(schemaLocation.toString(), schema);
    }
    
    static class SchemaCompilerFactory {

        private final SchemaPassThroughCache cache;
        private final JsonFactory factory;

        SchemaCompilerFactory(SchemaPassThroughCache cache, JsonFactory factory) {
            this.cache = cache;
            this.factory = factory;
        }

        SchemaCompiler create() {
            return new SchemaCompiler(cache, factory);
        }
    }
}