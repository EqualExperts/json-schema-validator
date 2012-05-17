package uk.co.o2.json.schema

import org.codehaus.jackson.JsonFactory
import org.codehaus.jackson.map.ObjectMapper
import org.gmock.WithGMock
import org.junit.After
import org.junit.Test
import static org.junit.Assert.fail
import org.codehaus.jackson.JsonParseException

@WithGMock
class SchemaCompilerTest {
    private static JsonFactory jsonFactory = new JsonFactory(new ObjectMapper())

    private final List<File> filesToDelete = []

    private SchemaPassThroughCache registry = new SchemaPassThroughCache(jsonFactory)
    private SchemaCompiler schemaFactory = new SchemaCompiler(registry, jsonFactory)
    private URL defaultSchemaLocation = new URL("http://www.example.com/")

    @Test(expected = IllegalArgumentException)
    void parse_shouldThrowAnException_givenAJsonNodeThatIsNotAnObjectNode() {

        URL schemaURL = saveSchemaToURL('''["notASchema"]''')

        schemaFactory.parse(schemaURL)
    }

    @Test
    void parse_shouldThrowAnException_giveAUrlToASchemaThatDoesNotExist(){
        try {
            schemaFactory.parse("file:///non/existent/file/url".toURL())
            fail "should throw"
        }
        catch (IllegalArgumentException ex){
            assert ex.cause instanceof IOException
        }
    }

    @Test
    void parse_shouldThrowAnException_givenAUrlToASchemaThatDoesntContainValidJson() {
        URL schemaURL = saveSchemaToURL('not valid json')

        try {
            schemaFactory.parse(schemaURL)
            fail "should throw"
        }
        catch (IllegalArgumentException ex){
            assert ex.message.contains("invalid JSON")
            assert ex.cause instanceof JsonParseException
        }
    }

    @Test
    void parse_shouldReturnASimpleTypeSchema_givenATypeString() {
        URL rawSchema = saveSchemaToURL('''{
            "type": "string"
        }''')

        SimpleTypeSchema compiledSchema = schemaFactory.parse(rawSchema) as SimpleTypeSchema

        assert compiledSchema.type == SimpleType.STRING
        assert compiledSchema.pattern == null
        assert compiledSchema.format == null
    }

    @Test
    void parse_shouldSetARegexPattern_givenASimpleTypeStringWithAPattern() {
        URL rawSchema = saveSchemaToURL('''{
            "type": "string",
            "pattern": "12345"
        }''')

        SimpleTypeSchema compiledSchema = schemaFactory.parse(rawSchema) as SimpleTypeSchema

        assert compiledSchema.type == SimpleType.STRING
        assert compiledSchema.pattern.pattern() == '12345'
    }

    @Test
    void parse_shouldReturnASimpleTypeSchema_givenATypeNumber() {
        URL rawSchema = saveSchemaToURL('''{
            "type": "number"
        }''')

        SimpleTypeSchema compiledSchema = schemaFactory.parse(rawSchema) as SimpleTypeSchema

        assert compiledSchema.type == SimpleType.NUMBER
        assert !compiledSchema.pattern
    }

    @Test
    void parse_shouldReturnASimpleTypeSchema_givenATypeInteger() {
        URL rawSchema = saveSchemaToURL('''{
            "type": "integer"
        }''')

        SimpleTypeSchema compiledSchema = schemaFactory.parse(rawSchema) as SimpleTypeSchema

        assert compiledSchema.type == SimpleType.INTEGER
        assert !compiledSchema.pattern
    }

    @Test
    void parse_shouldReturnASimpleTypeSchema_givenATypeBoolean() {
        URL rawSchema = saveSchemaToURL('''{
            "type": "boolean"
        }''')

        SimpleTypeSchema compiledSchema = schemaFactory.parse(rawSchema) as SimpleTypeSchema

        assert compiledSchema.type == SimpleType.BOOLEAN
        assert !compiledSchema.pattern
    }

    @Test
    void parse_shouldReturnASimpleTypeSchema_givenATypeNull() {
        URL rawSchema = saveSchemaToURL('''{
            "type": "null"
        }''')

        SimpleTypeSchema compiledSchema = schemaFactory.parse(rawSchema) as SimpleTypeSchema

        assert compiledSchema.type == SimpleType.NULL
        assert !compiledSchema.pattern
    }

    @Test
    void parse_shouldReturnASimpleTypeSchema_givenATypeAny() {
        URL rawSchema = saveSchemaToURL('''{
            "type": "any"
        }''')

        SimpleTypeSchema compiledSchema = schemaFactory.parse(rawSchema) as SimpleTypeSchema

        assert compiledSchema.type == SimpleType.ANY
        assert !compiledSchema.pattern
    }

    @Test(expected = IllegalArgumentException)
    void parse_shouldThrowAnException_givenASimpleTypeThatIsNotStringButHasAPattern() {
        URL rawSchema = saveSchemaToURL('''{
            "type": "integer",
            "pattern": "12345"
        }''')

        schemaFactory.parse(rawSchema)
    }

    @Test(expected = IllegalArgumentException)
    void parse_shouldThrowAnException_givenASimpleTypeThatIsNotCompatibleWithTheFormat() {
        URL rawSchema = saveSchemaToURL('''{
            "type": "integer",
            "format": "date-time"
        }''')

        schemaFactory.parse(rawSchema)
    }

    @Test
    void parse_shouldReturnASimpleTypeSchemaWithAFormat_givenAFormat() {
        URL rawSchema = saveSchemaToURL('''{
            "type": "string",
            "format": "date-time"
        }''')

        SimpleTypeSchema compiledSchema = schemaFactory.parse(rawSchema) as SimpleTypeSchema

        assert compiledSchema.format == 'date-time'
    }

    @Test
    void parse_shouldReturnASimpleTypeSchemaWithMinLength_givenAMinLengthForTypeString() {
        URL rawSchema = saveSchemaToURL('''{
            "type": "string",
            "minLength": 10
        }''')

        SimpleTypeSchema compiledSchema = schemaFactory.parse(rawSchema) as SimpleTypeSchema

        assert compiledSchema.minLength == 10
    }

    @Test
    void parse_shouldReturnASimpleTypeSchemaWithMinLength_givenAMaxLengthForTypeString() {
        URL rawSchema = saveSchemaToURL('''{
            "type": "string",
            "minLength": 10,
            "maxLength": 20
        }''')

        SimpleTypeSchema compiledSchema = schemaFactory.parse(rawSchema) as SimpleTypeSchema

        assert compiledSchema.minLength == 10
        assert compiledSchema.maxLength == 20
    }

    @Test(expected = IllegalArgumentException)
    void parse_shouldThrowException_givenAMaxLengthForTypeOtherThanString() {
        URL rawSchema = saveSchemaToURL('''{
            "type": "number",
            "maxLength": 20
        }''')

        schemaFactory.parse(rawSchema)
    }

    @Test(expected = IllegalArgumentException)
    void parse_shouldThrowException_givenAMinLengthForTypeOtherThanString() {
        URL rawSchema = saveSchemaToURL('''{
            "type": "number",
            "minLength": 20
        }''')

        schemaFactory.parse(rawSchema)
    }

    @Test
    void parse_shouldReturnASimpleTypeSchemaWithEnumeration_givenAnEnumeration() {
        URL rawSchema = saveSchemaToURL('''{
            "type": "string",
            "enumeration": ["A","B"]
        }''')

        SimpleTypeSchema compiledSchema = schemaFactory.parse(rawSchema) as SimpleTypeSchema

        assert compiledSchema.enumeration.size() == 2
    }

    @Test(expected = IllegalArgumentException)
    void parse_shouldThrowException_givenAnEnumerationForTypeNull() {
        URL rawSchema = saveSchemaToURL('''{
            "type": "null",
            "enumeration": ["A","B"]
        }''')

        schemaFactory.parse(rawSchema)
    }

    @Test(expected = IllegalArgumentException)
    void parse_shouldThrowException_givenAnEnumerationForTypeAny() {
        URL rawSchema = saveSchemaToURL('''{
            "type": "any",
            "enumeration": ["A","B"]
        }''')

        schemaFactory.parse(rawSchema)
    }

    @Test
    void parse_shouldReturnASimpleTypeSchemaWithMinimum_givenAMinimumPropertyForIntegerType() {
        URL rawSchema = saveSchemaToURL('''{
            "type": "integer",
            "minimum": 10
        }''')

        SimpleTypeSchema compiledSchema = schemaFactory.parse(rawSchema) as SimpleTypeSchema

        assert compiledSchema.minimum == 10
        assert compiledSchema.minimum.toString() == "10"
    }

    @Test
    void parse_shouldReturnASimpleTypeSchemaWithMinimum_givenAMinimumPropertyForNumberType() {
        URL rawSchema = saveSchemaToURL('''{
            "type": "number",
            "minimum": 10.05
        }''')

        SimpleTypeSchema compiledSchema = schemaFactory.parse(rawSchema) as SimpleTypeSchema

        assert compiledSchema.minimum == 10.05
        assert compiledSchema.minimum.toString() == "10.05"
    }

    @Test(expected = IllegalArgumentException)
    void parse_shouldThrowException_givenMinimumPropertyOtherThanNumberAndIntegerSimpleType() {
        URL rawSchema = saveSchemaToURL('''{
            "type": "string",
            "minimum": 10
        }''')

        schemaFactory.parse(rawSchema)
    }

    @Test
    void parse_shouldReturnASimpleTypeSchemaWithMaximum_givenAMaximumPropertyForIntegerType() {
        URL rawSchema = saveSchemaToURL('''{
            "type": "integer",
            "maximum": 10
        }''')

        SimpleTypeSchema compiledSchema = schemaFactory.parse(rawSchema) as SimpleTypeSchema

        assert compiledSchema.maximum == 10
        assert compiledSchema.maximum.toString() == "10"
    }

    @Test
    void parse_shouldReturnASimpleTypeSchemaWithMaximum_givenAMaximumPropertyForNumberType() {
        URL rawSchema = saveSchemaToURL('''{
            "type": "number",
            "maximum": 10.05
        }''')

        SimpleTypeSchema compiledSchema = schemaFactory.parse(rawSchema) as SimpleTypeSchema

        assert compiledSchema.maximum == 10.05
        assert compiledSchema.maximum.toString() == "10.05"
    }

    @Test(expected = IllegalArgumentException)
    void parse_shouldThrowException_givenMaximumPropertyOtherThanNumberAndIntegerSimpleType() {
        URL rawSchema = saveSchemaToURL('''{
            "type": "string",
            "maximum": 10
        }''')

        schemaFactory.parse(rawSchema)
    }

    @Test
    void parse_shouldReturnASimpleTypeSchemaWithExclusiveMinimum_givenAExclusiveMinimumPropertyForIntegerType() {
        URL rawSchema = saveSchemaToURL('''{
            "type": "integer",
            "exclusiveMinimum": true
        }''')

        SimpleTypeSchema compiledSchema = schemaFactory.parse(rawSchema) as SimpleTypeSchema

        assert compiledSchema.exclusiveMinimum

    }

    @Test
    void parse_shouldReturnASimpleTypeSchemaWithExclusiveMinimum_givenAExclusiveMinimumPropertyForNumberType() {
        URL rawSchema = saveSchemaToURL('''{
            "type": "number",
            "exclusiveMinimum": true
        }''')

        SimpleTypeSchema compiledSchema = schemaFactory.parse(rawSchema) as SimpleTypeSchema

        assert compiledSchema.exclusiveMinimum
    }

    @Test(expected = IllegalArgumentException)
    void parse_shouldThrowException_givenExclusiveMinimumPropertyOtherThanNumberAndIntegerSimpleType() {
        URL rawSchema = saveSchemaToURL('''{
            "type": "string",
            "exclusiveMinimum": true
        }''')

        schemaFactory.parse(rawSchema)
    }

    @Test
    void parse_shouldReturnASimpleTypeSchemaWithExclusiveMaximum_givenAExclusiveMaximumPropertyForIntegerType() {
        URL rawSchema = saveSchemaToURL('''{
            "type": "integer",
            "exclusiveMaximum": true
        }''')

        SimpleTypeSchema compiledSchema = schemaFactory.parse(rawSchema) as SimpleTypeSchema

        assert compiledSchema.exclusiveMaximum
    }

    @Test
    void parse_shouldReturnASimpleTypeSchemaWithExclusiveMaximum_givenAExclusiveMaximumPropertyForNumberType() {
        URL rawSchema = saveSchemaToURL('''{
            "type": "number",
            "exclusiveMaximum": true
        }''')

        SimpleTypeSchema compiledSchema = schemaFactory.parse(rawSchema) as SimpleTypeSchema

        assert compiledSchema.exclusiveMaximum
    }

    @Test(expected = IllegalArgumentException)
    void parse_shouldThrowException_givenExclusiveMaximumPropertyOtherThanNumberAndIntegerSimpleType() {
        URL rawSchema = saveSchemaToURL('''{
            "type": "string",
            "exclusiveMaximum": true
        }''')

        schemaFactory.parse(rawSchema)
    }

    @Test
    void parse_ShouldReturnAJsonSchema() {
        URL rawSchema = saveSchemaToURL('''{
            "type": "integer"
        }''')

        JsonSchema result = schemaFactory.parse(rawSchema)

        assert result instanceof JsonSchema
    }

    @Test
    void parse_shouldReturnAnDefaultObjectSchema_givenATypeObjectWithNoAdditionalInformation() {
        URL rawSchema = saveSchemaToURL('''{
            "type": "object"
        }''')

        ObjectSchema compiledSchema = schemaFactory.parse(rawSchema) as ObjectSchema

        assert compiledSchema.properties.empty
        assert compiledSchema.additionalProperties.is(ObjectSchema.ALLOW_ALL_ADDITIONAL_PROPERTIES)
    }

    @Test
    void parse_shouldReturnAnObjectSchemaThatAllowsAllAdditionalProperties_givenTypeObjectWithAdditionalPropertiesTrue() {
        URL rawSchema = saveSchemaToURL('''{
            "type": "object",
            "additionalProperties": true
        }''')

        ObjectSchema compiledSchema = schemaFactory.parse(rawSchema) as ObjectSchema

        assert compiledSchema.additionalProperties.is(ObjectSchema.ALLOW_ALL_ADDITIONAL_PROPERTIES)
    }

    @Test
    void parse_shouldReturnAnObjectSchemaThatForbidsAnyAdditionalProperties_givenTypeObjectWithAdditionalPropertiesFalse() {
        URL rawSchema = saveSchemaToURL('''{
            "type": "object",
            "additionalProperties": false
        }''')

        ObjectSchema compiledSchema = schemaFactory.parse(rawSchema) as ObjectSchema

        assert compiledSchema.additionalProperties.is(ObjectSchema.FORBID_ANY_ADDITIONAL_PROPERTIES)
    }

    @Test
    void parse_shouldReturnAnObjectSchemaWithACustomNestedSchemaForAdditionalProperties_givenTypeObjectWithAdditionalPropertiesAsAnObject() {
        URL rawSchema = saveSchemaToURL('''{
            "type": "object",
            "additionalProperties": {"type": "array"}
        }''')

        ObjectSchema compiledSchema = schemaFactory.parse(rawSchema) as ObjectSchema

        assert compiledSchema.additionalProperties instanceof ArraySchema
    }

    @Test
    void parse_shouldReturnAnObjectSchemaWithAppropriatePropertiesAndNestedSchema_givenTypeObjectWithPropertiesSet() {
        URL rawSchema = saveSchemaToURL('''{
            "type": "object",
            "properties": {
                "foo": { "type": "string", "required": true },
                "bar": { "type": "object", "required": false, "properties": {
                    "barA": { "type": "number"}
                }}
            }
        }''')

        ObjectSchema compiledSchema = (ObjectSchema) schemaFactory.parse(rawSchema)

        assert compiledSchema.properties.size() == 2
        assert compiledSchema.properties[0].name == "foo"
        assert compiledSchema.properties[0].required
        assert compiledSchema.properties[0].nestedSchema instanceof SimpleTypeSchema
        assert (compiledSchema.properties[0].nestedSchema as SimpleTypeSchema).type == SimpleType.STRING

        assert compiledSchema.properties[1].name == "bar"
        assert !compiledSchema.properties[1].required
        assert compiledSchema.properties[1].nestedSchema instanceof ObjectSchema
        assert (compiledSchema.properties[1].nestedSchema as ObjectSchema).properties.size() == 1
        assert (compiledSchema.properties[1].nestedSchema as ObjectSchema).properties[0].name == 'barA'
        assert !(compiledSchema.properties[1].nestedSchema as ObjectSchema).properties[0].required
    }

    @Test
    void parse_shouldReturnAnArraySchemaThatAllowsAnyItem_givenArrayObjectWithoutItemsPropertySet() {
        URL rawSchema = saveSchemaToURL('''{
            "type": "array"
            }
        }''')

        ArraySchema compiledSchema = schemaFactory.parse(rawSchema) as ArraySchema

        assert (compiledSchema.items as SimpleTypeSchema).type == SimpleType.ANY
    }

    @Test
    void parse_shouldReturnAnArraySchemaWithAnMinItemSchema_givenArrayObjectWithMinItemsPropertySet() {
        URL rawSchema = saveSchemaToURL('''{
            "type": "array",
            "minItems":1,
            "items": { "type": "number" }
            }
        }''')

        ArraySchema compiledSchema = schemaFactory.parse(rawSchema) as ArraySchema

        assert (compiledSchema.items as SimpleTypeSchema).type == SimpleType.NUMBER
        assert compiledSchema.minItems == 1
    }
    
    @Test
    void parse_shouldReturnAnArraySchemaWithAnMaxItemSchema_givenArrayObjectWithMaxItemsPropertySet() {
        URL rawSchema = saveSchemaToURL('''{
            "type": "array",
            "maxItems":3,
            "items": { "type": "number" }
            }
        }''')

        ArraySchema compiledSchema = schemaFactory.parse(rawSchema) as ArraySchema

        assert (compiledSchema.items as SimpleTypeSchema).type == SimpleType.NUMBER
        assert compiledSchema.maxItems == 3
    }

    @Test
    void parse_shouldReturnAnArraySchemaWithUniqueItems_givenArrayObjectWithUniqueItemsPropertySet() {
        URL rawSchema = saveSchemaToURL('''{
            "type": "array",
            "uniqueItems": true
            }
        }''')

        ArraySchema compiledSchema = schemaFactory.parse(rawSchema) as ArraySchema

        assert compiledSchema.uniqueItems
    }

    @Test
    void parse_shouldReturnAnArraySchema_givenArrayObjectWithMinAndMaxItemsPropertySet() {
        URL rawSchema = saveSchemaToURL('''{
            "type": "array",
            "minItems":1,
            "maxItems":3,
            "items": { "type": "number" }
            }
        }''')

        ArraySchema compiledSchema = schemaFactory.parse(rawSchema) as ArraySchema

        assert (compiledSchema.items as SimpleTypeSchema).type == SimpleType.NUMBER
        assert compiledSchema.minItems == 1
        assert compiledSchema.maxItems == 3
    }

    @Test
    void parse_shouldThrowAnException_givenAnUnknownType() {
        String unknownType = 'random-type'
        URL rawSchema = saveSchemaToURL("""{
            "type": "${unknownType}"
            }
        }""")

        try {
            schemaFactory.parse(rawSchema)
            fail('expected an exception')
        } catch (IllegalArgumentException e) {
            assert e.message.toLowerCase().contains('illegal schema type')
            assert e.message.contains(unknownType)
        }
    }
    
    @Test
    void parse_shouldReturnASchemaLinkWithAnAppropriateUrl_givenASchemaWithADollarRefThatContainsARelativeUrlReferencingASchemaThatIsAlreadyRegistered() {
        File rawSchema = saveSchemaToFile("""{
            "\$ref": "foo"
            }
        }""")

        URL referencedSchemaLocation = new File(rawSchema.parentFile, "foo").toURI().toURL()
        registry.registerSchema(referencedSchemaLocation, new SimpleTypeSchema(type: SimpleType.ANY))

        SchemaReference result = schemaFactory.parse(rawSchema.toURI().toURL()) as SchemaReference

        assert result.registry == registry
        assert result.schemaLocation == referencedSchemaLocation
    }

    @Test
    void parse_shouldRegisterAnyAdditionalSchemasReferencedInTheSchema_whenTheyHaveNotAlreadyBeenRegistered() {
        //create system of dependent schema files with a (trivial) cycle
        URL leafSchema = saveSchemaToURL("""{
            "type": "any"
        }""")
        String relativeLinkToTheLeafSchema = leafSchema.file

        URL selfReferentialIntermediateSchema = saveSchemaToURL("""{
            "type": "object",
            "properties": {
                "foo": {
                    "\$ref": "${leafSchema}"
                },
                "bar": {
                    "\$ref": ""
                }
            }
        }""")

        URL rootSchema = saveSchemaToURL("""{
            "type": "object",
            "properties": {
                "foo": {
                    "\$ref": "${selfReferentialIntermediateSchema}"
                },
                "bar": {
                    "\$ref": "${relativeLinkToTheLeafSchema}"
                }
            }
        }
        """)

        //preconditions
        assert !registry.hasSchema(leafSchema)
        assert !registry.hasSchema(selfReferentialIntermediateSchema)
        assert !registry.hasSchema(rootSchema)

        //execute
        ObjectSchema compiledRootSchema = schemaFactory.parse(rootSchema) as ObjectSchema

        //assert
        SimpleTypeSchema compiledLeafSchema = registry.getSchema(leafSchema) as SimpleTypeSchema
        assert compiledLeafSchema.type == SimpleType.ANY

        ObjectSchema compiledIntermediateSchema = registry.getSchema(selfReferentialIntermediateSchema) as ObjectSchema
        assert (compiledIntermediateSchema.properties.find { it.name == 'foo' }.nestedSchema as SchemaReference).schemaLocation == leafSchema
        assert (compiledIntermediateSchema.properties.find { it.name == 'bar' }.nestedSchema as SchemaReference).schemaLocation == selfReferentialIntermediateSchema

        assert registry.getSchema(rootSchema).is(compiledRootSchema)
        assert (compiledRootSchema.properties.find { it.name == 'foo' }.nestedSchema as SchemaReference).schemaLocation == selfReferentialIntermediateSchema
        assert (compiledRootSchema.properties.find { it.name == 'bar' }.nestedSchema as SchemaReference).schemaLocation == leafSchema
    }

    @Test(expected = IllegalArgumentException)
    void parse_shouldThrowAnException_givenASchemaWithReferencesASchemaThatDoesNotExist() {
       URL missingReferenceSchema = saveSchemaToURL("""{
           "type": "object",
           "properties": {
               "foo": {
                   "\$ref": "file:///non/existent/file/url"
               }
           }
       }
       """)

        schemaFactory.parse(missingReferenceSchema)
    }

    @Test(expected = IllegalArgumentException)
    void parse_shouldThrowAnException_givenASchemaWithReferencesASchemaThatContainsInvalidJson() {
        URL invalidReferencedSchema = saveSchemaToURL(" not valid json ")
        URL rootSchema = saveSchemaToURL("""{
           "type": "object",
           "properties": {
               "foo": {
                   "\$ref": "${invalidReferencedSchema}"
               }
           }
        }
        """)

        schemaFactory.parse(rootSchema)
    }

    private URL saveSchemaToURL(String schemaDefinition) {
        saveSchemaToFile(schemaDefinition).toURI().toURL()
    }

    private File saveSchemaToFile(String schemaDefinition) {
        File schemaFile = File.createTempFile("schema", ".json")
        schemaFile.deleteOnExit()
        filesToDelete << schemaFile

        schemaFile.write(schemaDefinition, "UTF-8")

        schemaFile
    }

    @After
    public void tearDown() {
        filesToDelete.each { it.delete() }
    }
}