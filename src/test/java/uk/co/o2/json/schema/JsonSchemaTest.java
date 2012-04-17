package uk.co.o2.json.schema;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class JsonSchemaTest {

    private JsonFactory jsonFactory = new JsonFactory(new ObjectMapper());
    private SchemaPassThroughCache schemaFactory = new SchemaPassThroughCache(jsonFactory);

    @Test
    public void happyCase_shouldValidateAJsonDocumentCorrectly() throws Exception {
        JsonSchema schema = schemaFactory.getSchema(getClass().getClassLoader().getResource("sample-json-schema.json"));
        JsonNode json = jsonFactory.createJsonParser(getClass().getClassLoader().getResource("valid-json-document.json")).readValueAsTree();

        List<ErrorMessage> errors = schema.validate(json);

        assertTrue(errors.isEmpty());
    }

    @Test
    public void sadCase_shouldValidateAJsonDocumentAndReportErrors() throws Exception {
        JsonSchema schema = schemaFactory.getSchema(getClass().getClassLoader().getResource("sample-json-schema.json"));
        JsonNode json = jsonFactory.createJsonParser(getClass().getClassLoader().getResource("invalid-json-document.json")).readValueAsTree();

        List<ErrorMessage> errors = schema.validate(json);

        assertEquals(8, errors.size());
        assertEquals("id", errors.get(0).getLocation());
        assertEquals("Missing required property id", errors.get(0).getMessage());

        assertEquals("brand", errors.get(1).getLocation());
        assertEquals("Value \"AnyBrand\" must be one of: [\"Nokia\", \"Samsung\", \"Apple\"]", errors.get(1).getMessage());

        assertEquals("salePrice.amount", errors.get(2).getLocation());
        assertEquals("Value '5.0' must be greater or equal to 10.0", errors.get(2).getMessage());

        assertEquals("costPrice.amount", errors.get(3).getLocation());
        assertEquals("Value '10.0' must be greater than 10.0 when exclusiveMinimum is true", errors.get(3).getMessage());

        assertEquals("extraInfo", errors.get(4).getLocation());
        assertEquals("Current array size of 4 is greater than allowed maximum array size of 3", errors.get(4).getMessage());

        assertEquals("description", errors.get(5).getLocation());
        assertEquals("Value 'short' must be greater or equal to 10 characters", errors.get(5).getMessage());

        assertEquals("longDescription", errors.get(6).getLocation());
        assertEquals("Value 'some long description more than permitted limit' must be less or equal to 20 characters", errors.get(6).getMessage());

        assertEquals("regexValue", errors.get(7).getLocation());
        assertEquals("Value '++' is not a valid regex", errors.get(7).getMessage());
    }
}
