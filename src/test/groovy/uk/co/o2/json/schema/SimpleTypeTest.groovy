package uk.co.o2.json.schema

import org.junit.Test
import org.codehaus.jackson.JsonNode
import org.codehaus.jackson.node.TextNode
import org.codehaus.jackson.node.BooleanNode
import org.gmock.WithGMock
import org.codehaus.jackson.node.BigIntegerNode
import org.codehaus.jackson.node.DecimalNode
import org.codehaus.jackson.node.DoubleNode
import org.codehaus.jackson.node.IntNode
import org.codehaus.jackson.node.LongNode
import org.codehaus.jackson.node.NullNode

@WithGMock
class SimpleTypeTest {

    @Test
    void matchesForString_shouldReturnTrue_givenATextualNode() {
        JsonNode node = new TextNode('foo')

        assert SimpleType.STRING.matches(node)
    }

    @Test
    void matchesForString_shouldReturnFalse_givenANonTextualNode() {
        JsonNode node = BooleanNode.TRUE

        assert !SimpleType.STRING.matches(node)
    }



    @Test
    void matchesForBoolean_shouldReturnTrue_givenABooleanNode() {
        JsonNode node = BooleanNode.FALSE

        assert SimpleType.BOOLEAN.matches(node)
    }

    @Test
    void matchesForBoolean_shouldReturnFalse_givenANonBooleanNode() {
        JsonNode node = new TextNode('bar')

        assert !SimpleType.BOOLEAN.matches(node)
    }

    @Test
    void matchesForNumber_shouldReturnTrue_givenANumericNode() {
        assert SimpleType.NUMBER.matches(new BigIntegerNode(new BigInteger("1")))
        assert SimpleType.NUMBER.matches(new DecimalNode(new BigDecimal("1")))
        assert SimpleType.NUMBER.matches(new DoubleNode(1))
        assert SimpleType.NUMBER.matches(new IntNode(1))
        assert SimpleType.NUMBER.matches(new LongNode(1))
    }

    @Test
    void matchesForNumber_shouldReturnFalse_givenANonNumberNode() {
        JsonNode node = new TextNode('bar')

        assert !SimpleType.NUMBER.matches(node)
    }

    @Test
    void matchesForInteger_shouldReturnTrue_givenAnIntegerNode() {
        assert SimpleType.INTEGER.matches(new BigIntegerNode(new BigInteger("1")))
        assert SimpleType.INTEGER.matches(new IntNode(1))
        assert SimpleType.INTEGER.matches(new LongNode(1))
    }

    @Test
    void matchesForInteger_shouldReturnFalse_givenANonIntegerNode() {
        JsonNode node = new DecimalNode(new BigDecimal('12345.67'))

        assert !SimpleType.INTEGER.matches(node)
    }

    @Test
    void matchesForNull_shouldReturnTrue_givenANullNode() {
        JsonNode Node = NullNode.instance

        assert SimpleType.NULL.matches(Node)

    }

    @Test
    void matchesForNull_shouldReturnFalse_givenANonNullNode() {
        JsonNode node = new DecimalNode(new BigDecimal('12345.67'))

        assert !SimpleType.NULL.matches(node)
    }

    @Test
    void matchesForAny_shouldReturnTrue_givenAnySortOfNodeAtAll() {
        JsonNode node = mock(JsonNode)

        play {
            assert SimpleType.ANY.matches(node)
        }
    }

    @Test
    void getValueForString_shouldReturnAString() {
        String expectedValue = "value"
        JsonNode node = new TextNode(expectedValue)

        def result = SimpleType.STRING.getValue(node)

        assert result instanceof String
        assert result == expectedValue
    }

    @Test
    void getValueForNumber_shouldReturnANumber() {
        BigDecimal expectedValue = new BigDecimal("123.45")
        JsonNode node = new DecimalNode(expectedValue)

        def result = SimpleType.NUMBER.getValue(node)

        assert result instanceof Number
        assert result == expectedValue
    }

    @Test
    void getValueForInteger_shouldReturnAnInteger() {
        Integer expectedValue = 12345
        JsonNode node = new IntNode(expectedValue)

        def result = SimpleType.INTEGER.getValue(node)

        assert result instanceof Integer
        assert result == expectedValue
    }

    @Test
    void getValueForBoolean_shouldReturnABoolean() {
        Boolean expectedValue = true
        JsonNode node = BooleanNode.valueOf(expectedValue)

        def result = SimpleType.BOOLEAN.getValue(node)

        assert result instanceof Boolean
        assert result == expectedValue
    }

    @Test(expected=IllegalStateException)
    void getValueForNull_shouldThrowAnException() {
        JsonNode node = NullNode.instance

        SimpleType.NULL.getValue(node)
    }

    @Test(expected=IllegalStateException)
    void getValueForAny_shouldThrowAnException() {
        JsonNode node = mock(JsonNode)

        play {
            SimpleType.ANY.getValue(node)
        }
    }
}
