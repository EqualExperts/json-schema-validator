package uk.co.o2.json.schema

import org.junit.Test
import org.codehaus.jackson.JsonNode
import org.codehaus.jackson.map.ObjectMapper
import org.codehaus.jackson.JsonFactory

class ArraySchemaTest {
    private static JsonFactory factory = new JsonFactory(new ObjectMapper())

    @Test
    void validate_shouldEmitAnErrorMessage_givenAJsonObject() {
        JsonNode document = factory.createJsonParser('''{"foo": "bar"}''').readValueAsTree()

        ArraySchema schema = new ArraySchema()

        List<ErrorMessage> result = schema.validate(document)

        assert result.size() == 1
        assert result[0].message.contains('must be an array')
    }

    @Test
    void validate_shouldEmitAnErrorMessage_givenASimpleValue() {
        JsonNode document = factory.createJsonParser('''"foo"''').readValueAsTree()

        ArraySchema schema = new ArraySchema()

        List<ErrorMessage> result = schema.validate(document)

        assert result.size() == 1
        assert result[0].message.contains('must be an array')
    }

    @Test
    void shouldImplementJsonSchema() {
        assert new ArraySchema() instanceof JsonSchema
    }

    @Test
    void validate_shouldAllowAnyArrayItem_whenNoItemSchemaIsSpecified() {
        JsonNode document = factory.createJsonParser('''[
            {"foo": "value1", "bar": 123},
            "string",
            {}
        ]''').readValueAsTree()

        ArraySchema schema = new ArraySchema()

        List<ErrorMessage> result = schema.validate(document)

        assert result.empty
    }

    @Test
    void validate_shouldValidateAllItemsAndCombineAnyErrorMessages() {
        JsonNode document = factory.createJsonParser('''[
            {"foo": "value1", "bar": 123},
            {"foo": true, "bar": "barValue"},
            {}
        ]''').readValueAsTree()

        ArraySchema schema = new ArraySchema(items: new ObjectSchema(properties: [
                new ObjectProperty(name: 'foo', required: true, nestedSchema: new SimpleTypeSchema(type: SimpleType.STRING)),
                new ObjectProperty(name: 'bar', required: true, nestedSchema: new SimpleTypeSchema(type: SimpleType.STRING))
        ]))

        List<ErrorMessage> result = schema.validate(document)

        assert result.size() == 4
        assert result[0].location == '[0].bar'
        assert result[1].location == '[1].foo'
        assert result[2].location == '[2].foo'
        assert result[3].location == '[2].bar'
    }

    @Test
    void validate_shouldNotReturnErrorMessage_givenNoOfItemsInArrayIsLessThanOrEqualToMaxItems() {
        JsonNode document = factory.createJsonParser('''[
            {"foo": "value1", "bar": 123},
            "string",
            {}
        ]''').readValueAsTree()

        ArraySchema schema = new ArraySchema(maxItems: 4)

        List<ErrorMessage> result = schema.validate(document)

        assert result.empty
    }

    @Test
    void validate_shouldReturnErrorMessage_givenNoOfItemsInArrayIsGreaterThanMaxItems() {
        JsonNode document = factory.createJsonParser('''[
            {"foo": "value1", "bar": 123},
            "string",
            {}
        ]''').readValueAsTree()

        ArraySchema schema = new ArraySchema(maxItems: 2)

        List<ErrorMessage> result = schema.validate(document)

        assert result.size() == 1
        assert result[0].location == ''
        assert result[0].message.equals("Current array size of 3 is greater than allowed maximum array size of 2")
    }

    @Test
    void validate_shouldNotReturnErrorMessage_givenNoOfItemsInArrayIsGreaterThanOrEqualToMinItems() {
        JsonNode document = factory.createJsonParser('''[
            {"foo": "value1", "bar": 123},
            "string",
            {}
        ]''').readValueAsTree()

        ArraySchema schema = new ArraySchema(minItems: 2)

        List<ErrorMessage> result = schema.validate(document)

        assert result.empty
    }

    @Test
    void validate_shouldReturnErrorMessage_givenNoOfItemsInArrayIsLessThanMinItems() {
        JsonNode document = factory.createJsonParser('''[
            {"foo": "value1", "bar": 123},
            "string",
            {}
        ]''').readValueAsTree()

        ArraySchema schema = new ArraySchema(minItems: 5)

        List<ErrorMessage> result = schema.validate(document)

        assert result.size() == 1
        assert result[0].location == ''
        assert result[0].message.equals("Current array size of 3 is less than allowed minimum array size of 5")
    }

    @Test
    void validate_shouldNotEmitAnErrorMessage_whenDuplicateElementsArePresentAndUniqueItemsIsFalse() {
        JsonNode document = factory.createJsonParser('''["foo", 1, 2, 3, "foo"]''').readValueAsTree()
        ArraySchema schema = new ArraySchema(uniqueItems: false)

        List<ErrorMessage> result = schema.validate(document)

        assert result.empty
    }

    @Test
    void validate_shouldEmitAnErrorMessage_whenDuplicateElementsArePresentAndUniqueItemsIsTrue() {
        JsonNode document = factory.createJsonParser('''["foo", 1, 2, 3, "foo"]''').readValueAsTree()
        ArraySchema schema = new ArraySchema(uniqueItems: true)

        List<ErrorMessage> result = schema.validate(document)

        assert result.size() == 1
        assert result[0].location == '[4]'
        assert result[0].message == "Duplicate array element"
    }

    @Test
    void validate_shouldEmitAnErrorMessageForEachDuplicateElement_whenDuplicateElementsArePresentAndUniqueItemsIsTrue() {
        JsonNode document = factory.createJsonParser('''["foo", "foo", "foo"]''').readValueAsTree()
        ArraySchema schema = new ArraySchema(uniqueItems: true)

        List<ErrorMessage> result = schema.validate(document)

        assert result.size() == 2
        assert result[0].location == '[1]'
        assert result[0].message == "Duplicate array element"
        assert result[1].location == '[2]'
        assert result[1].message == "Duplicate array element"
    }

    @Test
    void validate_shouldNotEmitAnErrorMessage_whenUniqueItemsIsTrueAndThereAreNoUniqueElements() {
        JsonNode document = factory.createJsonParser('''[1, "1"]''').readValueAsTree()
        ArraySchema schema = new ArraySchema(uniqueItems: true)

        List<ErrorMessage> result = schema.validate(document)

        assert result.empty
    }

    @Test
    void validate_shouldNotEmitAnErrorMessage_whenDuplicateElementsArePresentAndUniqueItemsIsNotSet() {
        JsonNode document = factory.createJsonParser('''["foo", 1, 2, 3, "foo"]''').readValueAsTree()
        ArraySchema schema = new ArraySchema()

        List<ErrorMessage> result = schema.validate(document)

        assert result.empty
    }
}
