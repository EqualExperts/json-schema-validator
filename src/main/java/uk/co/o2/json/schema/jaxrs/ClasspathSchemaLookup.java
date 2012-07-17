package uk.co.o2.json.schema.jaxrs;

import java.net.URL;

public class ClasspathSchemaLookup implements SchemaLookup {

    @Override
    public URL getSchemaURL(String schemaLocation) {
        if (schemaLocation == null) {
            throw new IllegalArgumentException("schemaLocation cannot be null");
        }

        if (!schemaLocation.startsWith("/")) {
            schemaLocation = "/" + schemaLocation;
        }

        URL schemaUrl =  this.getClass().getResource(schemaLocation);
        if (schemaUrl == null) {
            throw new IllegalArgumentException("Schema " + schemaLocation + " was not found");
        }
        return schemaUrl;
    }
}