package uk.co.o2.json.schema

import groovy.transform.PackageScope
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import org.codehaus.jackson.JsonFactory

class SchemaPassThroughCache {

    @PackageScope
    final ConcurrentMap<String, JsonSchema> registeredSchemas = new ConcurrentHashMap<String, JsonSchema>()
    private final JsonFactory factory

    SchemaPassThroughCache(JsonFactory factory) {
        this.factory = factory
    }

    JsonSchema getSchema(URL schemaLocation) {
        if (hasSchema(schemaLocation)) {
            return registeredSchemas[schemaLocation.toString()]
        }
        new SchemaCompiler(this, factory).parse(schemaLocation)
    }

    boolean hasSchema(URL schemaLocation) {
        registeredSchemas.containsKey(schemaLocation.toString())
    }

    @PackageScope
    void registerSchema(URL schemaLocation, JsonSchema schema) {
        registeredSchemas.putIfAbsent(schemaLocation.toString(), schema)
    }
}
