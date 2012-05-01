package uk.co.o2.json.schema.provider;

import java.net.URL;

public class ClasspathSchemaLookup  implements SchemaLookup {

    @Override
    public URL getSchemaURL(String schemaLocation) {
        URL schemaUrl =  this.getClass().getResource(schemaLocation);

        if (schemaLocation == null || schemaUrl == null) {
            throw new RuntimeException("Could not find schema: " + schemaLocation);
        }
        return schemaUrl;
    }
}
