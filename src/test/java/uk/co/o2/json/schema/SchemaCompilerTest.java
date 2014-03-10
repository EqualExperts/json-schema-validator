package uk.co.o2.json.schema;

import org.junit.After;
import org.junit.Test;

import javax.json.Json;
import javax.json.JsonException;
import javax.json.JsonReaderFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class SchemaCompilerTest {
    private static JsonReaderFactory jsonFactory = Json.createReaderFactory(null);

    private final List<File> filesToDelete = new ArrayList<>();

    private SchemaPassThroughCache registry = new SchemaPassThroughCache(jsonFactory);
    private SchemaCompiler schemaFactory = new SchemaCompiler(registry, jsonFactory);

    @Test(expected = IllegalArgumentException.class)
    public void parse_shouldThrowAnException_givenAJsonNodeThatIsNotAnObjectNode() throws Exception {
        URL schemaURL = saveSchemaToURL("[\"notASchema\"]");

        schemaFactory.parse(schemaURL);
    }

    @Test
    public void parse_shouldThrowAnException_giveAUrlToASchemaThatDoesNotExist() throws Exception {
        try {
            schemaFactory.parse(new URL("file:///non/existent/file/url"));
            fail("should throw");
        }
        catch (IllegalArgumentException ex){
            assertTrue(ex.getCause() instanceof IOException);
        }
    }

    @Test
    public void parse_shouldThrowAnException_givenAUrlToASchemaThatDoesntContainValidJson() throws Exception {
        URL schemaURL = saveSchemaToURL("not valid json");

        try {
            schemaFactory.parse(schemaURL);
            fail("should throw");
        }
        catch (IllegalArgumentException ex){
            assertTrue(ex.getMessage().contains("invalid JSON"));
            assertTrue(ex.getCause() instanceof JsonException);
        }
    }

    @Test
    public void parse_shouldReturnASimpleTypeSchema_givenATypeString() throws Exception {
        URL rawSchema = saveSchemaToURL("{ \"type\": \"string\" }");

        SimpleTypeSchema compiledSchema = (SimpleTypeSchema) schemaFactory.parse(rawSchema);

        assertEquals(SimpleType.STRING, compiledSchema.getType());
        assertNull(compiledSchema.getPattern());
        assertNull(compiledSchema.getFormat());
    }

    @Test
    public void parse_shouldSetARegexPattern_givenASimpleTypeStringWithAPattern() throws Exception {
        URL rawSchema = saveSchemaToURL("{ \"type\": \"string\", \"pattern\": \"12345\" }");

        SimpleTypeSchema compiledSchema = (SimpleTypeSchema) schemaFactory.parse(rawSchema);

        assertEquals(SimpleType.STRING, compiledSchema.getType());
        assertEquals("12345", compiledSchema.getPattern().pattern());
    }

    @Test
    public void parse_shouldReturnASimpleTypeSchema_givenATypeNumber() throws Exception {
        URL rawSchema = saveSchemaToURL("{ \"type\": \"number\" }");

        SimpleTypeSchema compiledSchema = (SimpleTypeSchema) schemaFactory.parse(rawSchema);

        assertEquals(SimpleType.NUMBER, compiledSchema.getType());
        assertNull(compiledSchema.getPattern());
    }

    @Test
    public void parse_shouldReturnASimpleTypeSchema_givenATypeInteger() throws Exception {
        URL rawSchema = saveSchemaToURL("{ \"type\": \"integer\" }");

        SimpleTypeSchema compiledSchema = (SimpleTypeSchema) schemaFactory.parse(rawSchema);

        assertEquals(SimpleType.INTEGER, compiledSchema.getType());
        assertNull(compiledSchema.getPattern());
    }

    @Test
    public void parse_shouldReturnASimpleTypeSchema_givenATypeBoolean() throws Exception {
        URL rawSchema = saveSchemaToURL("{ \"type\": \"boolean\" }");

        SimpleTypeSchema compiledSchema = (SimpleTypeSchema) schemaFactory.parse(rawSchema);

        assertEquals(SimpleType.BOOLEAN, compiledSchema.getType());
        assertNull(compiledSchema.getPattern());
    }

    @Test
    public void parse_shouldReturnASimpleTypeSchema_givenATypeNull() throws Exception {
        URL rawSchema = saveSchemaToURL("{ \"type\": \"null\" }");

        SimpleTypeSchema compiledSchema = (SimpleTypeSchema) schemaFactory.parse(rawSchema);

        assertEquals(SimpleType.NULL, compiledSchema.getType());
        assertNull(compiledSchema.getPattern());
    }

    @Test
    public void parse_shouldReturnASimpleTypeSchema_givenATypeAny() throws Exception {
        URL rawSchema = saveSchemaToURL("{ \"type\": \"any\" }");

        SimpleTypeSchema compiledSchema = (SimpleTypeSchema) schemaFactory.parse(rawSchema);

        assertEquals(SimpleType.ANY, compiledSchema.getType());
        assertNull(compiledSchema.getPattern());
    }

    @Test(expected = IllegalArgumentException.class)
    public void parse_shouldThrowAnException_givenASimpleTypeThatIsNotStringButHasAPattern() throws Exception {
        URL rawSchema = saveSchemaToURL("{ \"type\": \"integer\", \"pattern\": \"12345\" }");

        schemaFactory.parse(rawSchema);
    }

    @Test(expected = IllegalArgumentException.class)
    public void parse_shouldThrowAnException_givenASimpleTypeThatIsNotCompatibleWithTheFormat() throws Exception {
        URL rawSchema = saveSchemaToURL("{ \"type\": \"integer\", \"format\": \"date-time\" }");

        schemaFactory.parse(rawSchema);
    }

    @Test
    public void parse_shouldReturnASimpleTypeSchemaWithAFormat_givenAFormat() throws Exception {
        URL rawSchema = saveSchemaToURL("{ \"type\": \"string\", \"format\": \"date-time\" }");

        SimpleTypeSchema compiledSchema = (SimpleTypeSchema) schemaFactory.parse(rawSchema);

        assertEquals("date-time", compiledSchema.getFormat());
    }

    @Test
    public void parse_shouldReturnASimpleTypeSchemaWithMinLength_givenAMinLengthForTypeString() throws Exception {
        URL rawSchema = saveSchemaToURL("{ \"type\": \"string\", \"minLength\": 10 }");

        SimpleTypeSchema compiledSchema = (SimpleTypeSchema) schemaFactory.parse(rawSchema);

        assertEquals(10, compiledSchema.getMinLength());
    }

    @Test
    public void parse_shouldReturnASimpleTypeSchemaWithMinLength_givenAMaxLengthForTypeString() throws Exception {
        URL rawSchema = saveSchemaToURL("{ \"type\": \"string\", \"minLength\": 10, \"maxLength\": 20 }");

        SimpleTypeSchema compiledSchema = (SimpleTypeSchema) schemaFactory.parse(rawSchema);

        assertEquals(10, compiledSchema.getMinLength());
        assertEquals(20, compiledSchema.getMaxLength());
    }

    @Test(expected = IllegalArgumentException.class)
    public void parse_shouldThrowException_givenAMaxLengthForTypeOtherThanString() throws Exception {
        URL rawSchema = saveSchemaToURL("{ \"type\": \"number\", \"maxLength\": 20 }");

        schemaFactory.parse(rawSchema);
    }

    @Test(expected = IllegalArgumentException.class)
    public void parse_shouldThrowException_givenAMinLengthForTypeOtherThanString() throws Exception {
        URL rawSchema = saveSchemaToURL("{ \"type\": \"number\", \"minLength\": 20 }");

        schemaFactory.parse(rawSchema);
    }

    @Test
    public void parse_shouldReturnASimpleTypeSchemaWithEnumeration_givenAnEnumeration() throws Exception {
        URL rawSchema = saveSchemaToURL("{ \"type\": \"string\", \"enumeration\": [\"A\",\"B\"] }");

        SimpleTypeSchema compiledSchema = (SimpleTypeSchema) schemaFactory.parse(rawSchema);

        assertEquals(2, compiledSchema.getEnumeration().size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void parse_shouldThrowException_givenAnEnumerationForTypeNull() throws Exception {
        URL rawSchema = saveSchemaToURL("{ \"type\": \"null\", \"enumeration\": [\"A\",\"B\"] }");

        schemaFactory.parse(rawSchema);
    }

    @Test(expected = IllegalArgumentException.class)
    public void parse_shouldThrowException_givenAnEnumerationForTypeAny() throws Exception {
        URL rawSchema = saveSchemaToURL("{ \"type\": \"any\", \"enumeration\": [\"A\",\"B\"] }");

        schemaFactory.parse(rawSchema);
    }

    @Test
    public void parse_shouldReturnASimpleTypeSchemaWithMinimum_givenAMinimumPropertyForIntegerType() throws Exception {
        URL rawSchema = saveSchemaToURL("{ \"type\": \"integer\", \"minimum\": 10 }");

        SimpleTypeSchema compiledSchema = (SimpleTypeSchema) schemaFactory.parse(rawSchema);

        assertEquals("10", compiledSchema.getMinimum().toString());
    }

    @Test
    public void parse_shouldReturnASimpleTypeSchemaWithMinimum_givenAMinimumPropertyForNumberType() throws Exception {
        URL rawSchema = saveSchemaToURL("{ \"type\": \"number\", \"minimum\": 10.05 }");

        SimpleTypeSchema compiledSchema = (SimpleTypeSchema) schemaFactory.parse(rawSchema);

        assertEquals(new BigDecimal("10.05"), compiledSchema.getMinimum());
        assertEquals("10.05", compiledSchema.getMinimum().toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void parse_shouldThrowException_givenMinimumPropertyOtherThanNumberAndIntegerSimpleType() throws Exception {
        URL rawSchema = saveSchemaToURL("{ \"type\": \"string\", \"minimum\": 10 }");

        schemaFactory.parse(rawSchema);
    }

    @Test
    public void parse_shouldReturnASimpleTypeSchemaWithMaximum_givenAMaximumPropertyForIntegerType() throws Exception {
        URL rawSchema = saveSchemaToURL("{ \"type\": \"integer\", \"maximum\": 10 }");

        SimpleTypeSchema compiledSchema = (SimpleTypeSchema) schemaFactory.parse(rawSchema);

        assertEquals("10", compiledSchema.getMaximum().toString());
    }

    @Test
    public void parse_shouldReturnASimpleTypeSchemaWithMaximum_givenAMaximumPropertyForNumberType() throws Exception {
        URL rawSchema = saveSchemaToURL("{ \"type\": \"number\", \"maximum\": 10.05 }");

        SimpleTypeSchema compiledSchema = (SimpleTypeSchema) schemaFactory.parse(rawSchema);

        assertEquals(new BigDecimal("10.05"), compiledSchema.getMaximum());
        assertEquals("10.05", compiledSchema.getMaximum().toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void parse_shouldThrowException_givenMaximumPropertyOtherThanNumberAndIntegerSimpleType() throws Exception {
        URL rawSchema = saveSchemaToURL("{ \"type\": \"string\", \"maximum\": 10 }");

        schemaFactory.parse(rawSchema);
    }

    @Test
    public void parse_shouldReturnASimpleTypeSchemaWithExclusiveMinimum_givenAExclusiveMinimumPropertyForIntegerType() throws Exception {
        URL rawSchema = saveSchemaToURL("{ \"type\": \"integer\", \"exclusiveMinimum\": true }");

        SimpleTypeSchema compiledSchema = (SimpleTypeSchema) schemaFactory.parse(rawSchema);

        assertEquals(true, compiledSchema.isExclusiveMinimum());
    }

    @Test
    public void parse_shouldReturnASimpleTypeSchemaWithExclusiveMinimum_givenAExclusiveMinimumPropertyForNumberType() throws Exception {
        URL rawSchema = saveSchemaToURL("{ \"type\": \"number\", \"exclusiveMinimum\": true }");

        SimpleTypeSchema compiledSchema = (SimpleTypeSchema) schemaFactory.parse(rawSchema);

        assertEquals(true, compiledSchema.isExclusiveMinimum());
    }

    @Test(expected = IllegalArgumentException.class)
    public void parse_shouldThrowException_givenExclusiveMinimumPropertyOtherThanNumberAndIntegerSimpleType() throws Exception {
        URL rawSchema = saveSchemaToURL("{ \"type\": \"string\", \"exclusiveMinimum\": true }");

        schemaFactory.parse(rawSchema);
    }

    @Test
    public void parse_shouldReturnASimpleTypeSchemaWithExclusiveMaximum_givenAExclusiveMaximumPropertyForIntegerType() throws Exception {
        URL rawSchema = saveSchemaToURL("{ \"type\": \"integer\", \"exclusiveMaximum\": true }");

        SimpleTypeSchema compiledSchema = (SimpleTypeSchema) schemaFactory.parse(rawSchema);

        assertEquals(true, compiledSchema.isExclusiveMaximum());
    }

    @Test
    public void parse_shouldReturnASimpleTypeSchemaWithExclusiveMaximum_givenAExclusiveMaximumPropertyForNumberType() throws Exception {
        URL rawSchema = saveSchemaToURL("{ \"type\": \"number\", \"exclusiveMaximum\": true }");

        SimpleTypeSchema compiledSchema = (SimpleTypeSchema) schemaFactory.parse(rawSchema);

        assertEquals(true, compiledSchema.isExclusiveMaximum());
    }

    @Test(expected = IllegalArgumentException.class)
    public void parse_shouldThrowException_givenExclusiveMaximumPropertyOtherThanNumberAndIntegerSimpleType() throws Exception {
        URL rawSchema = saveSchemaToURL("{ \"type\": \"string\", \"exclusiveMaximum\": true }");

        schemaFactory.parse(rawSchema);
    }

    @Test
    public void parse_ShouldReturnAJsonSchema() throws Exception {
        URL rawSchema = saveSchemaToURL("{ \"type\": \"integer\" }");

        JsonSchema result = schemaFactory.parse(rawSchema);

        assertNotNull(result);
    }

    @Test
    public void parse_shouldReturnAnDefaultObjectSchema_givenATypeObjectWithNoAdditionalInformation() throws Exception {
        URL rawSchema = saveSchemaToURL("{ \"type\": \"object\" }");

        ObjectSchema compiledSchema = (ObjectSchema) schemaFactory.parse(rawSchema);

        assertTrue(compiledSchema.getProperties().isEmpty());
        assertSame(ObjectSchema.ALLOW_ALL_ADDITIONAL_PROPERTIES, compiledSchema.getAdditionalProperties());
    }

    @Test
    public void parse_shouldReturnAnObjectSchemaThatAllowsAllAdditionalProperties_givenTypeObjectWithAdditionalPropertiesTrue() throws Exception {
        URL rawSchema = saveSchemaToURL("{ \"type\": \"object\", \"additionalProperties\": true }");

        ObjectSchema compiledSchema = (ObjectSchema) schemaFactory.parse(rawSchema);

        assertSame(ObjectSchema.ALLOW_ALL_ADDITIONAL_PROPERTIES, compiledSchema.getAdditionalProperties());
    }

    @Test
    public void parse_shouldReturnAnObjectSchemaThatForbidsAnyAdditionalProperties_givenTypeObjectWithAdditionalPropertiesFalse() throws Exception {
        URL rawSchema = saveSchemaToURL("{ \"type\": \"object\", \"additionalProperties\": false }");

        ObjectSchema compiledSchema = (ObjectSchema) schemaFactory.parse(rawSchema);

        assertSame(ObjectSchema.FORBID_ANY_ADDITIONAL_PROPERTIES, compiledSchema.getAdditionalProperties());
    }

    @Test
    public void parse_shouldReturnAnObjectSchemaWithACustomNestedSchemaForAdditionalProperties_givenTypeObjectWithAdditionalPropertiesAsAnObject() throws Exception {
        URL rawSchema = saveSchemaToURL("{ \"type\": \"object\", \"additionalProperties\": { \"type\": \"array\" } }");

        ObjectSchema compiledSchema = (ObjectSchema) schemaFactory.parse(rawSchema);

        assertTrue(compiledSchema.getAdditionalProperties() instanceof ArraySchema);
    }

    @Test
    public void parse_shouldReturnAnObjectSchemaWithAppropriatePropertiesAndNestedSchema_givenTypeObjectWithPropertiesSet() throws Exception {
        URL rawSchema = saveSchemaToURL("{ " +
            "\"type\": \"object\"," +
            "\"properties\": {" +
                "\"foo\": { \"type\": \"string\", \"required\": true }," +
                "\"bar\": { \"type\": \"object\", \"required\": false, \"properties\": {" +
                    "\"barA\": { \"type\": \"number\"}" +
                "}}" +
            "}"+
        "}");

        ObjectSchema compiledSchema = (ObjectSchema) schemaFactory.parse(rawSchema);

        assertEquals(2, compiledSchema.getProperties().size());
        assertEquals("foo", compiledSchema.getProperties().get(0).getName());
        assertTrue(compiledSchema.getProperties().get(0).isRequired());
        assertTrue(compiledSchema.getProperties().get(0).getNestedSchema() instanceof SimpleTypeSchema);
        assertEquals(SimpleType.STRING, ((SimpleTypeSchema)compiledSchema.getProperties().get(0).getNestedSchema()).getType());

        assertEquals("bar", compiledSchema.getProperties().get(1).getName());
        assertFalse(compiledSchema.getProperties().get(1).isRequired());
        assertTrue(compiledSchema.getProperties().get(1).getNestedSchema() instanceof ObjectSchema);
        assertEquals(1, ((ObjectSchema) compiledSchema.getProperties().get(1).getNestedSchema()).getProperties().size());
        assertEquals("barA", ((ObjectSchema) compiledSchema.getProperties().get(1).getNestedSchema()).getProperties().get(0).getName());
        assertFalse(((ObjectSchema) compiledSchema.getProperties().get(1).getNestedSchema()).getProperties().get(0).isRequired());
    }

    @Test
    public void parse_shouldReturnAnArraySchemaThatAllowsAnyItem_givenArrayObjectWithoutItemsPropertySet() throws Exception {
        URL rawSchema = saveSchemaToURL("{ \"type\": \"array\" }");

        ArraySchema compiledSchema = (ArraySchema) schemaFactory.parse(rawSchema);

        assertEquals(SimpleType.ANY, ((SimpleTypeSchema) compiledSchema.getItems()).getType());
    }

    @Test
    public void parse_shouldReturnAnArraySchemaWithAnMinItemSchema_givenArrayObjectWithMinItemsPropertySet() throws Exception {
        URL rawSchema = saveSchemaToURL("{\"type\": \"array\", \"minItems\": 1, \"items\": {\"type\": \"number\" }}");

        ArraySchema compiledSchema = (ArraySchema) schemaFactory.parse(rawSchema);

        assertEquals(SimpleType.NUMBER, ((SimpleTypeSchema) compiledSchema.getItems()).getType());
        assertEquals(1, compiledSchema.getMinItems());
    }
    
    @Test
    public void parse_shouldReturnAnArraySchemaWithAnMaxItemSchema_givenArrayObjectWithMaxItemsPropertySet() throws Exception {
        URL rawSchema = saveSchemaToURL("{\"type\": \"array\", \"maxItems\": 3, \"items\": {\"type\": \"number\" }}");

        ArraySchema compiledSchema = (ArraySchema) schemaFactory.parse(rawSchema);

        assertEquals(SimpleType.NUMBER, ((SimpleTypeSchema) compiledSchema.getItems()).getType());
        assertEquals(3, compiledSchema.getMaxItems());
    }

    @Test
    public void parse_shouldReturnAnArraySchema_givenArrayObjectWithMinAndMaxItemsPropertySet() throws Exception {
        URL rawSchema = saveSchemaToURL("{\"type\": \"array\", \"minItems\": 1, \"maxItems\": 3, \"items\": {\"type\": \"number\" }}");

        ArraySchema compiledSchema = (ArraySchema) schemaFactory.parse(rawSchema);

        assertEquals(SimpleType.NUMBER, ((SimpleTypeSchema) compiledSchema.getItems()).getType());
        assertEquals(1, compiledSchema.getMinItems());
        assertEquals(3, compiledSchema.getMaxItems());
    }

    @Test
    public void parse_shouldThrowAnException_givenAnUnknownType() throws Exception {
        String unknownType = "random-type";
        URL rawSchema = saveSchemaToURL("{ \"type\": \"" + unknownType + "\" }");

        try {
            schemaFactory.parse(rawSchema);
            fail("expected an exception");
        } catch (IllegalArgumentException e) {
            assert e.getMessage().toLowerCase().contains("illegal schema type");
            assert e.getMessage().contains(unknownType);
        }
    }
    
    @Test
    public void parse_shouldReturnASchemaLinkWithAnAppropriateUrl_givenASchemaWithADollarRefThatContainsARelativeUrlReferencingASchemaThatIsAlreadyRegistered() throws Exception {
        File rawSchema = saveSchemaToFile("{ \"$ref\": \"foo\"}");
        URL referencedSchemaLocation = new File(rawSchema.getParentFile(), "foo").toURI().toURL();

        registry.registerSchema(referencedSchemaLocation, new SimpleTypeSchema() {{setType(SimpleType.ANY);}});

        SchemaReference result = (SchemaReference) schemaFactory.parse(rawSchema.toURI().toURL());

        assertEquals(registry, result.getRegistry());
        assertEquals(referencedSchemaLocation, result.getSchemaLocation());
    }

    @Test
    public void parse_shouldRegisterAnyAdditionalSchemasReferencedInTheSchema_whenTheyHaveNotAlreadyBeenRegistered() throws Exception {
        //create system of dependent schema files with a (trivial) cycle
        URL leafSchema = saveSchemaToURL("{ \"type\": \"any\" }");
        String relativeLinkToTheLeafSchema = leafSchema.getFile();

        URL selfReferentialIntermediateSchema = saveSchemaToURL("{" +
            "\"type\": \"object\"," +
            "\"properties\": {" +
                "\"foo\": {\"$ref\": \"" + leafSchema + "\"}," +
                "\"bar\": {\"$ref\": \"\"}" +
            "}}");

        URL rootSchema = saveSchemaToURL("{" +
            "\"type\": \"object\"," +
            "\"properties\": { " +
                "\"foo\": {\"$ref\": \"" + selfReferentialIntermediateSchema + "\"}," +
                "\"bar\": {\"$ref\": \"" + relativeLinkToTheLeafSchema + "\"}" +
            "}}");

        //preconditions
        assertFalse(registry.hasSchema(leafSchema));
        assertFalse(registry.hasSchema(selfReferentialIntermediateSchema));
        assertFalse(registry.hasSchema(rootSchema));

        //execute
        ObjectSchema compiledRootSchema = (ObjectSchema) schemaFactory.parse(rootSchema);

        //assert
        SimpleTypeSchema compiledLeafSchema = (SimpleTypeSchema) registry.getSchema(leafSchema);
        assertEquals(SimpleType.ANY, compiledLeafSchema.getType());

        ObjectSchema compiledIntermediateSchema = (ObjectSchema) registry.getSchema(selfReferentialIntermediateSchema);

        assertEquals(findNestedSchemaForProperty(SchemaReference.class, compiledIntermediateSchema, "foo").getSchemaLocation(), leafSchema);
        assertEquals(findNestedSchemaForProperty(SchemaReference.class, compiledIntermediateSchema, "bar").getSchemaLocation(), selfReferentialIntermediateSchema);

        assertSame(compiledRootSchema, registry.getSchema(rootSchema));
        assertEquals(findNestedSchemaForProperty(SchemaReference.class, compiledRootSchema, "foo").getSchemaLocation(), selfReferentialIntermediateSchema);
        assertEquals(findNestedSchemaForProperty(SchemaReference.class, compiledRootSchema, "bar").getSchemaLocation(), leafSchema);
    }

    @Test(expected = IllegalArgumentException.class)
    public void parse_shouldThrowAnException_givenASchemaWithReferencesASchemaThatDoesNotExist() throws Exception {
       URL missingReferenceSchema = saveSchemaToURL("{\"type\": \"object\"," +
           "\"properties\": {" +
               "\"foo\": {\"$ref\": \"file:///non/existent/file/url\"}" +
           "}}"
       );

        schemaFactory.parse(missingReferenceSchema);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void parse_shouldThrowAnException_givenASchemaWithReferencesASchemaThatContainsInvalidJson() throws Exception {
        URL invalidReferencedSchema = saveSchemaToURL(" not valid json ");
        URL rootSchema = saveSchemaToURL("{\"type\": \"object\"," +
           "\"properties\": {" +
               "\"foo\": {\"$ref\": \"" + invalidReferencedSchema + "\"}" +
           "}}"
       );

        schemaFactory.parse(rootSchema);
    }

    @Test
    public void parse_shouldThrowARuntimeException_givenASchemaWithAReferenceThatIsAMalformedURL() throws Exception {
         URL rootSchema = saveSchemaToURL("{\"type\": \"object\"," +
           "\"properties\": {" +
               "\"foo\": {\"$ref\": \"fil://#badUrl\"}" +
           "}}"
       );

        try {
            schemaFactory.parse(rootSchema);
            fail("Expected an exception");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getCause() instanceof MalformedURLException);
        }
    }

    private <T extends JsonSchema> T findNestedSchemaForProperty(Class<T> schemaReferenceClass, ObjectSchema objectSchema, String propertyName) {
        for(ObjectSchema.Property property : objectSchema.getProperties()) {
            if (property.getName().equals(propertyName)) {
                return schemaReferenceClass.cast(property.getNestedSchema());
            }
        }
        return null;
    }

    private URL saveSchemaToURL(String schemaDefinition) throws Exception {
        return saveSchemaToFile(schemaDefinition).toURI().toURL();
    }

    private File saveSchemaToFile(String schemaDefinition) throws Exception {
        File schemaFile = File.createTempFile("schema", ".json");
        schemaFile.deleteOnExit();
        filesToDelete.add(schemaFile);

        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(schemaFile, false), "UTF-8")) {
            writer.write(schemaDefinition);
            writer.flush();
        }

        return schemaFile;
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    @After
    public void tearDown() {
        for (File f : filesToDelete) {
            f.delete();
        }
    }
}