package uk.co.o2.json.schema;

import org.junit.Test;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.TextNode;
import org.codehaus.jackson.node.BooleanNode;
import org.codehaus.jackson.node.BigIntegerNode;
import org.codehaus.jackson.node.DecimalNode;
import org.codehaus.jackson.node.DoubleNode;
import org.codehaus.jackson.node.IntNode;
import org.codehaus.jackson.node.LongNode;
import org.codehaus.jackson.node.NullNode;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.mockito.Mockito.mock;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;

public class SimpleTypeTest {

    @Test
	public void matchesForString_shouldReturnTrue_givenATextualNode() throws Exception {
        JsonNode node = new TextNode("foo");

        assertTrue(SimpleType.STRING.matches(node));
    }

    @Test
	public void matchesForString_shouldReturnFalse_givenANonTextualNode() throws Exception {
        JsonNode node = BooleanNode.TRUE;

        assertFalse(SimpleType.STRING.matches(node));
    }

    @Test
	public void matchesForBoolean_shouldReturnTrue_givenABooleanNode() throws Exception {
        JsonNode node = BooleanNode.FALSE;

        assertTrue(SimpleType.BOOLEAN.matches(node));
    }

    @Test
	public void matchesForBoolean_shouldReturnFalse_givenANonBooleanNode() throws Exception {
        JsonNode node = new TextNode("bar");

        assertFalse(SimpleType.BOOLEAN.matches(node));
    }

    @Test
	public void matchesForNumber_shouldReturnTrue_givenANumericNode() throws Exception {
        assertTrue(SimpleType.NUMBER.matches(new BigIntegerNode(new BigInteger("1"))));
        assertTrue(SimpleType.NUMBER.matches(new DecimalNode(new BigDecimal("1"))));
        assertTrue(SimpleType.NUMBER.matches(new DoubleNode(1)));
        assertTrue(SimpleType.NUMBER.matches(new IntNode(1)));
        assertTrue(SimpleType.NUMBER.matches(new LongNode(1)));
    }

    @Test
	public void matchesForNumber_shouldReturnFalse_givenANonNumberNode() throws Exception {
        JsonNode node = new TextNode("bar");

        assertFalse(SimpleType.NUMBER.matches(node));
    }

    @Test
	public void matchesForInteger_shouldReturnTrue_givenAnIntegerNode() throws Exception {
        assertTrue(SimpleType.INTEGER.matches(new BigIntegerNode(new BigInteger("1"))));
        assertTrue(SimpleType.INTEGER.matches(new IntNode(1)));
        assertTrue(SimpleType.INTEGER.matches(new LongNode(1)));
    }

    @Test
	public void matchesForInteger_shouldReturnFalse_givenANonIntegerNode() throws Exception {
        JsonNode node = new DecimalNode(new BigDecimal("12345.67"));

        assertFalse(SimpleType.INTEGER.matches(node));
    }

    @Test
	public void matchesForNull_shouldReturnTrue_givenANullNode() throws Exception {
        JsonNode Node = NullNode.instance;

        assertTrue(SimpleType.NULL.matches(Node));

    }

    @Test
	public void matchesForNull_shouldReturnFalse_givenANonNullNode() throws Exception {
        JsonNode node = new DecimalNode(new BigDecimal("12345.67"));

        assertFalse(SimpleType.NULL.matches(node));
    }

    @Test
	public void matchesForAny_shouldReturnTrue_givenAnySortOfNodeAtAll() throws Exception {
        JsonNode node = mock(JsonNode.class);

        assertTrue(SimpleType.ANY.matches(node));
    }

    @Test
	public void getValueForString_shouldReturnAString() throws Exception {
        String expectedValue = "value";
        JsonNode node = new TextNode(expectedValue);

        Object result = SimpleType.STRING.getValue(node);

        assertTrue(result instanceof String);
        assertEquals(expectedValue, result);
    }

    @Test
	public void getValueForNumber_shouldReturnANumber() throws Exception {
        BigDecimal expectedValue = new BigDecimal("123.45");
        JsonNode node = new DecimalNode(expectedValue);

        Object result = SimpleType.NUMBER.getValue(node);

        assertTrue(result instanceof Number);
        assertEquals(expectedValue, result);
    }

    @Test
	public void getValueForInteger_shouldReturnAnInteger() throws Exception {
        Integer expectedValue = 12345;
        JsonNode node = new IntNode(expectedValue);

        Object result = SimpleType.INTEGER.getValue(node);

        assertTrue(result instanceof Integer);
        assertEquals(expectedValue, result);
    }

    @Test
	public void getValueForBoolean_shouldReturnABoolean() throws Exception {
        Boolean expectedValue = true;
        JsonNode node = BooleanNode.valueOf(expectedValue);

        Object result = SimpleType.BOOLEAN.getValue(node);

        assertTrue(result instanceof Boolean);
        assertEquals(expectedValue, result);
    }

    @Test(expected=IllegalStateException.class)
	public void getValueForNull_shouldThrowAnException() throws Exception {
        JsonNode node = NullNode.instance;

        SimpleType.NULL.getValue(node);
    }

    @Test(expected=IllegalStateException.class)
	public void getValueForAny_shouldThrowAnException() throws Exception {
        JsonNode node = mock(JsonNode.class);

        SimpleType.ANY.getValue(node);
    }
}
