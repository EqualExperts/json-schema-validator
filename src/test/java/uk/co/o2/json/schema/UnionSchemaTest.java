package uk.co.o2.json.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UnionSchemaTest {

    @Test
    public void shouldImplementJsonSchema() throws Exception {
        assertTrue(JsonSchema.class.isAssignableFrom(UnionSchema.class));
    }

    @Test
    public void validate_shouldReturnNoErrors_givenAtLeastOneNestedSchemaWithNoErrors() throws Exception {
        UnionSchema schema = new UnionSchema();

        SimpleTypeSchema noErrors = new SimpleTypeSchema();
        noErrors.setType(SimpleType.ANY);

        ObjectSchema anObviousError = new ObjectSchema();

        schema.getNestedSchemas().add(noErrors);
        schema.getNestedSchemas().add(anObviousError);

        JsonNode nodeToValidate = new TextNode("a string value");

        List<ErrorMessage> result = schema.validate(nodeToValidate);

        assertTrue(result.isEmpty());
    }

    @Test
    public void validate_shouldReturnASingleErrorMessageListingTheAcceptableTypes_givenNoneOfTheNestedSchemaAcceptThisTypeOfNode() throws Exception {
        UnionSchema schema = new UnionSchema();

        SimpleTypeSchema intSchema = new SimpleTypeSchema();
        intSchema.setType(SimpleType.INTEGER);

        SimpleTypeSchema anotherIntSchema = new SimpleTypeSchema();
        anotherIntSchema.setType(SimpleType.INTEGER);

        SimpleTypeSchema nullSchema = new SimpleTypeSchema();
        nullSchema.setType(SimpleType.NULL);

        ObjectSchema objectSchema = new ObjectSchema();

        schema.getNestedSchemas().add(intSchema);
        schema.getNestedSchemas().add(anotherIntSchema);
        schema.getNestedSchemas().add(nullSchema);
        schema.getNestedSchemas().add(objectSchema);

        JsonNode nodeToValidate = new TextNode("a string value");

        List<ErrorMessage> result = schema.validate(nodeToValidate);

        assertEquals(1, result.size());
        ErrorMessage message = result.get(0);

        //this error message is chosen to be consistent with the list emitted by SimpleTypeSchema
        assertEquals("", message.getLocation());
        assertEquals("Invalid type: must be one of: [\"integer\", \"null\", \"object\"]", message.getMessage());
    }

    @Test
    public void validate_shouldReturnTheErrorMessagesFromTheNestedSchemaWithTheFewestErrors() throws Exception {
        UnionSchema schema = new UnionSchema();

        SimpleTypeSchema intSchemaA = new SimpleTypeSchema();
        intSchemaA.setType(SimpleType.INTEGER);
        intSchemaA.setMinimum(10);
        schema.getNestedSchemas().add(intSchemaA);

        SimpleTypeSchema intSchemaB = new SimpleTypeSchema();
        intSchemaB.setType(SimpleType.INTEGER);
        intSchemaB.setMinimum(10);
        intSchemaB.setMaximum(15);
        schema.getNestedSchemas().add(intSchemaB);

        JsonNode nodeToValidate = new IntNode(5);

        List<ErrorMessage> result = schema.validate(nodeToValidate);

        List<ErrorMessage> expectedErrorMessages = intSchemaA.validate(nodeToValidate);
        assertEquals(1, result.size());
        assertEquals(expectedErrorMessages, result);
    }

    @Test
    public void isAcceptableType_shouldReturnTrue_whenAnyNestedSchemaAcceptsTheType() throws Exception {
        UnionSchema schema = new UnionSchema();

        SimpleTypeSchema intSchema = new SimpleTypeSchema();
        intSchema.setType(SimpleType.INTEGER);
        schema.getNestedSchemas().add(intSchema);

        SimpleTypeSchema nullSchema = new SimpleTypeSchema();
        nullSchema.setType(SimpleType.NULL);
        schema.getNestedSchemas().add(nullSchema);

        JsonNode nodeToValidate = new IntNode(5);

        boolean result = schema.isAcceptableType(nodeToValidate);

        assertTrue(result);
    }

    @Test
    public void isAcceptableType_shouldReturnFalse_whenNoNestedSchemaAcceptsTheType() throws Exception {
        UnionSchema schema = new UnionSchema();

        SimpleTypeSchema intSchema = new SimpleTypeSchema();
        intSchema.setType(SimpleType.INTEGER);
        schema.getNestedSchemas().add(intSchema);

        SimpleTypeSchema nullSchema = new SimpleTypeSchema();
        nullSchema.setType(SimpleType.NULL);
        schema.getNestedSchemas().add(nullSchema);

        JsonNode nodeToValidate = new TextNode("");

        boolean result = schema.isAcceptableType(nodeToValidate);

        assertFalse(result);
    }

    @Test
    public void getDescription_shouldReturnUnion() throws Exception {
        UnionSchema schema = new UnionSchema();

        String description = schema.getDescription();

        assertEquals("union", description);
    }
}
