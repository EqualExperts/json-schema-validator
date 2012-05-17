package uk.co.o2.json.schema

import org.codehaus.jackson.JsonFactory
import org.codehaus.jackson.JsonNode
import org.codehaus.jackson.map.ObjectMapper
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

class JsonSchemaTest {
    private static final String validJsonDocument = """
        {
          "id" : 1,
          "brand" : "Samsung",
          "salePrice" : {
            "amount" : 10.00,
            "additionalProperty" : "xxxxxxx"
          },
          "costPrice" : {
            "amount" : 70.00,
            "additionalProperty" : "xxxxxxx"
          },
          "extraInfo": [
            {
              "info" : "green length"
            },
            {
              "info" : "big length"
            }
          ],
          "description": "some description",
          "regexValue" : "123.321.111.+",
          "nonNegativeValue" : 0
        }
    """
    private static final String invalidJsonDocument = """
        {
          "brand" : "AnyBrand",
          "salePrice" : {
            "amount" : 5.00
          },
          "costPrice" : {
            "amount" : 10.00
          },
          "extraInfo": [
            {
              "info" : "green length"
            },
            {
              "info" : "big length"
            },
            {
              "info" : "pink"
            },
            {
              "info" : "blue length XXX blue length"
            }
          ],
          "description": "short",
          "longDescription" : "some long description more than permitted limit",
          "regexValue" : "++",
          "nonNegativeValue" : -1
        }
    """
    private static final String jsonSchema = """
    {
       "type" : "object",
       "required" : true,
       "additionalProperties" : false,
       "properties" : {
           "id" : {"type" : "integer", "required" : true},
           "brand" : {"type" : "string", "required" : true, "enumeration": ["Nokia", "Samsung", "Apple"]},
           "salePrice" : {
                "type": "object",
                "required": true,
                "additionalProperties": true,
                "properties": {
                    "amount" : { "type": "number", "required": true, "minimum": 10.00, "maximum": 100.00}
                }
           },
           "costPrice" : {
                "type": "object",
                "required": true,
                "additionalProperties": true,
                "properties": {
                    "amount" : { "type": "number", "required": true, "minimum": 10.00, "maximum": 100.00, "exclusiveMaximum" : true, "exclusiveMinimum" : true}
                }
           },
           "extraInfo" : {
                "type" : "array",
                "additionalItems" : false,
                "minItems" : 1,
                "maxItems" : 3,
                "items" : {
                    "type" : "object",
                    "additionalProperties" : false,
                    "properties" : {
                        "info" : {"type" : "string", "required" : true}
                    }
                }
           },
           "description" : {"type" : "string", "required" : true, "minLength" : 10},
           "longDescription" : {"type" : "string", "minLength" : 10, "maxLength": 20},
           "regexValue" : {"type" : "string", "format" : "regex"},
           "nonNegativeValue" : {"type" : "integer", "minimum" : 0, "required":true}
       }
    }
    """

    private static File schemaFile

    @BeforeClass
    static void setUp() {
        schemaFile = File.createTempFile('sample-json-schema', '.json')
        schemaFile.deleteOnExit()

        schemaFile.write(jsonSchema, "UTF-8")
    }

    @Test
    public void happyCase_shouldValidateAJsonDocumentCorrectly() {
        JsonFactory jsonFactory = new JsonFactory(new ObjectMapper())
        SchemaCompiler schemaFactory = new SchemaCompiler(new SchemaPassThroughCache(jsonFactory), jsonFactory);
        JsonSchema compiledSchema = schemaFactory.parse(schemaFile.toURI().toURL())

        JsonNode json = jsonFactory.createJsonParser(validJsonDocument).readValueAsTree()

        List<ErrorMessage> errors = compiledSchema.validate(json)

        assert errors.empty
    }

    @Test
    void sadCase_shouldValidateAJsonDocumentAndReportErrors() {
        JsonFactory jsonFactory = new JsonFactory(new ObjectMapper())
        SchemaCompiler schemaFactory = new SchemaCompiler(new SchemaPassThroughCache(jsonFactory), jsonFactory);
        JsonSchema compiledSchema = schemaFactory.parse(schemaFile.toURI().toURL())

        JsonNode json = jsonFactory.createJsonParser(invalidJsonDocument).readValueAsTree()

        List<ErrorMessage> errors = compiledSchema.validate(json)

        assert errors.size() == 9
        assert errors[0].location == "id"
        assert errors[0].message == "Missing required property id"

        assert errors[1].location == "brand"
        assert errors[1].message == "Value \"AnyBrand\" must be one of: [\"Nokia\", \"Samsung\", \"Apple\"]"

        assert errors[2].location == "salePrice.amount"
        assert errors[2].message == "Value '5.0' must be greater or equal to 10.0"

        assert errors[3].location == "costPrice.amount"
        assert errors[3].message == "Value '10.0' must be greater than 10.0 when exclusiveMinimum is true"

        assert errors[4].location == "extraInfo"
        assert errors[4].message == "Current array size of 4 is greater than allowed maximum array size of 3"

        assert errors[5].location == "description"
        assert errors[5].message == "Value 'short' must be greater or equal to 10 characters"

        assert errors[6].location == "longDescription"
        assert errors[6].message == "Value 'some long description more than permitted limit' must be less or equal to 20 characters"

        assert errors[7].location == "regexValue"
        assert errors[7].message == "Value '++' is not a valid regex"

        assert errors[8].location == "nonNegativeValue"
        assert errors[8].message == "Value '-1' must be greater or equal to 0"
    }

    @AfterClass
    static void tearDown() {
        schemaFile.delete()
    }
}
