package uk.co.o2.json.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.Test;

import java.net.URL;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
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
        JsonNode expectedDocument = new TextNode("I am a document!!!");
        List<ErrorMessage> expectedResults = Arrays.asList(new ErrorMessage("foo", "bar"));

        JsonSchema mockReferencedSchema = mock(JsonSchema.class);
        when(mockReferencedSchema.validate(expectedDocument)).thenReturn(expectedResults);
        when(registry.getSchema(expectedSchemaLocation)).thenReturn(mockReferencedSchema);

        JsonSchema schema = new SchemaReference(registry, expectedSchemaLocation);

        List<ErrorMessage> results = schema.validate(expectedDocument);

        assertSame(expectedResults, results);
    }

    @Test
    public void getDescription_shouldReturnTheDescriptionOfTheReferencedSchema() throws Exception {
        URL expectedSchemaLocation = new URL("http://www.example.com/");
        String expectedDescription = "blahBlahBlah";

        JsonSchema mockReferencedSchema = mock(JsonSchema.class);
        doReturn(expectedDescription).when(mockReferencedSchema).getDescription();
        when(registry.getSchema(expectedSchemaLocation)).thenReturn(mockReferencedSchema);

        JsonSchema schema = new SchemaReference(registry, expectedSchemaLocation);

        String description = schema.getDescription();

        assertEquals(expectedDescription, description);
    }
}
