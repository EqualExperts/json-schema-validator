package uk.co.o2.json.schema.jaxrs;

import java.net.URL;

public interface SchemaLookup {
    URL getSchemaURL(String schemaLocation);
}
