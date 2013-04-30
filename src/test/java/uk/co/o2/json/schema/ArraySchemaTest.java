package uk.co.o2.json.schema;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ArraySchemaTest {
    private static JsonFactory factory = new JsonFactory(new ObjectMapper());

    @Test
    public void validate_shouldEmitAnErrorMessage_givenAJsonObject() throws Exception {
        JsonNode document = factory.createJsonParser("{\"foo\": \"bar\"}").readValueAsTree();

        ArraySchema schema = new ArraySchema();

        List<ErrorMessage> result = schema.validate(document);

        assertEquals(1, result.size());
        assertTrue(result.get(0).getMessage().contains("must be an array"));
    }

    @Test
    public void validate_shouldEmitAnErrorMessage_givenASimpleValue() throws Exception {
        JsonNode document = factory.createJsonParser("\"foo\"").readValueAsTree();

        ArraySchema schema = new ArraySchema();

        List<ErrorMessage> result = schema.validate(document);

        assertEquals(1, result.size());
        assertTrue(result.get(0).getMessage().contains("must be an array"));
    }

    @Test
    public void shouldImplementJsonSchema() {
        assertTrue(JsonSchema.class.isAssignableFrom(ArraySchema.class));
    }

    @Test
    public void validate_shouldAllowAnyArrayItem_whenNoItemSchemaIsSpecified() throws Exception {
        JsonNode document = factory.createJsonParser("[" +
            "{\"foo\": \"value1\", \"bar\": 123}," +
            "\"string\"," +
            "{}" +
        "]").readValueAsTree();

        ArraySchema schema = new ArraySchema();

        List<ErrorMessage> result = schema.validate(document);

        assertTrue(result.isEmpty());
    }

    @Test
    public void validate_shouldValidateAllItemsAndCombineAnyErrorMessages() throws Exception {
        JsonNode document = factory.createJsonParser("[" +
            "{\"foo\": \"value1\", \"bar\": 123}," +
            "{\"foo\": true, \"bar\": \"barValue\"}," +
            "{}" +
        "]").readValueAsTree();

        ArraySchema schema = new ArraySchema();
        schema.setItems(new ObjectSchema() {{
            getProperties().add(new ObjectSchema.Property() {{
                setName("foo");
                setRequired(true);
                setNestedSchema(new SimpleTypeSchema() {{
                    setType(SimpleType.STRING);
                }});
            }});
            getProperties().add(new ObjectSchema.Property(){{
                setName("bar");
                setRequired(true);
                setNestedSchema(new SimpleTypeSchema() {{
                    setType(SimpleType.STRING);
                }});
            }});
        }});

        List<ErrorMessage> result = schema.validate(document);

        assertEquals(4, result.size());
        assertEquals("[0].bar", result.get(0).getLocation());
        assertEquals("[1].foo", result.get(1).getLocation());
        assertEquals("[2].foo", result.get(2).getLocation());
        assertEquals("[2].bar", result.get(3).getLocation());
    }

    @Test
    public void validate_shouldNotReturnErrorMessage_givenNoOfItemsInArrayIsLessThanOrEqualToMaxItems() throws Exception {
        JsonNode document = factory.createJsonParser("[" +
            "{\"foo\": \"value1\", \"bar\": 123}," +
            "\"string\"," +
            "{}" +
        "]").readValueAsTree();

        ArraySchema schema = new ArraySchema();
        schema.setMaxItems(4);

        List<ErrorMessage> result = schema.validate(document);

        assertTrue(result.isEmpty());
    }

    @Test
    public void validate_shouldReturnErrorMessage_givenNoOfItemsInArrayIsGreaterThanMaxItems() throws Exception {
        JsonNode document = factory.createJsonParser("[" +
            "{\"foo\": \"value1\", \"bar\": 123}," +
            "\"string\"," +
            "{}" +
        "]").readValueAsTree();

        ArraySchema schema = new ArraySchema();
        schema.setMaxItems(2);

        List<ErrorMessage> result = schema.validate(document);

        assertEquals(1, result.size());
        assertEquals("", result.get(0).getLocation());
        assertEquals("Current array size of 3 is greater than allowed maximum array size of 2", result.get(0).getMessage());
    }

    @Test
    public void validate_shouldNotReturnErrorMessage_givenNoOfItemsInArrayIsGreaterThanOrEqualToMinItems() throws Exception {
        JsonNode document = factory.createJsonParser("[" +
            "{\"foo\": \"value1\", \"bar\": 123}," +
            "\"string\"," +
            "{}" +
        "]").readValueAsTree();

        ArraySchema schema = new ArraySchema();
        schema.setMinItems(2);

        List<ErrorMessage> result = schema.validate(document);

        assertTrue(result.isEmpty());
    }

    @Test
    public void validate_shouldReturnErrorMessage_givenNoOfItemsInArrayIsLessThanMinItems() throws Exception {
        JsonNode document = factory.createJsonParser("[" +
            "{\"foo\": \"value1\", \"bar\": 123}," +
            "\"string\"," +
            "{}" +
        "]").readValueAsTree();

        ArraySchema schema = new ArraySchema();
        schema.setMinItems(5);

        List<ErrorMessage> result = schema.validate(document);

        assertEquals(1, result.size());
        assertEquals("",result.get(0).getLocation());
        assertEquals("Current array size of 3 is less than allowed minimum array size of 5", result.get(0).getMessage());
    }
}