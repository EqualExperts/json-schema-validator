package uk.co.o2.json.sample;


import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import uk.co.o2.json.schema.ErrorMessage;
import uk.co.o2.json.schema.JsonSchema;
import uk.co.o2.json.schema.SchemaPassThroughCache;

import java.util.List;

import static org.junit.Assert.assertTrue;

public class SampleSchemaTest {

    private JsonFactory jsonFactory = new JsonFactory(new ObjectMapper());
    private SchemaPassThroughCache schemaFactory = new SchemaPassThroughCache(jsonFactory);

    @Test
    public void validateSchema_shouldWorkFromADifferentPackage() throws Exception {
        JsonSchema schema = schemaFactory.getSchema(getClass().getClassLoader().getResource("sample-json-schema.json"));
        JsonNode json = jsonFactory.createJsonParser(getClass().getClassLoader().getResource("valid-json-document.json")).readValueAsTree();

        List<ErrorMessage> errors = schema.validate(json);

        assertTrue(errors.isEmpty());
    }

}
