package uk.co.o2.json.schema

import org.codehaus.jackson.JsonNode
import org.codehaus.jackson.node.TextNode
import org.gmock.WithGMock
import org.junit.Before
import org.junit.Test

@WithGMock
class SchemaReferenceTest {

    SchemaPassThroughCache registry

    @Before
    void setUp() {
        registry = mock(SchemaPassThroughCache)
    }

    @Test
    void constructor_shouldSetRegistryAndSchemaLocationFields() {
        URL schemaLocation = new URL("http://www.example.com/")

        SchemaReference ref  = new SchemaReference(registry, schemaLocation)

        assert ref.registry.is(registry)
        assert ref.schemaLocation.is(schemaLocation)
    }

    @Test
    void validate_shouldLoadAJsonSchemaFromTheRegistryAndDelegateTheValidateCall() {
        URL expectedSchemaLocation = new URL("http://www.example.com/")
        JsonNode expectedDocument = new TextNode("I am a document!!!")
        List<ErrorMessage> expectedResults = [new ErrorMessage("foo", "bar")]

        JsonSchema mockSchema = mock(JsonSchema)
        mockSchema.validate(expectedDocument).returns(expectedResults)
        registry.getSchema(expectedSchemaLocation).returns(mockSchema)

        play {
            JsonSchema schema = new SchemaReference(registry, expectedSchemaLocation)
            List<ErrorMessage> results = schema.validate(expectedDocument)

            assert results.is(expectedResults)
        }
    }
}
