package uk.co.o2.json.schema

import org.junit.Test
import org.codehaus.jackson.JsonFactory
import org.codehaus.jackson.JsonNode
import org.codehaus.jackson.map.ObjectMapper
import org.gmock.WithGMock

@WithGMock
class ObjectSchemaTest {
    private static JsonFactory factory = new JsonFactory(new ObjectMapper())

    @Test
    void validate_shouldValidateAllPropertiesAndCombineAnyErrorMessages() {
        JsonNode document = factory.createJsonParser('''{
            "foo": "a",
            "bar": {
                "baz": 1
            }
        }''').readValueAsTree()

        ObjectSchema schema = new ObjectSchema(properties: [
            new ObjectProperty(name: 'foo', nestedSchema: new SimpleTypeSchema(type: SimpleType.NUMBER)),
            new ObjectProperty(name: 'bar', nestedSchema:
                new ObjectSchema(properties: [
                    new ObjectProperty(name: 'quux', nestedSchema: new SimpleTypeSchema(type: SimpleType.NUMBER), required: true),
                    new ObjectProperty(name: 'baz', nestedSchema: new SimpleTypeSchema(type: SimpleType.STRING))
                ])
            )
        ])

        List<ErrorMessage> result = schema.validate(document)

        assert result.size() == 3
        assert result[0].location == 'foo'
        assert result[1].location == 'bar.quux'
        assert result[2].location == 'bar.baz'
    }

    @Test
    void validate_shouldEmitAnErrorMessage_givenAJsonArray() {
        JsonNode document = factory.createJsonParser('''[ {"foo": "bar"} ]''').readValueAsTree()

        ObjectSchema schema = new ObjectSchema()

        List<ErrorMessage> result = schema.validate(document)

        assert result.size() == 1
        assert result[0].message.contains('must be an object')
    }

    @Test
    void validate_shouldEmitAnErrorMessage_givenASimpleValue() {
        JsonNode document = factory.createJsonParser('''"foo"''').readValueAsTree()

        ObjectSchema schema = new ObjectSchema()

        List<ErrorMessage> result = schema.validate(document)

        assert result.size() == 1
        assert result[0].message.contains('must be an object')
    }

    @Test
    void validate_shouldReportAnError_whenARequiredPropertyIsMissing() {
        JsonNode document = factory.createJsonParser('''{"foo": "bar"}''').readValueAsTree()

        ObjectSchema schema = new ObjectSchema(properties: [
            new ObjectProperty(name: 'id', required: true),
            new ObjectProperty(name: 'missingObject', required: true, nestedSchema: new ObjectSchema())
        ])

        List<ErrorMessage> result = schema.validate(document)

        assert result.size() == 2
        assert result[0].message.contains('required')
        assert result[0].message.contains('id')
        assert result[0].location == 'id'
        assert result[1].message.contains('required')
        assert result[1].message.contains('missingObject')
        assert result[1].location == 'missingObject'
    }

    @Test
    void validate_shouldNotReportAnError_whenARequiredPropertyIsPresent() {
        JsonNode document = factory.createJsonParser('''{"id": "value", "foo": {}}''').readValueAsTree()

        ObjectSchema schema = new ObjectSchema(properties: [
            new ObjectProperty(name: 'id', required: true),
            new ObjectProperty(name: 'foo', required: true, nestedSchema: new ObjectSchema())
        ])

        List<ErrorMessage> result = schema.validate(document)

        assert result.empty
    }

    @Test
    void validate_shouldNotReportAnError_whenANonRequiredPropertyIsNotPresent() {
        JsonNode document = factory.createJsonParser('''{"foo": "bar"}''').readValueAsTree()

        ObjectSchema schema = new ObjectSchema(properties: [
            new ObjectProperty(name: 'id', required: false),
            new ObjectProperty(name: 'missingObject', required: false, nestedSchema: new ObjectSchema())
        ])

        List<ErrorMessage> result = schema.validate(document)

        assert result.empty
    }

    @Test
    void validate_shouldReportAnError_whenAnAdditionalPropertyIsFoundThatDoesNotConformToSchema() {
        JsonNode document = factory.createJsonParser("""{
            "additionalProperty": "NotReallyMuchOfANumber",
            "additionalProperty2": "Also Wrong"
        }""").readValueAsTree()

        ObjectSchema schema = new ObjectSchema(additionalProperties: new SimpleTypeSchema(type: SimpleType.NUMBER))

        List<ErrorMessage> result = schema.validate(document)

        assert result.size() == 2
        assert result[0].location == 'additionalProperty'
        assert result[0].message.toLowerCase().contains('invalid type')
        assert result[1].location == 'additionalProperty2'
        assert result[1].message.toLowerCase().contains('invalid type')
    }

    @Test
    void validate_shouldNotReportAnError_whenAnAdditionalPropertyIsFoundAndAllAdditionalPropertiesAreAllowed() {
        JsonNode document = factory.createJsonParser("""{
            "additionalProperty": "NotReallyMuchOfANumber",
            "additionalProperty2": "Also Wrong"
        }""").readValueAsTree()

        ObjectSchema schema = new ObjectSchema(additionalProperties: ObjectSchema.ALLOW_ALL_ADDITIONAL_PROPERTIES)

        List<ErrorMessage> result = schema.validate(document)

        assert result.size() == 0
    }

    @Test
    void validate_shouldReportAnError_whenAnAdditionalPropertyIsFoundAndNoAdditionalPropertiesAreAllowed() {
        JsonNode document = factory.createJsonParser("""{
            "additionalProperty": "NotReallyMuchOfANumber"
        }""").readValueAsTree()

        ObjectSchema schema = new ObjectSchema(additionalProperties: ObjectSchema.FORBID_ANY_ADDITIONAL_PROPERTIES)

        List<ErrorMessage> result = schema.validate(document)

        assert result.size() == 1
        assert result[0].location == 'additionalProperty'
        assert result[0].message.toLowerCase().contains('unexpected property')
    }

    @Test
    void shouldImplementJsonSchema() {
        assert new ObjectSchema() instanceof JsonSchema
    }
}
