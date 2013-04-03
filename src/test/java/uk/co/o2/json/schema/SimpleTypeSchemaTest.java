package uk.co.o2.json.schema;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;
import org.codehaus.jackson.node.*;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SimpleTypeSchemaTest {
    private static final JsonFactory factory = new JsonFactory(new ObjectMapper());

    @Test
	public void validate_shouldReturnAnErrorMessage_givenAnIncompatibleType() throws Exception {
        JsonNode document = factory.createJsonParser("\"abc123\"").readValueAsTree();

        List<ErrorMessage> result = new SimpleTypeSchema(){{setType(SimpleType.NUMBER);}}.validate(document);

        assertEquals(1, result.size());
        assertEquals("", result.get(0).getLocation());
        assertTrue(result.get(0).getMessage().toLowerCase().contains("invalid type"));
    }

    @Test
	public void validate_shouldNotReturnAnErrorMessage_givenACompatibleType() throws Exception {
        JsonNode document = factory.createJsonParser("12345").readValueAsTree();

        List<ErrorMessage> result = new SimpleTypeSchema(){{setType(SimpleType.NUMBER);}}.validate(document);

        assertEquals(true, result.isEmpty());
    }

    @Test
	public void setPattern_shouldThrowAnException_whenAPatternIsSpecifiedForATypeOtherThanString() throws Exception {
        SimpleTypeSchema schema = new SimpleTypeSchema();
        schema.setType(SimpleType.NUMBER);

        try {
            schema.setPattern(Pattern.compile("\\d+"));
            fail("Expected an exception");
        } catch (IllegalArgumentException e) {
            //expected
        }
    }

    @Test
	public void setType_shouldThrowAnException_givenATypeOtherThanString_whenAPatternHasBeenSet() throws Exception {
        SimpleTypeSchema schema = new SimpleTypeSchema();
        schema.setType(SimpleType.STRING);
        schema.setPattern(Pattern.compile("."));

        try {
            schema.setType(SimpleType.NUMBER);
            fail("Expected an exception");
        } catch (IllegalArgumentException e) {
            //expected
        }
    }

    @Test
	public void validate_shouldNotPerformRegexValidation_whenAPatternIsNotProvided() throws Exception {
        SimpleTypeSchema schema = new SimpleTypeSchema();
        schema.setType(SimpleType.STRING);
        schema.setPattern(null);

        List<ErrorMessage> result = schema.validate(new TextNode("blah"));

        assertEquals(true, result.isEmpty());
    }

    @Test
	public void validate_shouldReturnAnErrorMessage_whenAStringDoesNotMatchTheProvidedRegex() throws Exception {
        Pattern expectedRegex = Pattern.compile("\\d+");
        String jsonStringValue = "DoesNotMatchTheRegex";

        SimpleTypeSchema schema = new SimpleTypeSchema();
        schema.setType(SimpleType.STRING);
        schema.setPattern(expectedRegex);

        JsonNode nodeToValidate = new TextNode(jsonStringValue);

        List<ErrorMessage> result = schema.validate(nodeToValidate);

        assertEquals(1, result.size());
        assertEquals("", result.get(0).getLocation());
        assertTrue(result.get(0).getMessage().contains(jsonStringValue));
        assertTrue(result.get(0).getMessage().contains("does not match"));
        assertTrue(result.get(0).getMessage().contains(expectedRegex.pattern()));
    }

    @Test
	public void validate_shouldNotReturnAnyErrorMessages_whenAStringDoesMatchTheProvidedRegex() throws Exception {
        SimpleTypeSchema schema = new SimpleTypeSchema();
        schema.setType(SimpleType.STRING);
        schema.setPattern(Pattern.compile(".+"));
        JsonNode nodeToValidate = new TextNode("Anything should match this regex");

        List<ErrorMessage> result = schema.validate(nodeToValidate);

        assertEquals(true, result.isEmpty());
    }

    @Test
	public void shouldImplementJsonSchema() throws Exception {
        assertTrue(JsonSchema.class.isAssignableFrom(SimpleTypeSchema.class));
    }

    @Test
	public void validate_shouldNotReturnAnErrorMessage_givenAFormatOfDateTimeAndAStringValueThatIsAIso8601DateTime() throws Exception {
        SimpleTypeSchema schema = new SimpleTypeSchema();
        schema.setType(SimpleType.STRING);
        schema.setFormat("date-time");
        JsonNode nodeToValidate = new TextNode("2011-05-10T11:11:17Z");

        List<ErrorMessage> result = schema.validate(nodeToValidate);

        assertEquals(true, result.isEmpty());
    }

    @Test
	public void validate_shouldReturnAnErrorMessage_givenAFormatOfDateTimeAndAStringValueThatIsNotAValidIso8601DateTime() throws Exception {
        String invalidDateTime = "2011-05-44T11:11:17Z";
        SimpleTypeSchema schema = new SimpleTypeSchema();
		schema.setType(SimpleType.STRING);
		schema.setFormat("date-time");
        JsonNode nodeToValidate = new TextNode(invalidDateTime);

        List<ErrorMessage> result = schema.validate(nodeToValidate);

        assertEquals(1, result.size());
        assertEquals("", result.get(0).getLocation());
        assertTrue(result.get(0).getMessage().contains(invalidDateTime));
        assertTrue(result.get(0).getMessage().contains("date-time"));
    }

    @Test
    public void validate_shouldReturnAnErrorMessage_givenAFormatOfDateTimeAndAStringValueThatIsAnAlmostValidIso8601DateTime() throws Exception {
        String invalidDateTime = "2011-May-10T165:11:17z";
        SimpleTypeSchema schema = new SimpleTypeSchema();
        schema.setType(SimpleType.STRING);
        schema.setFormat("date-time");
        JsonNode nodeToValidate = new TextNode(invalidDateTime);

        List<ErrorMessage> result = schema.validate(nodeToValidate);

        assertEquals(1, result.size());
        assertEquals("", result.get(0).getLocation());
        assertTrue(result.get(0).getMessage().contains(invalidDateTime));
        assertTrue(result.get(0).getMessage().contains("date-time"));
    }

    @Test
	public void setFormat_shouldThrowAnException_whenTypeIsNotStringAndFormatIsDateTime() throws Exception {
        SimpleTypeSchema schema = new SimpleTypeSchema();
		schema.setType(SimpleType.NUMBER);
		try {
            schema.setFormat("date-time");
            fail("expected an exception");
        } catch (IllegalArgumentException e) {
            //expected
        }
    }

    @Test
	public void validate_shouldNotReturnAnErrorMessage_givenAFormatOfDateAndAStringValueThatIsAValidDate() throws Exception {
        SimpleTypeSchema schema = new SimpleTypeSchema();
		schema.setType(SimpleType.STRING);
		schema.setFormat("date");
        JsonNode nodeToValidate = new TextNode("2011-05-10");

        List<ErrorMessage> result = schema.validate(nodeToValidate);

        assertEquals(true, result.isEmpty());
    }

    @Test
	public void validate_shouldReturnAnErrorMessage_givenAFormatOfDateAndAStringValueThatIsNotAValidDate() throws Exception {
        String invalidDate = "2011-May-10";
        SimpleTypeSchema schema = new SimpleTypeSchema();
		schema.setType(SimpleType.STRING);
		schema.setFormat("date");
        JsonNode nodeToValidate = new TextNode(invalidDate);

        List<ErrorMessage> result = schema.validate(nodeToValidate);

        assertEquals(1, result.size());
        assertEquals("", result.get(0).getLocation());
        assertTrue( result.get(0).getMessage().contains(invalidDate));
        assertTrue(result.get(0).getMessage().contains("date"));
    }

    @Test
    public void validate_shouldReturnAnErrorMessage_givenAFormatOfDateAndAStringValueThatIsAlmostAValidDate() throws Exception {
        String invalidDate = "2011-05-44";
        SimpleTypeSchema schema = new SimpleTypeSchema();
        schema.setType(SimpleType.STRING);
        schema.setFormat("date");
        JsonNode nodeToValidate = new TextNode(invalidDate);

        List<ErrorMessage> result = schema.validate(nodeToValidate);

        assertEquals(1, result.size());
        assertEquals("", result.get(0).getLocation());
        assertTrue( result.get(0).getMessage().contains(invalidDate));
        assertTrue(result.get(0).getMessage().contains("date"));
    }

    @Test
    public void validate_shouldReturnAnErrorMessage_givenAFormatOfDateAndAStringValueThatDoesNotHaveTheCorrectNumberOfDigits() throws Exception {
        for(String invalidDate : new String[]{
            "5-05-22",
            "95-05-22",
            "995-05-22",
            "1995-5-22",
            "1992-05-2",
            "1111"
        }) {
            SimpleTypeSchema schema = new SimpleTypeSchema();
            schema.setType(SimpleType.STRING);
            schema.setFormat("date");
            JsonNode nodeToValidate = new TextNode(invalidDate);

            List<ErrorMessage> result = schema.validate(nodeToValidate);

            assertEquals("Expected '" + invalidDate + "' to be an invalid format date.", 1, result.size());
            assertEquals("", result.get(0).getLocation());
            assertTrue( result.get(0).getMessage().contains(invalidDate));
            assertTrue(result.get(0).getMessage().contains("date"));
        }
    }

    @Test
	public void validate_shouldReturnAnErrorMessage_givenAFormatOfDateAndAStringValueThatIsAFullDateTime() throws Exception {
        String invalidDate = "2011-05-10T11:47:16Z";
        SimpleTypeSchema schema = new SimpleTypeSchema();
		schema.setType(SimpleType.STRING);
		schema.setFormat("date");
        JsonNode nodeToValidate = new TextNode(invalidDate);

        List<ErrorMessage> result = schema.validate(nodeToValidate);

        assertEquals(1, result.size());
        assertEquals("", result.get(0).getLocation());
        assertTrue(result.get(0).getMessage().contains(invalidDate));
        assertTrue(result.get(0).getMessage().contains("date"));
    }

    @Test
	public void setFormat_shouldThrowAnException_whenTypeIsNotStringAndFormatIsDate() throws Exception {
        SimpleTypeSchema schema = new SimpleTypeSchema();
        schema.setType(SimpleType.NUMBER);
        try {
            schema.setFormat("date");
            fail("Expected an exception");
        } catch (IllegalArgumentException e) {
            //expected
        }
    }

    @Test
	public void validate_shouldNotReturnAnErrorMessage_givenAFormatOfTimeAndAStringValueThatIsAValidTime() throws Exception {
        SimpleTypeSchema schema = new SimpleTypeSchema();
		schema.setType(SimpleType.STRING);
		schema.setFormat("time");
        JsonNode nodeToValidate = new TextNode("13:15:47");

        List<ErrorMessage> result = schema.validate(nodeToValidate);

        assertEquals(true, result.isEmpty());
    }

    @Test
	public void validate_shouldReturnAnErrorMessage_givenAFormatOfTimeAndAStringValueThatIsNotAValidTime() throws Exception {
        String invalidTime = "2011-05-10";
        SimpleTypeSchema schema = new SimpleTypeSchema();
		schema.setType(SimpleType.STRING);
		schema.setFormat("time");
        JsonNode nodeToValidate = new TextNode(invalidTime);

        List<ErrorMessage> result = schema.validate(nodeToValidate);

        assertEquals(1, result.size());
        assertEquals("", result.get(0).getLocation());
        assertTrue(result.get(0).getMessage().contains(invalidTime));
        assertTrue(result.get(0).getMessage().contains("time"));
    }

    @Test
    public void validate_shouldReturnAnErrorMessage_givenAFormatOfTimeAndAStringValueThatIsAlmostAValidTime() throws Exception {
        String invalidTime = "13:75:47";
        SimpleTypeSchema schema = new SimpleTypeSchema();
        schema.setType(SimpleType.STRING);
        schema.setFormat("time");
        JsonNode nodeToValidate = new TextNode(invalidTime);

        List<ErrorMessage> result = schema.validate(nodeToValidate);

        assertEquals(1, result.size());
        assertEquals("", result.get(0).getLocation());
        assertTrue(result.get(0).getMessage().contains(invalidTime));
        assertTrue(result.get(0).getMessage().contains("time"));
    }

    @Test
	public void validate_shouldReturnAnErrorMessage_givenAFormatOfTimeAndAStringValueThatHasExtraInformationAfterTheValidTime() throws Exception {
        String invalidTime = "11:47:16-blah";
        SimpleTypeSchema schema = new SimpleTypeSchema();
		schema.setType(SimpleType.STRING);
		schema.setFormat("time");
        JsonNode nodeToValidate = new TextNode(invalidTime);

        List<ErrorMessage> result = schema.validate(nodeToValidate);

        assertEquals(1, result.size());
        assertEquals("", result.get(0).getLocation());
        assertTrue(result.get(0).getMessage().contains(invalidTime));
        assertTrue(result.get(0).getMessage().contains("time"));
    }

    @Test
	public void setFormat_shouldThrowAnException_whenTypeIsNotStringAndFormatIsTime() throws Exception {
        SimpleTypeSchema schema = new SimpleTypeSchema();
        schema.setType(SimpleType.NUMBER);

        try {
            schema.setFormat("time");
            fail("Expected an exception");
        } catch (IllegalArgumentException e) {
            //expected
        }
    }

    @Test
	public void validate_shouldNotReturnAnErrorMessage_givenAFormatOfRegexAndAStringValueThatIsAValidRegex() throws Exception {
        SimpleTypeSchema schema = new SimpleTypeSchema();
		schema.setType(SimpleType.STRING);
		schema.setFormat("regex");
        JsonNode nodeToValidate = new TextNode(".*");

        List<ErrorMessage> result = schema.validate(nodeToValidate);

        assertEquals(true, result.isEmpty());
    }

    @Test
	public void validate_shouldReturnAnErrorMessage_givenAFormatOfRegexAndAStringValueThatIsNotAValidRegex() throws Exception {
        String invalidRegex = "+";
        SimpleTypeSchema schema = new SimpleTypeSchema();
		schema.setType(SimpleType.STRING);
		schema.setFormat("regex");
        JsonNode nodeToValidate = new TextNode(invalidRegex);

        List<ErrorMessage> result = schema.validate(nodeToValidate);

        assertEquals(1, result.size());
        assertEquals("", result.get(0).getLocation());
        assertTrue(result.get(0).getMessage().contains(invalidRegex));
        assertTrue(result.get(0).getMessage().contains("regex"));
    }

    @Test
	public void setFormat_shouldThrowAnException_whenTypeIsNotStringAndFormatIsRegex() throws Exception {
        SimpleTypeSchema schema = new SimpleTypeSchema();
        schema.setType(SimpleType.NUMBER);

        try {
            schema.setFormat("regex");
            fail("Expected an exception");
        } catch (IllegalArgumentException e) {
            //expected
        }
    }

    @Test
	public void validate_shouldNotReturnAnErrorMessage_givenAFormatOfUriAndAStringValueThatIsAValidUri() throws Exception {
        SimpleTypeSchema schema = new SimpleTypeSchema();
		schema.setType(SimpleType.STRING);
		schema.setFormat("uri");
        JsonNode nodeToValidate = new TextNode("http://www.example.com");

        List<ErrorMessage> result = schema.validate(nodeToValidate);

        assertEquals(true, result.isEmpty());
    }

    @Test
	public void validate_shouldReturnAnErrorMessage_givenAFormatOfUriAndAStringValueThatIsNotAValidUri() throws Exception {
        String invalidUri = ":this-isn't-a-valid-uri"; //it"s surprisingly difficult to NOT be a valid URI
        SimpleTypeSchema schema = new SimpleTypeSchema();
		schema.setType(SimpleType.STRING);
		schema.setFormat("uri");
        JsonNode nodeToValidate = new TextNode(invalidUri);

        List<ErrorMessage> result = schema.validate(nodeToValidate);

        assertEquals(1, result.size());
        assertEquals("", result.get(0).getLocation());
        assertTrue(result.get(0).getMessage().contains(invalidUri));
        assertTrue(result.get(0).getMessage().contains("uri"));
    }

    @Test
	public void setFormat_shouldThrowAnException_whenTypeIsNotStringAndFormatIsUri() throws Exception {
        SimpleTypeSchema schema = new SimpleTypeSchema();
        schema.setType(SimpleType.NUMBER);

        try {
            schema.setFormat("uri");
            fail("Expected an exception");
        } catch (IllegalArgumentException e) {
            //expected
        }
    }

    @Test
	public void validate_shouldNotReturnAnErrorMessage_givenAnUnknownFormatAndAnyOtherwiseValidValue() throws Exception {
        SimpleTypeSchema schema = new SimpleTypeSchema();
        schema.setFormat("custom-format");

        JsonNode nodeToValidate = new TextNode("I am a valid custom-format instance, but it\"s not possible to check");

        List<ErrorMessage> result = schema.validate(nodeToValidate);

        assertEquals(true, result.isEmpty());
    }

    @Test
	public void validate_shouldNotReturnAnErrorMessage_givenAFormatOfUtcMillisecAndANumberValue() throws Exception {
        SimpleTypeSchema schema = new SimpleTypeSchema();
		schema.setType(SimpleType.NUMBER);
		schema.setFormat("utc-millisec");
        JsonNode nodeToValidate = new LongNode(12345L);

        List<ErrorMessage> result = schema.validate(nodeToValidate);

        assertEquals(true, result.isEmpty());
    }

    @Test
	public void validate_shouldNotReturnAnErrorMessage_givenAFormatOfUtcMillisecAndAnIntegerValue() throws Exception {
        SimpleTypeSchema schema = new SimpleTypeSchema();
		schema.setType(SimpleType.INTEGER);
		schema.setFormat("utc-millisec");
        JsonNode nodeToValidate = new IntNode(12345);

        List<ErrorMessage> result = schema.validate(nodeToValidate);

        assertEquals(true, result.isEmpty());
    }

    @Test
	public void setFormat_shouldThrowAnException_whenTypeIsNotNumberOrIntegerAndFormatIsUtcMillisec() throws Exception {
        SimpleTypeSchema schema = new SimpleTypeSchema();
        schema.setType(SimpleType.BOOLEAN);

        try {
            schema.setFormat("utc-millisec");
            fail("Expected an exception");
        } catch (IllegalArgumentException e) {
            //expected
        }
    }

    @Test
	public void validate_shouldReturnAnErrorMessage_givenAStringThatIsLongerThanAMaxLength() throws Exception {
        String invalidString = "1234567890XXXX";
        SimpleTypeSchema schema = new SimpleTypeSchema();
        schema.setType(SimpleType.STRING);
        schema.setMaxLength(10);
        JsonNode nodeToValidate = new TextNode(invalidString);

        List<ErrorMessage> result = schema.validate(nodeToValidate);

        assertEquals(1, result.size());
        assertEquals("", result.get(0).getLocation());
        assertTrue(result.get(0).getMessage().contains("10"));
        assertTrue(result.get(0).getMessage().contains(invalidString));

    }

    @Test
    public void validate_shouldNotReturnAnErrorMessage_givenAValueEqualToMaximum(){
        SimpleTypeSchema schema = new SimpleTypeSchema();
        schema.setType(SimpleType.NUMBER);
        schema.setMaximum(20);

        JsonNode nodeToValidate = new DoubleNode(20);

        List<ErrorMessage> result = schema.validate(nodeToValidate);

        assertEquals(true, result.isEmpty());

    }


    @Test
	public void validate_shouldNotReturnAnErrorMessage_givenAStringThatIsLesserThanOrEqualToMaxLength() throws Exception {
        SimpleTypeSchema schema = new SimpleTypeSchema();
        schema.setType(SimpleType.STRING);
        schema.setMaxLength(10);
        JsonNode nodeToValidate = new TextNode("123");

        List<ErrorMessage> result = schema.validate(nodeToValidate);

        assertEquals(true, result.isEmpty());
    }

    @Test
	public void setMaxLength_shouldThrowAnException_givenATypeOtherThenString() throws Exception {
        SimpleTypeSchema schema = new SimpleTypeSchema();
        schema.setType(SimpleType.INTEGER);

        try {
            schema.setMaxLength(10);
            fail("Expected an exception");
        } catch (IllegalArgumentException e) {
            //expected
        }
    }


    @Test
	public void validate_shouldReturnAnErrorMessage_givenAStringThatIsLessThanAMinLength() throws Exception {
        String invalidString = "1";
        SimpleTypeSchema schema = new SimpleTypeSchema();
        schema.setType(SimpleType.STRING);
        schema.setMinLength(2);
        JsonNode nodeToValidate = new TextNode(invalidString);

        List<ErrorMessage> result = schema.validate(nodeToValidate);

        assertEquals(1, result.size());
        assertEquals("", result.get(0).getLocation());
        assertTrue(result.get(0).getMessage().contains("2"));
        assertTrue(result.get(0).getMessage().contains(invalidString));

    }

    @Test
	public void validate_shouldNotReturnAnErrorMessage_givenAStringThatIsGreaterThanOrEqualToMinLength() throws Exception {
        SimpleTypeSchema schema = new SimpleTypeSchema();
        schema.setType(SimpleType.STRING);
        schema.setMinLength(2);
        JsonNode nodeToValidate = new TextNode("12");

        List<ErrorMessage> result = schema.validate(nodeToValidate);

        assertEquals(true, result.isEmpty());
    }

    @Test
	public void setMinLength_shouldThrowAnException_givenATypeOtherThenString() throws Exception {
        SimpleTypeSchema schema = new SimpleTypeSchema();
        schema.setType(SimpleType.INTEGER);

        try {
            schema.setMinLength(10);
            fail("Expected an exception");
        } catch (IllegalArgumentException e) {
            //expected
        }
    }

    @Test
	public void validate_shouldReturnAnErrorMessage_givenANumberThatIsLessThanAMinimumValue() throws Exception {
        double invalidValue = 1.5;
        SimpleTypeSchema schema = new SimpleTypeSchema();
        schema.setType(SimpleType.NUMBER);
        schema.setMinimum(2);
        JsonNode nodeToValidate = new DoubleNode(invalidValue);

        List<ErrorMessage> result = schema.validate(nodeToValidate);

        assertEquals(1, result.size());
        assertEquals("", result.get(0).getLocation());
        assertTrue(result.get(0).getMessage().contains("2"));
        assertTrue(result.get(0).getMessage().contains(String.valueOf(invalidValue)));

    }

    @Test
	public void validate_shouldNotReturnAnErrorMessage_givenANumberThatIsGreaterThanOrEqualToMinimum() throws Exception {
        SimpleTypeSchema schema = new SimpleTypeSchema();
        schema.setType(SimpleType.NUMBER);
        schema.setMinimum(2);
        JsonNode nodeToValidate = new DoubleNode(2.5);

        List<ErrorMessage> result = schema.validate(nodeToValidate);

        assertEquals(true, result.isEmpty());
    }


    @Test
	public void validate_shouldReturnAnErrorMessage_givenAnIntegerThatIsLessThanAMinimumValue() throws Exception {
        int invalidInt = 1;
        JsonNode nodeToValidate = new IntNode(invalidInt);
        SimpleTypeSchema schema = new SimpleTypeSchema();
        schema.setType(SimpleType.INTEGER);
        schema.setMinimum(10);

        List<ErrorMessage> result = schema.validate(nodeToValidate);

        assertEquals(1, result.size());
        assertEquals("", result.get(0).getLocation());
        assertTrue(result.get(0).getMessage().contains("10"));
        assertTrue(result.get(0).getMessage().contains(String.valueOf(invalidInt)));

    }

    @Test
	public void validate_shouldNotReturnAnErrorMessage_givenAnIntegerThatIsGreaterThanOrEqualToMinimum() throws Exception {
        SimpleTypeSchema schema = new SimpleTypeSchema();
		schema.setType(SimpleType.INTEGER);
		schema.setMinimum(2);
        JsonNode nodeToValidate = new IntNode(3);

        List<ErrorMessage> result = schema.validate(nodeToValidate);

        assertEquals(true, result.isEmpty());
    }

    @SuppressWarnings({"UnusedDeclaration"})
    @Test
	public void setMinimum_shouldThrowAnException_givenATypeOtherThenNumberOrInteger() throws Exception {
        SimpleTypeSchema number = new SimpleTypeSchema() {{ setType(SimpleType.NUMBER); setMinimum(10); }};
        SimpleTypeSchema integer = new SimpleTypeSchema() {{ setType(SimpleType.INTEGER); setMinimum(10);}};
        try {
            SimpleTypeSchema string = new SimpleTypeSchema() {{ setType(SimpleType.STRING); setMinimum(10);}};
            fail("Expected an exception");
        } catch (IllegalArgumentException e) {
            // good
        }
        try {
            SimpleTypeSchema booleanValue = new SimpleTypeSchema() {{ setType(SimpleType.BOOLEAN); setMinimum(10); }};
            fail("Expected an exception");
        } catch (IllegalArgumentException e) {
            // good
        }

    }

    @Test
	public void validate_shouldReturnAnErrorMessage_givenANumberThatIsGreaterThanMaximumValue() throws Exception {
        double invalidValue = 2.5;
        SimpleTypeSchema schema = new SimpleTypeSchema();
		schema.setType(SimpleType.NUMBER);
		schema.setMaximum(2);
        JsonNode nodeToValidate = new DoubleNode(invalidValue);

        List<ErrorMessage> result = schema.validate(nodeToValidate);

        assertEquals(1, result.size());
        assertEquals("", result.get(0).getLocation());
        assertTrue(result.get(0).getMessage().contains("2"));
        assertTrue(result.get(0).getMessage().contains(String.valueOf(invalidValue)));

    }

    @Test
	public void validate_shouldNotReturnAnErrorMessage_givenANumberThatIsLesserThanOrEqualToMaximum() throws Exception {
        SimpleTypeSchema schema = new SimpleTypeSchema();
		schema.setType(SimpleType.NUMBER);
		schema.setMaximum(2);
        JsonNode nodeToValidate = new DoubleNode(1.5);

        List<ErrorMessage> result = schema.validate(nodeToValidate);

        assertEquals(true, result.isEmpty());
    }

    @Test
	public void validate_shouldNotReturnAnErrorMessage_givenAStringLengthThatIsEqualToMaxLength() throws Exception {
        SimpleTypeSchema schema = new SimpleTypeSchema();
		schema.setType(SimpleType.STRING);
		schema.setMaxLength(5);
        JsonNode nodeToValidate = new TextNode("abcde");

        List<ErrorMessage> result = schema.validate(nodeToValidate);

        assertEquals(true, result.isEmpty());
    }


    @Test
	public void validate_shouldReturnAnErrorMessage_givenAnIntegerThatIsGreaterThanAMaximumValue() throws Exception {
        int invalidInt = 11;
        SimpleTypeSchema schema = new SimpleTypeSchema();
		schema.setType(SimpleType.INTEGER);
		schema.setMaximum(10);
        JsonNode nodeToValidate = new IntNode(invalidInt);

        List<ErrorMessage> result = schema.validate(nodeToValidate);

        assertEquals(1, result.size());
        assertEquals("", result.get(0).getLocation());
        assertTrue(result.get(0).getMessage().contains("10"));
        assertTrue(result.get(0).getMessage().contains(String.valueOf(invalidInt)));

    }

    @Test
	public void validate_shouldNotReturnAnErrorMessage_givenAnIntegerThatIsLesserThanOrEqualToMaximum() throws Exception {
        SimpleTypeSchema schema = new SimpleTypeSchema();
		schema.setType(SimpleType.INTEGER);
		schema.setMaximum(2);
        JsonNode nodeToValidate = new IntNode(1);

        List<ErrorMessage> result = schema.validate(nodeToValidate);

        assertEquals(true, result.isEmpty());
    }

    @SuppressWarnings({"UnusedDeclaration"})
    @Test
	public void setMaximum_shouldThrowAnException_givenATypeOtherThenNumberOrInteger() throws Exception {
        SimpleTypeSchema number = new SimpleTypeSchema() {{ setType(SimpleType.NUMBER); setMaximum(10);}};
        SimpleTypeSchema integer = new SimpleTypeSchema() {{ setType(SimpleType.INTEGER); setMaximum(10);}};
        try {
            SimpleTypeSchema string = new SimpleTypeSchema() {{ setType(SimpleType.STRING); setMaximum(10);}};
            fail();
        } catch (IllegalArgumentException e) {
            // good
        }
        try {
            SimpleTypeSchema booleanValue = new SimpleTypeSchema() {{ setType(SimpleType.BOOLEAN); setMaximum(10);}};
            fail();
        } catch (IllegalArgumentException e) {
            // good
        }

    }

    @Test
	public void validate_shouldReturnAnErrorMessage_givenAnIntegerEqualToMinimumWhenExclusiveMinimumIsTrue() throws Exception {
        int invalidInt = 11;
        SimpleTypeSchema schema = new SimpleTypeSchema();
		schema.setType(SimpleType.INTEGER);
		schema.setMinimum(11);
		schema.setExclusiveMinimum(true);
        JsonNode nodeToValidate = new IntNode(invalidInt);

        List<ErrorMessage> result = schema.validate(nodeToValidate);

        assertEquals(1, result.size());
        assertEquals("", result.get(0).getLocation());
        assertTrue(result.get(0).getMessage().contains("11"));
        assertTrue(result.get(0).getMessage().contains("exclusiveMinimum"));

    }

    @Test
	public void validate_shouldReturnAnErrorMessage_givenANumberEqualToMinimumWhenExclusiveMinimumIsTrue() throws Exception {
        double invalidValue = 11.999;
        SimpleTypeSchema schema = new SimpleTypeSchema();
		schema.setType(SimpleType.NUMBER);
		schema.setMinimum(11.999);
		schema.setExclusiveMinimum(true);
        JsonNode nodeToValidate = new DoubleNode(invalidValue);

        List<ErrorMessage> result = schema.validate(nodeToValidate);

        assertEquals(1, result.size());
        assertEquals("", result.get(0).getLocation());
        assertTrue(result.get(0).getMessage().contains("11.999"));
        assertTrue(result.get(0).getMessage().contains("exclusiveMinimum"));
    }

    @Test
	public void validate_shouldReturnAnErrorMessage_givenANumberLessThanMinimumWhenExclusiveMinimumIsTrue() throws Exception {
        double invalidValue = 6;
        SimpleTypeSchema schema = new SimpleTypeSchema();
		schema.setType(SimpleType.NUMBER);
		schema.setMinimum(11.999);
		schema.setExclusiveMinimum(true);
        JsonNode nodeToValidate = new DoubleNode(invalidValue);

        List<ErrorMessage> result = schema.validate(nodeToValidate);

        assertEquals(1, result.size());
        assertEquals("", result.get(0).getLocation());
        assertTrue(result.get(0).getMessage().contains("11.999"));
        assertTrue(result.get(0).getMessage().contains("exclusiveMinimum"));
    }

    @Test
	public void validate_shouldNotReturnAnErrorMessage_givenANumberGreaterThanMinimumWhenExclusiveMinimumIsTrue() throws Exception {
        double validValue = 14;
        SimpleTypeSchema schema = new SimpleTypeSchema();
		schema.setType(SimpleType.NUMBER);
		schema.setMinimum(11.999);
		schema.setExclusiveMinimum(true);
        JsonNode nodeToValidate = new DoubleNode(validValue);

        List<ErrorMessage> result = schema.validate(nodeToValidate);

        assertEquals(true, result.isEmpty());
    }

    @Test
	public void validate_shouldReturnAnErrorMessage_givenAnIntegerEqualToMaximumWhenExclusiveMaximumIsTrue() throws Exception {
        int invalidInt = 11;
        SimpleTypeSchema schema = new SimpleTypeSchema();
		schema.setType(SimpleType.INTEGER);
		schema.setMaximum(11);
		schema.setExclusiveMaximum(true);
        JsonNode nodeToValidate = new IntNode(invalidInt);

        List<ErrorMessage> result = schema.validate(nodeToValidate);

        assertEquals(1, result.size());
        assertEquals("", result.get(0).getLocation());
        assertTrue(result.get(0).getMessage().contains("11"));
        assertTrue(result.get(0).getMessage().contains("exclusiveMaximum"));
    }

    @Test
	public void validate_shouldReturnAnErrorMessage_givenAnIntegerGreaterThanMaximumWhenExclusiveMaximumIsTrue() throws Exception {
        int invalidInt = 14;
        SimpleTypeSchema schema = new SimpleTypeSchema();
		schema.setType(SimpleType.INTEGER);
		schema.setMaximum(11);
		schema.setExclusiveMaximum(true);
        JsonNode nodeToValidate = new IntNode(invalidInt);

        List<ErrorMessage> result = schema.validate(nodeToValidate);

        assertEquals(1, result.size());
        assertEquals("", result.get(0).getLocation());
        assertTrue(result.get(0).getMessage().contains("11"));
        assertTrue(result.get(0).getMessage().contains("exclusiveMaximum"));
    }

    @Test
	public void validate_shouldNotReturnAnErrorMessage_givenAnIntegerLessThanMaximumWhenExclusiveMaximumIsTrue() throws Exception {
        int validInt = 7;
        SimpleTypeSchema schema = new SimpleTypeSchema();
		schema.setType(SimpleType.INTEGER);
		schema.setMaximum(11);
		schema.setExclusiveMaximum(true);
        JsonNode nodeToValidate = new IntNode(validInt);

        List<ErrorMessage> result = schema.validate(nodeToValidate);

        assertEquals(true, result.isEmpty());
    }

    @Test
	public void validate_shouldReturnAnErrorMessage_givenANumberEqualToMaximumWhenExclusiveMaximumIsTrue() throws Exception {
        double invalidValue = 12;
        SimpleTypeSchema schema = new SimpleTypeSchema();
		schema.setType(SimpleType.NUMBER);
		schema.setMaximum(12);
		schema.setExclusiveMaximum(true);
        JsonNode nodeToValidate = new DoubleNode(invalidValue);

        List<ErrorMessage> result = schema.validate(nodeToValidate);

        assertEquals(1, result.size());
        assertEquals("", result.get(0).getLocation());
        assertTrue(result.get(0).getMessage().contains("12"));
        assertTrue(result.get(0).getMessage().contains("exclusiveMaximum"));
    }

    @Test
	public void setExclusiveMinimum_shouldThrowAnException_whenTypeIsNotIntegerOrNumber() throws Exception {
        SimpleTypeSchema schema = new SimpleTypeSchema();
        schema.setType(SimpleType.STRING);

        try {
            schema.setExclusiveMinimum(true);
            fail("Expected an exception");
        } catch (IllegalArgumentException e) {
            //expected
        }
    }

    @Test
	public void setExclusiveMaximum_shouldThrowAnException_whenTypeIsNotIntegerOrNumber() throws Exception {
        SimpleTypeSchema schema = new SimpleTypeSchema();
        schema.setType(SimpleType.STRING);

        try {
            schema.setExclusiveMaximum(true);
            fail("Expected an exception");
        } catch (IllegalArgumentException e) {
            //expected
        }
    }

    @Test
	public void setFormat_shouldThrowAnException_whenTypeIsNotIntegerOrNumberAndFormatIsUtcMillisec() throws Exception {
        SimpleTypeSchema schema = new SimpleTypeSchema();
        schema.setType(SimpleType.STRING);

        try {
            schema.setFormat("utc-millisec");
            fail("Expected an exception");
        } catch (IllegalArgumentException e) {
            //expected
        }
    }

    @Test
	public void setType_shouldThrowAnException_givenATypeThatIsNotCompatibleWithTheFormat() throws Exception {
        SimpleTypeSchema schema = new SimpleTypeSchema();
		schema.setType(SimpleType.NUMBER);
		schema.setFormat("utc-millisec");
        try {
            schema.setType(SimpleType.STRING);
            fail("expected an exception");
        } catch (IllegalArgumentException e) {
            //expected
        }
    }

    @Test
	public void setEnumeration_shouldThrowException_givenEnumValuesAreNotOfTheSameTypeSpecified() throws Exception {
        SimpleTypeSchema schema = new SimpleTypeSchema();
        schema.setType(SimpleType.STRING);

        try {
            schema.setEnumeration(Arrays.<JsonNode>asList(new TextNode("A"), new IntNode(4)));
            fail("Expected an exception");
        } catch (IllegalArgumentException e) {
            //expected
        }
    }

    @Test
	public void setEnumeration_shouldThrowException_givenEnumValuesAreOfSimpleTypeNull() throws Exception {
        SimpleTypeSchema schema = new SimpleTypeSchema();
        schema.setType(SimpleType.NULL);

        try {
            schema.setEnumeration(Arrays.<JsonNode>asList(NullNode.getInstance()));
            fail("Expected an exception");
        } catch (IllegalArgumentException e) {
            //expected
        }
    }

    @Test
	public void setEnumeration_shouldThrowException_givenEnumValuesAreOfSimpleTypeAny() throws Exception {
        SimpleTypeSchema schema = new SimpleTypeSchema();
        schema.setType(SimpleType.ANY);

        try {
            schema.setEnumeration(Arrays.<JsonNode>asList(new TextNode("A")));
            fail("Expected an exception");
        } catch (IllegalArgumentException e) {
            //expected
        }
    }

    @Test
	public void setEnumeration_shouldNotThrowException_givenEnumValuesAreOfTypeString() throws Exception {
        SimpleTypeSchema schema = new SimpleTypeSchema();
        schema.setType(SimpleType.STRING);

        schema.setEnumeration(Arrays.<JsonNode>asList(new TextNode("A")));
    }

    @Test
	public void setEnumeration_shouldNotThrowException_givenEnumValuesAreOfTypeNumber() throws Exception {
        SimpleTypeSchema schema = new SimpleTypeSchema();
		schema.setType(SimpleType.NUMBER);
		schema.setEnumeration(Arrays.<JsonNode>asList(new DoubleNode(10.22)));
    }

    @Test
	public void setEnumeration_shouldNotThrowException_givenEnumValuesAreOfTypeInteger() throws Exception {
        SimpleTypeSchema schema = new SimpleTypeSchema();
		schema.setType(SimpleType.INTEGER);
		schema.setEnumeration(Arrays.<JsonNode>asList(new IntNode(12)));
    }

    @Test
	public void setEnumeration_shouldNotThrowException_givenEnumValuesAreOfTypeBoolean() throws Exception {
        SimpleTypeSchema schema = new SimpleTypeSchema();
		schema.setType(SimpleType.BOOLEAN);
		schema.setEnumeration(Arrays.<JsonNode>asList(BooleanNode.TRUE));
    }

    @Test
	public void validate_shouldNotReturnErrorMessage_givenTheValueAreFromEnumerationValuesOfSimpleTypeNumber() throws Exception {
        SimpleTypeSchema schema = new SimpleTypeSchema();
		schema.setType(SimpleType.NUMBER);
		schema.setEnumeration(Arrays.<JsonNode>asList(new DoubleNode(10.00), new DoubleNode(10.05)));
        JsonNode nodeToValidate = new DoubleNode(10.05);

        List<ErrorMessage> result = schema.validate(nodeToValidate);

        assertEquals(true, result.isEmpty());
    }

    @Test
	public void validate_shouldReturnErrorMessage_givenTheValueIsNotFromEnumerationValuesOfSimpleTypeNumber() throws Exception {
        double invalidValue = 10.50;
        SimpleTypeSchema schema = new SimpleTypeSchema();
		schema.setType(SimpleType.NUMBER);
		schema.setEnumeration(Arrays.<JsonNode>asList(new DoubleNode(10.00), new DoubleNode(10.05)));
        JsonNode nodeToValidate = new DoubleNode(invalidValue);

        List<ErrorMessage> result = schema.validate(nodeToValidate);

        assertEquals(1, result.size());
        assertEquals("", result.get(0).getLocation());
        assertTrue(result.get(0).getMessage().contains(String.valueOf(invalidValue)));
        assertTrue(result.get(0).getMessage().contains("one of"));
        assertTrue(result.get(0).getMessage().contains("10.0"));
        assertTrue(result.get(0).getMessage().contains("10.05"));
    }

    @Test
	public void validate_shouldNotReturnErrorMessage_givenTheValueAreFromEnumerationValuesOfSimpleTypeString() throws Exception {
        SimpleTypeSchema schema = new SimpleTypeSchema();
		schema.setType(SimpleType.STRING);
		schema.setEnumeration(Arrays.<JsonNode>asList(new TextNode("A"), new TextNode("B")));
        JsonNode nodeToValidate = new TextNode("A");

        List<ErrorMessage> result = schema.validate(nodeToValidate);

        assertEquals(true, result.isEmpty());
    }

    @Test
	public void validate_shouldReturnErrorMessage_givenTheValueIsNotFromEnumerationValuesOfSimpleTypeString() throws Exception {
        String invalidValue = "C";
        SimpleTypeSchema schema = new SimpleTypeSchema();
		schema.setType(SimpleType.STRING);
		schema.setEnumeration(Arrays.<JsonNode>asList(new TextNode("A"), new TextNode("B")));
        JsonNode nodeToValidate = new TextNode(invalidValue);

        List<ErrorMessage> result = schema.validate(nodeToValidate);

        assertEquals(1, result.size());
        assertEquals("", result.get(0).getLocation());
        assertTrue(result.get(0).getMessage().contains(invalidValue));
        assertTrue(result.get(0).getMessage().contains("one of"));
        assertTrue(result.get(0).getMessage().contains("A"));
        assertTrue(result.get(0).getMessage().contains("B"));
    }
}
