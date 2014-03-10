package uk.co.o2.json.schema;

import org.junit.Test;

import javax.json.Json;
import javax.json.JsonReader;
import javax.json.JsonValue;
import java.io.StringReader;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ObjectSchemaTest {

    @Test
    public void validate_shouldValidateAllPropertiesAndCombineAnyErrorMessages() throws Exception {
        JsonValue document = readValue("{ " +
            "\"foo\": \"a\"," +
            "\"bar\": {" +
                "\"baz\": 1 " +
            "}" +
        "}");

        ObjectSchema schema = new ObjectSchema();
        schema.getProperties().add(new ObjectSchema.Property() {{
            setName("foo");
            setNestedSchema(new SimpleTypeSchema() {{
                setType(SimpleType.NUMBER);
            }});
        }});
        schema.getProperties().add(new ObjectSchema.Property() {{
            setName("bar");
            setNestedSchema(new ObjectSchema() {{
                getProperties().add(new ObjectSchema.Property() {{
                    setName("quux");
                    setNestedSchema(new SimpleTypeSchema() {{
                        setType(SimpleType.NUMBER);
                    }});
                    setRequired(true);
                }});
                getProperties().add(new ObjectSchema.Property() {{
                    setName("baz");
                    setNestedSchema(new SimpleTypeSchema() {{
                        setType(SimpleType.STRING);
                    }});
                }});
            }});
        }});

        List<ErrorMessage> result = schema.validate(document);

        assertEquals(3, result.size());
        assertEquals("foo", result.get(0).getLocation());
        assertEquals("bar.quux", result.get(1).getLocation());
        assertEquals("bar.baz", result.get(2).getLocation());
    }

    @Test
    public void validate_shouldEmitAnErrorMessage_givenAJsonArray() throws Exception {
        JsonValue document = readValue("[ {\"foo\": \"bar\"} ]");

        ObjectSchema schema = new ObjectSchema();

        List<ErrorMessage> result = schema.validate(document);

        assertEquals(1, result.size());
        assertTrue(result.get(0).getMessage().contains("must be an object"));
    }

    @Test
    public void validate_shouldEmitAnErrorMessage_givenASimpleValue() throws Exception {
        JsonValue document = Json.createObjectBuilder().add("foo", "\"foo\"").build().get("foo");

        ObjectSchema schema = new ObjectSchema();

        List<ErrorMessage> result = schema.validate(document);

        assertEquals(1, result.size());
        assertTrue(result.get(0).getMessage().contains("must be an object"));
    }

    @Test
    public void validate_shouldReportAnError_whenARequiredPropertyIsMissing() throws Exception {
        JsonValue document = readValue("{\"foo\": \"bar\"}");

        ObjectSchema schema = new ObjectSchema();
        schema.getProperties().add(new ObjectSchema.Property(){{
            setName("id");
            setRequired(true);
        }});
        schema.getProperties().add(new ObjectSchema.Property(){{
            setName("missingObject");
            setRequired(true);
            setNestedSchema(new ObjectSchema());
        }});

        List<ErrorMessage> result = schema.validate(document);

        assertEquals(2, result.size());
        assertTrue(result.get(0).getMessage().contains("required"));
        assertTrue(result.get(0).getMessage().contains("id"));
        assertEquals("id", result.get(0).getLocation());
        assertTrue(result.get(1).getMessage().contains("required"));
        assertTrue(result.get(1).getMessage().contains("missingObject"));
        assertEquals("missingObject", result.get(1).getLocation());
    }

    @Test
    public void validate_shouldNotReportAnError_whenARequiredPropertyIsPresent() throws Exception {
        JsonValue document = readValue("{\"id\": \"value\", \"foo\": {}}");

        ObjectSchema schema = new ObjectSchema();
        schema.getProperties().add(new ObjectSchema.Property(){{
            setName("id");
            setRequired(true);
        }});
        schema.getProperties().add(new ObjectSchema.Property(){{
            setName("foo");
            setRequired(true);
            setNestedSchema(new ObjectSchema());
        }});

        List<ErrorMessage> result = schema.validate(document);

        assertTrue(result.isEmpty());
    }

    @Test
    public void validate_shouldNotReportAnError_whenANonRequiredPropertyIsNotPresent() throws Exception {
        JsonValue document = readValue("{\"foo\": \"bar\"}");

        ObjectSchema schema = new ObjectSchema();
        schema.getProperties().add(new ObjectSchema.Property(){{
            setName("id");
            setRequired(false);
        }});
        schema.getProperties().add(new ObjectSchema.Property() {{
            setName("missingObject");
            setRequired(false);
            setNestedSchema(new ObjectSchema());
        }});

        List<ErrorMessage> result = schema.validate(document);

        assertTrue(result.isEmpty());
    }

    @Test
    public void validate_shouldReportAnError_whenAnAdditionalPropertyIsFoundThatDoesNotConformToSchema() throws Exception {
        JsonValue document = readValue("{" +
            "\"additionalProperty\": \"NotReallyMuchOfANumber\"," +
            "\"additionalProperty2\": \"Also Wrong\"" +
        "}");

        ObjectSchema schema = new ObjectSchema();
        schema.setAdditionalProperties(new SimpleTypeSchema(){{
            setType(SimpleType.NUMBER);
        }});

        List<ErrorMessage> result = schema.validate(document);

        assertEquals(2, result.size());
        assertEquals("additionalProperty", result.get(0).getLocation());
        assertTrue(result.get(0).getMessage().toLowerCase().contains("invalid type"));
        assertEquals("additionalProperty2", result.get(1).getLocation());
        assertTrue(result.get(1).getMessage().toLowerCase().contains("invalid type"));
    }

    @Test
    public void validate_shouldNotReportAnError_whenAnAdditionalPropertyIsFoundAndAllAdditionalPropertiesAreAllowed() throws Exception {
        JsonValue document = readValue("{" +
            "\"additionalProperty\": \"NotReallyMuchOfANumber\"," +
            "\"additionalProperty2\": \"Also Wrong\"" +
        "}");

        ObjectSchema schema = new ObjectSchema();
        schema.setAdditionalProperties(ObjectSchema.ALLOW_ALL_ADDITIONAL_PROPERTIES);

        List<ErrorMessage> result = schema.validate(document);

        assertEquals(0, result.size());
    }

    @Test
    public void validate_shouldReportAnError_whenAnAdditionalPropertyIsFoundAndNoAdditionalPropertiesAreAllowed() throws Exception {
        JsonValue document = readValue("{" +
            "\"additionalProperty\": \"NotReallyMuchOfANumber\"" +
        "}");

        ObjectSchema schema = new ObjectSchema();
        schema.setAdditionalProperties(ObjectSchema.FORBID_ANY_ADDITIONAL_PROPERTIES);

        List<ErrorMessage> result = schema.validate(document);

        assertEquals(1, result.size());
        assertEquals("additionalProperty", result.get(0).getLocation());
        assertTrue(result.get(0).getMessage().toLowerCase().contains("unexpected property"));
    }

    @Test
    public void shouldImplementJsonSchema() throws Exception {
        assertTrue(JsonSchema.class.isAssignableFrom(ObjectSchema.class));
    }

    private JsonValue readValue(String rawJson) {
        try (JsonReader reader = Json.createReader(new StringReader(rawJson))) {
            return reader.read();
        }
    }
}
