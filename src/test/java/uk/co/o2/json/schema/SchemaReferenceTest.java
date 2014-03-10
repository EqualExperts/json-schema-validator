package uk.co.o2.json.schema;

import org.junit.Test;

import javax.json.Json;
import javax.json.JsonValue;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.when;

public class SchemaReferenceTest {

    private SchemaPassThroughCache registry = mock(SchemaPassThroughCache.class);

    @Test
    public void constructor_shouldSetRegistryAndSchemaLocationFields() throws Exception {
        URL schemaLocation = new URL("http://www.example.com/");

        SchemaReference ref  = new SchemaReference(registry, schemaLocation);

        assertSame(registry, ref.getRegistry());
        assertSame(schemaLocation, ref.getSchemaLocation());
    }

    @Test
    public void validate_shouldLoadAJsonSchemaFromTheRegistryAndDelegateTheValidateCall() throws Exception {
        URL expectedSchemaLocation = new URL("http://www.example.com/");
        JsonValue expectedDocument = Json.createObjectBuilder().add("foo", "I am a document!!!").build().getJsonString("foo");
        List<ErrorMessage> expectedResults = Arrays.asList(new ErrorMessage("foo", "bar"));

        JsonSchema mockReferencedSchema = mock(JsonSchema.class);
        when(mockReferencedSchema.validate(expectedDocument)).thenReturn(expectedResults);
        when(registry.getSchema(expectedSchemaLocation)).thenReturn(mockReferencedSchema);

        JsonSchema schema = new SchemaReference(registry, expectedSchemaLocation);

        List<ErrorMessage> results = schema.validate(expectedDocument);

        assertSame(expectedResults, results);
    }
}
