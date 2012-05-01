package uk.co.o2.json.schema.provider;


import java.net.URL;

public interface SchemaLookup {
    
    URL getSchemaURL(String schemaLocation);
}
