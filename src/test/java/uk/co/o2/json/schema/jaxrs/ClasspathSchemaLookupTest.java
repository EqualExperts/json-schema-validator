package uk.co.o2.json.schema.jaxrs;

import org.junit.Test;

import java.net.URL;

import static org.junit.Assert.*;

public class ClasspathSchemaLookupTest {

    private final SchemaLookup lookup = new ClasspathSchemaLookup();

    @Test
    public void getSchemaURL_shouldReturnAURL_givenAValidSchemaLocation() throws Exception {
        String expectedSchemaLocation = "/sample-json-schema.json";

        URL schemaURL = lookup.getSchemaURL(expectedSchemaLocation);

        assertNotNull(schemaURL);
        assertTrue(schemaURL.toExternalForm().endsWith(expectedSchemaLocation));
    }

    @Test
    public void getSchemaURL_shouldPrependALeadingSlash_givenARelativeSchemaLocation() throws Exception {
        String expectedSchemaLocation = "sample-json-schema.json";

        URL relativeSchemaURL = lookup.getSchemaURL(expectedSchemaLocation);

        assertEquals(relativeSchemaURL, lookup.getSchemaURL("/" + expectedSchemaLocation));
    }

    @Test
    public void getSchemaURL_shouldThrowAnIllegalArgumentException_givenANullSchemaLocation() throws Exception {
        try {
            lookup.getSchemaURL(null);
            fail("expected an exception");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("cannot be null"));
        }
    }

    @Test
    public void getSchemaURL_shouldThrowAnIllegalArgumentException_givenASchemaLocationThatDoesNotExist() throws Exception {
        String expectedSchemaLocation = "/json-schema-that-does-not-exist.json";

        try {
            lookup.getSchemaURL(expectedSchemaLocation);
            fail("expected an exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Schema " + expectedSchemaLocation + " was not found", e.getMessage());
        }
    }
}
