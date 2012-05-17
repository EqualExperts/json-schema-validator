package uk.co.o2.json.schema

import java.util.regex.Pattern
import org.codehaus.jackson.JsonFactory
import org.codehaus.jackson.JsonNode
import org.codehaus.jackson.map.ObjectMapper
import org.junit.Test
import org.codehaus.jackson.node.*
import static org.junit.Assert.fail

class SimpleTypeSchemaTest {
    private static JsonFactory factory = new JsonFactory(new ObjectMapper())

    @Test
    void validate_shouldReturnAnErrorMessage_givenAnIncompatibleType() {
        JsonNode document = factory.createJsonParser('''"abc123"''').readValueAsTree()

        List<ErrorMessage> result = new SimpleTypeSchema(type: SimpleType.NUMBER).validate(document)

        assert result.size() == 1
        assert result[0].location == ''
        assert result[0].message.toLowerCase().contains('invalid type')
    }

    @Test
    void validate_shouldNotReturnAnErrorMessage_givenACompatibleType() {
        JsonNode document = factory.createJsonParser('''12345''').readValueAsTree()

        List<ErrorMessage> result = new SimpleTypeSchema(type: SimpleType.NUMBER).validate(document)

        assert result.empty
    }

    @Test(expected = IllegalArgumentException)
    void setPattern_shouldThrowAnException_whenAPatternIsSpecifiedForATypeOtherThanString() {
        SimpleTypeSchema schema = new SimpleTypeSchema(type: SimpleType.NUMBER)
        schema.pattern = ~/\d+/
    }

    @Test(expected = IllegalArgumentException)
    void setType_shouldThrowAnException_givenATypeOtherThanString_whenAPatternHasBeenSet() {
        SimpleTypeSchema schema = new SimpleTypeSchema(type: SimpleType.STRING, pattern: ~/\d+/)
        schema.type = SimpleType.NUMBER
    }

    @Test
    void validate_shouldNotPerformRegexValidation_whenAPatternIsNotProvided() {
        SimpleTypeSchema schema = new SimpleTypeSchema(type: SimpleType.STRING, pattern: null)

        List<ErrorMessage> result = schema.validate(new TextNode("blah"))

        assert result.empty
    }

    @Test
    void validate_shouldReturnAnErrorMessage_whenAStringDoesNotMatchTheProvidedRegex() {
        Pattern expectedRegex = ~/\d+/
        String jsonStringValue = "DoesNotMatchTheRegex"

        SimpleTypeSchema schema = new SimpleTypeSchema(type: SimpleType.STRING, pattern: expectedRegex)
        JsonNode nodeToValidate = new TextNode(jsonStringValue)

        List<ErrorMessage> result = schema.validate(nodeToValidate)

        assert result.size() == 1
        assert result[0].location == ''
        assert result[0].message.contains(jsonStringValue)
        assert result[0].message.contains('does not match')
        assert result[0].message.contains(expectedRegex.pattern())
    }

    @Test
    void validate_shouldNotReturnAnyErrorMessages_whenAStringDoesMatchTheProvidedRegex() {
        SimpleTypeSchema schema = new SimpleTypeSchema(type: SimpleType.STRING, pattern: ~/.+/)
        JsonNode nodeToValidate = new TextNode("Anything should match this regex")

        List<ErrorMessage> result = schema.validate(nodeToValidate)

        assert result.empty
    }

    @Test
    void shouldImplementJsonSchema() {
        assert new SimpleTypeSchema() instanceof JsonSchema
    }

    @Test
    void validate_shouldNotReturnAnErrorMessage_givenAFormatOfDateTimeAndAStringValueThatIsAIso8601DateTime() {
        SimpleTypeSchema schema = new SimpleTypeSchema(type: SimpleType.STRING, format: "date-time")
        JsonNode nodeToValidate = new TextNode("2011-05-10T11:11:17Z")

        List<ErrorMessage> result = schema.validate(nodeToValidate)

        assert result.empty
    }

    @Test
    void validate_shouldReturnAnErrorMessage_givenAFormatOfDateTimeAndAStringValueThatIsNotAValidIso8601DateTime() {
        String invalidDateTime = "2011-May-10T165:11:17z"
        SimpleTypeSchema schema = new SimpleTypeSchema(type: SimpleType.STRING, format: "date-time")
        JsonNode nodeToValidate = new TextNode(invalidDateTime)

        List<ErrorMessage> result = schema.validate(nodeToValidate)

        assert result.size() == 1
        assert result[0].location == ''
        assert result[0].message.contains(invalidDateTime)
        assert result[0].message.contains('date-time')
    }

    @Test(expected = IllegalArgumentException)
    void setFormat_shouldThrowAnException_whenTypeIsNotStringAndFormatIsDateTime() {
        new SimpleTypeSchema(type: SimpleType.NUMBER, format: "date-time")
    }

    @Test
    void validate_shouldNotReturnAnErrorMessage_givenAFormatOfDateAndAStringValueThatIsAValidDate() {
        SimpleTypeSchema schema = new SimpleTypeSchema(type: SimpleType.STRING, format: "date")
        JsonNode nodeToValidate = new TextNode("2011-05-10")

        List<ErrorMessage> result = schema.validate(nodeToValidate)

        assert result.empty
    }

    @Test
    void validate_shouldReturnAnErrorMessage_givenAFormatOfDateAndAStringValueThatIsNotAValidDate() {
        String invalidDate = "2011-May-10"
        SimpleTypeSchema schema = new SimpleTypeSchema(type: SimpleType.STRING, format: "date")
        JsonNode nodeToValidate = new TextNode(invalidDate)

        List<ErrorMessage> result = schema.validate(nodeToValidate)

        assert result.size() == 1
        assert result[0].location == ''
        assert result[0].message.contains(invalidDate)
        assert result[0].message.contains('date')
    }

    @Test
    void validate_shouldReturnAnErrorMessage_givenAFormatOfDateAndAStringValueThatIsAFullDateTime() {
        String invalidDate = "2011-05-10T11:47:16Z"
        SimpleTypeSchema schema = new SimpleTypeSchema(type: SimpleType.STRING, format: "date")
        JsonNode nodeToValidate = new TextNode(invalidDate)

        List<ErrorMessage> result = schema.validate(nodeToValidate)

        assert result.size() == 1
        assert result[0].location == ''
        assert result[0].message.contains(invalidDate)
        assert result[0].message.contains('date')
    }

    @Test(expected = IllegalArgumentException)
    void setFormat_shouldThrowAnException_whenTypeIsNotStringAndFormatIsDate() {
        new SimpleTypeSchema(type: SimpleType.NUMBER, format: "date")
    }

    @Test
    void validate_shouldNotReturnAnErrorMessage_givenAFormatOfTimeAndAStringValueThatIsAValidTime() {
        SimpleTypeSchema schema = new SimpleTypeSchema(type: SimpleType.STRING, format: "time")
        JsonNode nodeToValidate = new TextNode("13:15:47")

        List<ErrorMessage> result = schema.validate(nodeToValidate)

        assert result.empty
    }

    @Test
    void validate_shouldReturnAnErrorMessage_givenAFormatOfTimeAndAStringValueThatIsNotAValidTime() {
        String invalidTime = "2011-05-10"
        SimpleTypeSchema schema = new SimpleTypeSchema(type: SimpleType.STRING, format: "time")
        JsonNode nodeToValidate = new TextNode(invalidTime)

        List<ErrorMessage> result = schema.validate(nodeToValidate)

        assert result.size() == 1
        assert result[0].location == ''
        assert result[0].message.contains(invalidTime)
        assert result[0].message.contains('time')
    }

    @Test
    void validate_shouldReturnAnErrorMessage_givenAFormatOfTimeAndAStringValueThatHasExtraInformationAfterTheValidTime() {
        String invalidTime = "11:47:16-blah"
        SimpleTypeSchema schema = new SimpleTypeSchema(type: SimpleType.STRING, format: "time")
        JsonNode nodeToValidate = new TextNode(invalidTime)

        List<ErrorMessage> result = schema.validate(nodeToValidate)

        assert result.size() == 1
        assert result[0].location == ''
        assert result[0].message.contains(invalidTime)
        assert result[0].message.contains('time')
    }

    @Test(expected = IllegalArgumentException)
    void setFormat_shouldThrowAnException_whenTypeIsNotStringAndFormatIsTime() {
        new SimpleTypeSchema(type: SimpleType.NUMBER, format: "time")
    }

    @Test
    void validate_shouldNotReturnAnErrorMessage_givenAFormatOfRegexAndAStringValueThatIsAValidRegex() {
        SimpleTypeSchema schema = new SimpleTypeSchema(type: SimpleType.STRING, format: "regex")
        JsonNode nodeToValidate = new TextNode(".*")

        List<ErrorMessage> result = schema.validate(nodeToValidate)

        assert result.empty
    }

    @Test
    void validate_shouldReturnAnErrorMessage_givenAFormatOfRegexAndAStringValueThatIsNotAValidRegex() {
        String invalidRegex = "+"
        SimpleTypeSchema schema = new SimpleTypeSchema(type: SimpleType.STRING, format: "regex")
        JsonNode nodeToValidate = new TextNode(invalidRegex)

        List<ErrorMessage> result = schema.validate(nodeToValidate)

        assert result.size() == 1
        assert result[0].location == ''
        assert result[0].message.contains(invalidRegex)
        assert result[0].message.contains('regex')
    }

    @Test(expected = IllegalArgumentException)
    void setFormat_shouldThrowAnException_whenTypeIsNotStringAndFormatIsRegex() {
        new SimpleTypeSchema(type: SimpleType.NUMBER, format: "regex")
    }

    @Test
    void validate_shouldNotReturnAnErrorMessage_givenAFormatOfUriAndAStringValueThatIsAValidUri() {
        SimpleTypeSchema schema = new SimpleTypeSchema(type: SimpleType.STRING, format: "uri")
        JsonNode nodeToValidate = new TextNode("http://www.example.com")

        List<ErrorMessage> result = schema.validate(nodeToValidate)

        assert result.empty
    }

    @Test
    void validate_shouldReturnAnErrorMessage_givenAFormatOfUriAndAStringValueThatIsNotAValidUri() {
        String invalidUri = ":this-isnt-a-valid-uri" //it's surprisingly difficult to NOT be a valid URI
        SimpleTypeSchema schema = new SimpleTypeSchema(type: SimpleType.STRING, format: "uri")
        JsonNode nodeToValidate = new TextNode(invalidUri)

        List<ErrorMessage> result = schema.validate(nodeToValidate)

        assert result.size() == 1
        assert result[0].location == ''
        assert result[0].message.contains(invalidUri)
        assert result[0].message.contains('uri')
    }

    @Test(expected = IllegalArgumentException)
    void setFormat_shouldThrowAnException_whenTypeIsNotStringAndFormatIsUri() {
        new SimpleTypeSchema(type: SimpleType.NUMBER, format: "uri")
    }

    @Test
    void validate_shouldNotReturnAnErrorMessage_givenAnUnknownFormatAndAnyOtherwiseValidValue() {
        SimpleTypeSchema schema = new SimpleTypeSchema(format: "custom-format")
        JsonNode nodeToValidate = new TextNode('I am a valid custom-format instance, but it\'s not possible to check')

        List<ErrorMessage> result = schema.validate(nodeToValidate)

        assert result.empty
    }

    @Test
    void validate_shouldNotReturnAnErrorMessage_givenAFormatOfUtcMillisecAndANumberValue() {
        SimpleTypeSchema schema = new SimpleTypeSchema(type: SimpleType.NUMBER, format: "utc-millisec")
        JsonNode nodeToValidate = new LongNode(12345L)

        List<ErrorMessage> result = schema.validate(nodeToValidate)

        assert result.empty
    }

    @Test
    void validate_shouldNotReturnAnErrorMessage_givenAFormatOfUtcMillisecAndAnIntegerValue() {
        SimpleTypeSchema schema = new SimpleTypeSchema(type: SimpleType.INTEGER, format: "utc-millisec")
        JsonNode nodeToValidate = new IntNode(12345)

        List<ErrorMessage> result = schema.validate(nodeToValidate)

        assert result.empty
    }

    @Test
    void validate_shouldReturnAnErrorMessage_givenAStringThatIsLongerThanAMaxLength() {
        def invalidString = "1234567890XXXX"
        SimpleTypeSchema schema = new SimpleTypeSchema(type: SimpleType.STRING, maxLength: 10)
        JsonNode nodeToValidate = new TextNode(invalidString)

        List<ErrorMessage> result = schema.validate(nodeToValidate)

        assert result.size() == 1
        assert result[0].location == ''
        assert result[0].message.contains("10")
        assert result[0].message.contains(invalidString)

    }

    @Test
    void validate_shouldReturnAnErrorMessage_givenAStringThatIsLongerThanAMaxLengthOfZero() {
        def invalidString = "1234567890XXXX"
        SimpleTypeSchema schema = new SimpleTypeSchema(type: SimpleType.STRING, maxLength: 0)
        JsonNode nodeToValidate = new TextNode(invalidString)

        List<ErrorMessage> result = schema.validate(nodeToValidate)

        assert result.size() == 1
        assert result[0].location == ''
        assert result[0].message.contains("0")
        assert result[0].message.contains(invalidString)

    }

    @Test
    void validate_shouldNotReturnAnErrorMessage_givenAValueEqualToMaximum() {
        SimpleTypeSchema schema = new SimpleTypeSchema(type: SimpleType.NUMBER, maximum: 20)
        JsonNode nodeToValidate = new DoubleNode(20)

        List<ErrorMessage> result = schema.validate(nodeToValidate)

        assert result.empty

    }


    @Test
    void validate_shouldNotReturnAnErrorMessage_givenAStringThatIsLesserThanOrEqualToMaxLength() {
        SimpleTypeSchema schema = new SimpleTypeSchema(type: SimpleType.STRING, maxLength: 10)
        JsonNode nodeToValidate = new TextNode("123")

        List<ErrorMessage> result = schema.validate(nodeToValidate)

        assert result.empty
    }

    @Test(expected = IllegalArgumentException)
    void setMaxLength_shouldThrowAnException_givenATypeOtherThenString() {
        new SimpleTypeSchema(type: SimpleType.INTEGER, maxLength: 10)
    }


    @Test
    void validate_shouldReturnAnErrorMessage_givenAStringThatIsLessThanAMinLength() {
        def invalidString = "1"
        SimpleTypeSchema schema = new SimpleTypeSchema(type: SimpleType.STRING, minLength: 2)
        JsonNode nodeToValidate = new TextNode(invalidString)

        List<ErrorMessage> result = schema.validate(nodeToValidate)

        assert result.size() == 1
        assert result[0].location == ''
        assert result[0].message.contains("2")
        assert result[0].message.contains(invalidString)

    }

    @Test
    void validate_shouldNotReturnAnErrorMessage_givenAStringThatIsGreaterThanOrEqualToMinLength() {
        SimpleTypeSchema schema = new SimpleTypeSchema(type: SimpleType.STRING, minLength: 2)
        JsonNode nodeToValidate = new TextNode("12")

        List<ErrorMessage> result = schema.validate(nodeToValidate)

        assert result.empty
    }

    @Test(expected = IllegalArgumentException)
    void setMinLength_shouldThrowAnException_givenATypeOtherThenString() {
        new SimpleTypeSchema(type: SimpleType.INTEGER, minLength: 10)
    }

    @Test
    void validate_shouldReturnAnErrorMessage_givenANumberThatIsLessThanAMinimumValue() {
        double invalidValue = 1.5
        SimpleTypeSchema schema = new SimpleTypeSchema(type: SimpleType.NUMBER, minimum: 2)
        JsonNode nodeToValidate = new DoubleNode(invalidValue)

        List<ErrorMessage> result = schema.validate(nodeToValidate)

        assert result.size() == 1
        assert result[0].location == ''
        assert result[0].message.contains("2")
        assert result[0].message.contains(invalidValue.toString())

    }

    @Test
    void validate_shouldReturnAnErrorMessage_givenAnIntegerThatIsLessThanAMinimumValueOfZero() {
        int invalidValue = -2
        SimpleTypeSchema schema = new SimpleTypeSchema(type: SimpleType.INTEGER, minimum: 0)
        JsonNode nodeToValidate = new IntNode(invalidValue)

        List<ErrorMessage> result = schema.validate(nodeToValidate)

        assert result.size() == 1
        assert result[0].location == ''
        assert result[0].message.contains("0")
        assert result[0].message.contains(invalidValue.toString())

    }

    @Test
    void validate_shouldNotReturnAnErrorMessage_givenANumberThatIsGreaterThanOrEqualToMinimum() {
        SimpleTypeSchema schema = new SimpleTypeSchema(type: SimpleType.NUMBER, minimum: 2)
        JsonNode nodeToValidate = new DoubleNode(2.5)

        List<ErrorMessage> result = schema.validate(nodeToValidate)

        assert result.empty
    }


    @Test
    void validate_shouldReturnAnErrorMessage_givenAnIntegerThatIsLessThanAMinimumValue() {
        int invalidInt = 1
        SimpleTypeSchema schema = new SimpleTypeSchema(type: SimpleType.INTEGER, minimum: 10)
        JsonNode nodeToValidate = new IntNode(invalidInt)

        List<ErrorMessage> result = schema.validate(nodeToValidate)

        assert result.size() == 1
        assert result[0].location == ''
        assert result[0].message.contains("10")
        assert result[0].message.contains(invalidInt.toString())

    }

    @Test
    void validate_shouldNotReturnAnErrorMessage_givenAnIntegerThatIsGreaterThanOrEqualToMinimum() {
        SimpleTypeSchema schema = new SimpleTypeSchema(type: SimpleType.INTEGER, minimum: 2)
        JsonNode nodeToValidate = new IntNode(3)

        List<ErrorMessage> result = schema.validate(nodeToValidate)

        assert result.empty
    }

    @Test
    void setMinimum_shouldThrowAnException_givenATypeOtherThenNumberOrInteger() {
        SimpleTypeSchema number = new SimpleTypeSchema(type: SimpleType.NUMBER, minimum: 10)
        SimpleTypeSchema integer = new SimpleTypeSchema(type: SimpleType.INTEGER, minimum: 10)
        try {
            SimpleTypeSchema string = new SimpleTypeSchema(type: SimpleType.STRING, minimum: 10)
            fail()
        } catch (IllegalArgumentException e) {
            // good
        }
        try {
            SimpleTypeSchema booleanValue = new SimpleTypeSchema(type: SimpleType.BOOLEAN, minimum: 10)
            fail()
        } catch (IllegalArgumentException e) {
            // good
        }

    }

    @Test
    void validate_shouldReturnAnErrorMessage_givenANumberThatIsGreaterThanMaximumValue() {
        double invalidValue = 2.5
        SimpleTypeSchema schema = new SimpleTypeSchema(type: SimpleType.NUMBER, maximum: 2)
        JsonNode nodeToValidate = new DoubleNode(invalidValue)

        List<ErrorMessage> result = schema.validate(nodeToValidate)

        assert result.size() == 1
        assert result[0].location == ''
        assert result[0].message.contains("2")
        assert result[0].message.contains(invalidValue.toString())

    }

    @Test
    void validate_shouldReturnAnErrorMessage_givenANumberThatIsGreaterThanMaximumValueOfZero() {
        double invalidValue = 2.5
        SimpleTypeSchema schema = new SimpleTypeSchema(type: SimpleType.NUMBER, maximum: 0)
        JsonNode nodeToValidate = new DoubleNode(invalidValue)

        List<ErrorMessage> result = schema.validate(nodeToValidate)

        assert result.size() == 1
        assert result[0].location == ''
        assert result[0].message.contains("0")
        assert result[0].message.contains(invalidValue.toString())

    }

    @Test
    void validate_shouldNotReturnAnErrorMessage_givenANumberThatIsLesserThanOrEqualToMaximum() {
        SimpleTypeSchema schema = new SimpleTypeSchema(type: SimpleType.NUMBER, maximum: 2)
        JsonNode nodeToValidate = new DoubleNode(1.5)

        List<ErrorMessage> result = schema.validate(nodeToValidate)

        assert result.empty
    }

    @Test
    void validate_shouldNotReturnAnErrorMessage_givenAStringLengthThatIsEqualToMaxLength() {
        SimpleTypeSchema schema = new SimpleTypeSchema(type: SimpleType.STRING, maxLength: 5)
        JsonNode nodeToValidate = new TextNode("abcde")

        List<ErrorMessage> result = schema.validate(nodeToValidate)

        assert result.empty
    }


    @Test
    void validate_shouldReturnAnErrorMessage_givenAnIntegerThatIsGreaterThanAMaximumValue() {
        int invalidInt = 11
        SimpleTypeSchema schema = new SimpleTypeSchema(type: SimpleType.INTEGER, maximum: 10)
        JsonNode nodeToValidate = new IntNode(invalidInt)

        List<ErrorMessage> result = schema.validate(nodeToValidate)

        assert result.size() == 1
        assert result[0].location == ''
        assert result[0].message.contains("10")
        assert result[0].message.contains(invalidInt.toString())

    }

    @Test
    void validate_shouldNotReturnAnErrorMessage_givenAnIntegerThatIsLesserThanOrEqualToMaximum() {
        SimpleTypeSchema schema = new SimpleTypeSchema(type: SimpleType.INTEGER, maximum: 2)
        JsonNode nodeToValidate = new IntNode(1)

        List<ErrorMessage> result = schema.validate(nodeToValidate)

        assert result.empty
    }

    @Test
    void setMaximum_shouldThrowAnException_givenATypeOtherThenNumberOrInteger() {
        SimpleTypeSchema number = new SimpleTypeSchema(type: SimpleType.NUMBER, maximum: 10)
        SimpleTypeSchema integer = new SimpleTypeSchema(type: SimpleType.INTEGER, maximum: 10)
        try {
            SimpleTypeSchema string = new SimpleTypeSchema(type: SimpleType.STRING, maximum: 10)
            fail()
        } catch (IllegalArgumentException e) {
            // good
        }
        try {
            SimpleTypeSchema booleanValue = new SimpleTypeSchema(type: SimpleType.BOOLEAN, maximum: 10)
            fail()
        } catch (IllegalArgumentException e) {
            // good
        }

    }

    @Test
    void validate_shouldReturnAnErrorMessage_givenAnIntegerEqualToMinimumWhenExclusiveMinimumIsTrue() {
        int invalidInt = 11
        SimpleTypeSchema schema = new SimpleTypeSchema(type: SimpleType.INTEGER, minimum: 11, exclusiveMinimum: true)
        JsonNode nodeToValidate = new IntNode(invalidInt)

        List<ErrorMessage> result = schema.validate(nodeToValidate)

        assert result.size() == 1
        assert result[0].location == ''
        assert result[0].message.contains("11")
        assert result[0].message.contains("exclusiveMinimum")

    }

    @Test
    void validate_shouldReturnAnErrorMessage_givenANumberEqualToMinimumWhenExclusiveMinimumIsTrue() {
        double invalidValue = 11.999
        SimpleTypeSchema schema = new SimpleTypeSchema(type: SimpleType.NUMBER, minimum: 11.999, exclusiveMinimum: true)
        JsonNode nodeToValidate = new DoubleNode(invalidValue)

        List<ErrorMessage> result = schema.validate(nodeToValidate)

        assert result.size() == 1
        assert result[0].location == ''
        assert result[0].message.contains("11.999")
        assert result[0].message.contains("exclusiveMinimum")
    }

    @Test
    void validate_shouldReturnAnErrorMessage_givenANumberLessThanMinimumWhenExclusiveMinimumIsTrue() {
        double invalidValue = 6
        SimpleTypeSchema schema = new SimpleTypeSchema(type: SimpleType.NUMBER, minimum: 11.999, exclusiveMinimum: true)
        JsonNode nodeToValidate = new DoubleNode(invalidValue)

        List<ErrorMessage> result = schema.validate(nodeToValidate)

        assert result.size() == 1
        assert result[0].location == ''
        assert result[0].message.contains("11.999")
        assert result[0].message.contains("exclusiveMinimum")
    }

    @Test
    void validate_shouldNotReturnAnErrorMessage_givenANumberGreaterThanMinimumWhenExclusiveMinimumIsTrue() {
        double validValue = 14
        SimpleTypeSchema schema = new SimpleTypeSchema(type: SimpleType.NUMBER, minimum: 11.999, exclusiveMinimum: true)
        JsonNode nodeToValidate = new DoubleNode(validValue)

        List<ErrorMessage> result = schema.validate(nodeToValidate)

        assert result.empty
    }

    @Test
    void validate_shouldReturnAnErrorMessage_givenAnIntegerEqualToMaximumWhenExclusiveMaximumIsTrue() {
        int invalidInt = 11
        SimpleTypeSchema schema = new SimpleTypeSchema(type: SimpleType.INTEGER, maximum: 11, exclusiveMaximum: true)
        JsonNode nodeToValidate = new IntNode(invalidInt)

        List<ErrorMessage> result = schema.validate(nodeToValidate)

        assert result.size() == 1
        assert result[0].location == ''
        assert result[0].message.contains("11")
        assert result[0].message.contains("exclusiveMaximum")
    }

    @Test
    void validate_shouldReturnAnErrorMessage_givenAnIntegerGreaterThanMaximumWhenExclusiveMaximumIsTrue() {
        int invalidInt = 14
        SimpleTypeSchema schema = new SimpleTypeSchema(type: SimpleType.INTEGER, maximum: 11, exclusiveMaximum: true)
        JsonNode nodeToValidate = new IntNode(invalidInt)

        List<ErrorMessage> result = schema.validate(nodeToValidate)

        assert result.size() == 1
        assert result[0].location == ''
        assert result[0].message.contains("11")
        assert result[0].message.contains("exclusiveMaximum")
    }

    @Test
    void validate_shouldNotReturnAnErrorMessage_givenAnIntegerLessThanMaximumWhenExclusiveMaximumIsTrue() {
        int validInt = 7
        SimpleTypeSchema schema = new SimpleTypeSchema(type: SimpleType.INTEGER, maximum: 11, exclusiveMaximum: true)
        JsonNode nodeToValidate = new IntNode(validInt)

        List<ErrorMessage> result = schema.validate(nodeToValidate)

        assert result.empty
    }

    @Test
    void validate_shouldReturnAnErrorMessage_givenANumberEqualToMaximumWhenExclusiveMaximumIsTrue() {
        double invalidValue = 12
        SimpleTypeSchema schema = new SimpleTypeSchema(type: SimpleType.NUMBER, maximum: 12, exclusiveMaximum: true)
        JsonNode nodeToValidate = new DoubleNode(invalidValue)

        List<ErrorMessage> result = schema.validate(nodeToValidate)

        assert result.size() == 1
        assert result[0].location == ''
        assert result[0].message.contains("12")
        assert result[0].message.contains("exclusiveMaximum")
    }

    @Test(expected = IllegalArgumentException)
    void setExclusiveMinimum_shouldThrowAnException_whenTypeIsNotIntegerOrNumber() {
        new SimpleTypeSchema(type: SimpleType.STRING, exclusiveMinimum: true)
    }

    @Test(expected = IllegalArgumentException)
    void setExclusiveMaximum_shouldThrowAnException_whenTypeIsNotIntegerOrNumber() {
        new SimpleTypeSchema(type: SimpleType.STRING, exclusiveMaximum: true)
    }

    @Test(expected = IllegalArgumentException)
    void setFormat_shouldThrowAnException_whenTypeIsNotIntegerOrNumberAndFormatIsUtcMillisec() {
        new SimpleTypeSchema(type: SimpleType.STRING, format: "utc-millisec")
    }

    @Test(expected = IllegalArgumentException)
    void setType_shouldThrowAnException_givenATypeThatIsNotCompatibleWithTheFormat() {
        SimpleTypeSchema schema = new SimpleTypeSchema(type: SimpleType.NUMBER, format: "utc-millisec")
        schema.type = SimpleType.STRING
    }

    @Test(expected = IllegalArgumentException)
    void setEnumeration_shouldThrowException_givenEnumValuesAreNotOfTheSameTypeSpecified() {
        new SimpleTypeSchema(type: SimpleType.STRING, enumeration: [new TextNode("A"), new IntNode(4)])
    }

    @Test(expected = IllegalArgumentException)
    void setEnumeration_shouldThrowException_givenEnumValuesAreOfSimpleTypeNull() {
        new SimpleTypeSchema(type: SimpleType.NULL, enumeration: [NullNode.instance])
    }

    @Test(expected = IllegalArgumentException)
    void setEnumeration_shouldThrowException_givenEnumValuesAreOfSimpleTypeAny() {
        new SimpleTypeSchema(type: SimpleType.ANY, enumeration: [new TextNode("A")])
    }

    @Test
    void setEnumeration_shouldNotThrowException_givenEnumValuesAreOfTypeString() {
        new SimpleTypeSchema(type: SimpleType.STRING, enumeration: [new TextNode("A")])
    }

    @Test
    void setEnumeration_shouldNotThrowException_givenEnumValuesAreOfTypeNumber() {
        new SimpleTypeSchema(type: SimpleType.NUMBER, enumeration: [new DoubleNode(10.22)])
    }

    @Test
    void setEnumeration_shouldNotThrowException_givenEnumValuesAreOfTypeInteger() {
        new SimpleTypeSchema(type: SimpleType.INTEGER, enumeration: [new IntNode(12)])
    }

    @Test
    void setEnumeration_shouldNotThrowException_givenEnumValuesAreOfTypeBoolean() {
        new SimpleTypeSchema(type: SimpleType.BOOLEAN, enumeration: [BooleanNode.TRUE])
    }

    @Test
    void validate_shouldNotReturnErrorMessage_givenTheValueAreFromEnumerationValuesOfSimpleTypeNumber() {
        SimpleTypeSchema schema = new SimpleTypeSchema(type: SimpleType.NUMBER, enumeration: [new DoubleNode(10.00), new DoubleNode(10.05),])
        JsonNode nodeToValidate = new DoubleNode(10.05)

        List<ErrorMessage> result = schema.validate(nodeToValidate)

        assert result.empty
    }

    @Test
    void validate_shouldReturnErrorMessage_givenTheValueIsNotFromEnumerationValuesOfSimpleTypeNumber() {
        double invalidValue = 10.50
        SimpleTypeSchema schema = new SimpleTypeSchema(type: SimpleType.NUMBER, enumeration: [new DoubleNode(10.00), new DoubleNode(10.05),])
        JsonNode nodeToValidate = new DoubleNode(invalidValue)

        List<ErrorMessage> result = schema.validate(nodeToValidate)

        assert result.size() == 1
        assert result[0].location == ''
        assert result[0].message.contains(invalidValue.toString())
        assert result[0].message.contains("one of")
        assert result[0].message.contains("10.0")
        assert result[0].message.contains("10.05")
    }

    @Test
    void validate_shouldNotReturnErrorMessage_givenTheValueAreFromEnumerationValuesOfSimpleTypeString() {
        SimpleTypeSchema schema = new SimpleTypeSchema(type: SimpleType.STRING, enumeration: [new TextNode("A"), new TextNode("B")])
        JsonNode nodeToValidate = new TextNode("A")

        List<ErrorMessage> result = schema.validate(nodeToValidate)

        assert result.empty
    }

    @Test
    void validate_shouldReturnErrorMessage_givenTheValueIsNotFromEnumerationValuesOfSimpleTypeString() {
        String invalidValue = "C"
        SimpleTypeSchema schema = new SimpleTypeSchema(type: SimpleType.STRING, enumeration: [new TextNode("A"), new TextNode("B")])
        JsonNode nodeToValidate = new TextNode(invalidValue)

        List<ErrorMessage> result = schema.validate(nodeToValidate)

        assert result.size() == 1
        assert result[0].location == ''
        assert result[0].message.contains(invalidValue)
        assert result[0].message.contains("one of")
        assert result[0].message.contains("A")
        assert result[0].message.contains("B")
    }

    @Test
    void validate_shouldReturnErrorMessage_givenTheValueDoesNotMatchTheSameCase() {
        String invalidValue = "abc"
        SimpleTypeSchema schema = new SimpleTypeSchema(type: SimpleType.STRING, enumeration: [new TextNode("ABC"), new TextNode("def")])
        JsonNode nodeToValidate = new TextNode(invalidValue)

        List<ErrorMessage> result = schema.validate(nodeToValidate)

        assert result.size() == 1
        assert result[0].location == ''
        assert result[0].message.contains(invalidValue)
        assert result[0].message.contains("one of")
        assert result[0].message.contains("ABC")
        assert result[0].message.contains("def")
    }

    @Test
    void validate_shouldReturnErrorMessage_givenTheValueHasLeadingOrTrailingWhitespace() {
        SimpleTypeSchema schema = new SimpleTypeSchema(type: SimpleType.STRING, enumeration: [new TextNode("A"), new TextNode("B")])

        ["A ", " A", "\tA", "A\t", "\nA", "A\n"].each { invalidValue ->
            JsonNode nodeToValidate = new TextNode(invalidValue)

            List<ErrorMessage> result = schema.validate(nodeToValidate)

            assert result.size() == 1
        }
    }
}
