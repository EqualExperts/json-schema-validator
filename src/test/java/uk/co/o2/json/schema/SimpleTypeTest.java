package uk.co.o2.json.schema;

import org.junit.Test;

import javax.json.Json;
import javax.json.JsonValue;
import java.math.BigDecimal;
import java.math.BigInteger;

import static org.mockito.Mockito.mock;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;

public class SimpleTypeTest {

    @Test
	public void matchesForString_shouldReturnTrue_givenATextualNode() throws Exception {
        JsonValue node = Json.createArrayBuilder().add("foo").build().get(0);

        assertTrue(SimpleType.STRING.matches(node));
    }

    @Test
	public void matchesForString_shouldReturnFalse_givenANonTextualNode() throws Exception {
        JsonValue node = JsonValue.TRUE;

        assertFalse(SimpleType.STRING.matches(node));
    }

    @Test
	public void matchesForBoolean_shouldReturnTrue_givenABooleanNode() throws Exception {
        JsonValue node = JsonValue.FALSE;

        assertTrue(SimpleType.BOOLEAN.matches(node));
    }

    @Test
	public void matchesForBoolean_shouldReturnFalse_givenANonBooleanNode() throws Exception {
        JsonValue node = Json.createArrayBuilder().add("bar").build().get(0);

        assertFalse(SimpleType.BOOLEAN.matches(node));
    }

    @Test
	public void matchesForNumber_shouldReturnTrue_givenANumericNode() throws Exception {
        assertTrue(SimpleType.NUMBER.matches(Json.createArrayBuilder().add(new BigInteger("1")).build().get(0)));
        assertTrue(SimpleType.NUMBER.matches(Json.createArrayBuilder().add(new BigDecimal("1")).build().get(0)));
        assertTrue(SimpleType.NUMBER.matches(Json.createArrayBuilder().add(1d).build().get(0)));
        assertTrue(SimpleType.NUMBER.matches(Json.createArrayBuilder().add(1).build().get(0)));
        assertTrue(SimpleType.NUMBER.matches(Json.createArrayBuilder().add(1L).build().get(0)));
    }

    @Test
	public void matchesForNumber_shouldReturnFalse_givenANonNumberNode() throws Exception {
        JsonValue node = Json.createArrayBuilder().add("bar").build().get(0);

        assertFalse(SimpleType.NUMBER.matches(node));
    }

    @Test
	public void matchesForInteger_shouldReturnTrue_givenAnIntegerNode() throws Exception {

        assertTrue(SimpleType.INTEGER.matches(Json.createArrayBuilder().add(new BigInteger("1")).build().get(0)));
        assertTrue(SimpleType.INTEGER.matches(Json.createArrayBuilder().add(1).build().get(0)));
        assertTrue(SimpleType.INTEGER.matches(Json.createArrayBuilder().add(1L).build().get(0)));

    }

    @Test
	public void matchesForInteger_shouldReturnFalse_givenANonIntegerNode() throws Exception {
        JsonValue node = Json.createArrayBuilder().add("12345.67").build().get(0);

        assertFalse(SimpleType.INTEGER.matches(node));
    }

    @Test
	public void matchesForNull_shouldReturnTrue_givenANullNode() throws Exception {
        JsonValue node = JsonValue.NULL;

        assertTrue(SimpleType.NULL.matches(node));

    }

    @Test
	public void matchesForNull_shouldReturnFalse_givenANonNullNode() throws Exception {
        JsonValue node = Json.createArrayBuilder().add(new BigDecimal("12345.67")).build().get(0);

        assertFalse(SimpleType.NULL.matches(node));
    }

    @Test
	public void matchesForAny_shouldReturnTrue_givenAnySortOfNodeAtAll() throws Exception {
        JsonValue node = mock(JsonValue.class);

        assertTrue(SimpleType.ANY.matches(node));
    }

    @Test
	public void getValueForString_shouldReturnAString() throws Exception {
        String expectedValue = "value";
        JsonValue node = Json.createArrayBuilder().add(expectedValue).build().get(0);

        Object result = SimpleType.STRING.getValue(node);

        assertTrue(result instanceof String);
        assertEquals(expectedValue, result);
    }

    @Test
	public void getValueForNumber_shouldReturnANumber() throws Exception {
        BigDecimal expectedValue = new BigDecimal("123.45");
        JsonValue node = Json.createArrayBuilder().add(expectedValue).build().get(0);

        Object result = SimpleType.NUMBER.getValue(node);

        assertTrue(result instanceof Number);
        assertEquals(expectedValue, result);
    }

    @Test
	public void getValueForInteger_shouldReturnAnInteger() throws Exception {
        Integer expectedValue = 12345;
        JsonValue node = Json.createArrayBuilder().add(expectedValue).build().get(0);

        Object result = SimpleType.INTEGER.getValue(node);

        assertTrue(result instanceof Integer);
        assertEquals(expectedValue, result);
    }

    @Test
	public void getValueForBoolean_shouldReturnABoolean() throws Exception {
        JsonValue node = JsonValue.TRUE;

        Object result = SimpleType.BOOLEAN.getValue(node);

        assertTrue(result instanceof Boolean);
        assertEquals(true, result);
    }

    @Test(expected=IllegalStateException.class)
	public void getValueForNull_shouldThrowAnException() throws Exception {
        JsonValue node = JsonValue.NULL;

        SimpleType.NULL.getValue(node);
    }

    @Test(expected=IllegalStateException.class)
	public void getValueForAny_shouldThrowAnException() throws Exception {
        JsonValue node = mock(JsonValue.class);

        SimpleType.ANY.getValue(node);
    }
}
