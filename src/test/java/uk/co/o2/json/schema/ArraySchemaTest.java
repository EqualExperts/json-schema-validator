package uk.co.o2.json.schema;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

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

    @Test
    public void getDescription_shouldReturnArray() throws Exception {
        ArraySchema schema = new ArraySchema();

        String description = schema.getDescription();

        assertEquals("array", description);
    }

    @Test
    public void isAcceptableType_shouldReturnTrue_givenAnArray() throws Exception {
        JsonNode document = factory.createJsonParser("[]").readValueAsTree();
        ArraySchema schema = new ArraySchema();

        boolean result =  schema.isAcceptableType(document);

        assertTrue(result);
    }

    @Test
    public void isAcceptableType_shouldReturnFalse_givenAnObject() throws Exception {
        JsonNode document = factory.createJsonParser("{}").readValueAsTree();
        ArraySchema schema = new ArraySchema();

        boolean result =  schema.isAcceptableType(document);

        assertFalse(result);
    }

    @Test
    public void isAcceptableType_shouldReturnFalse_givenAValueNode() throws Exception {
        JsonNode document = new TextNode("blah");
        ArraySchema schema = new ArraySchema();

        boolean result =  schema.isAcceptableType(document);

        assertFalse(result);
    }

    @Test
    public void isAcceptableType_shouldReturnFalse_givenANullNode() throws Exception {
        JsonNode document = NullNode.getInstance();
        ArraySchema schema = new ArraySchema();

        boolean result =  schema.isAcceptableType(document);

        assertFalse(result);
    }
}