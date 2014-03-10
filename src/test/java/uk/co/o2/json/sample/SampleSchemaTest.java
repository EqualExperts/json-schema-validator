package uk.co.o2.json.sample;

import org.junit.Test;
import uk.co.o2.json.schema.ErrorMessage;
import uk.co.o2.json.schema.JsonSchema;
import uk.co.o2.json.schema.SchemaPassThroughCache;

import javax.json.Json;
import javax.json.JsonReader;
import javax.json.JsonValue;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class SampleSchemaTest {

    private SchemaPassThroughCache schemaFactory = new SchemaPassThroughCache(Json.createReaderFactory(null));

    @Test
    public void validateSchema_shouldWorkFromADifferentPackage() throws Exception {
        JsonSchema schema = schemaFactory.getSchema(getClass().getClassLoader().getResource("sample-json-schema.json"));
        try (JsonReader reader = Json.createReader(getClass().getClassLoader().getResourceAsStream("valid-json-document.json"))) {
            JsonValue json = reader.read();

            List<ErrorMessage> errors = schema.validate(json);

            assertTrue(errors.isEmpty());
        }
    }

}
