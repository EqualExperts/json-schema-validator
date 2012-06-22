package uk.co.o2.json.sample;


import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;
import uk.co.o2.json.schema.ErrorMessage;
import uk.co.o2.json.schema.JsonSchema;
import uk.co.o2.json.schema.SchemaPassThroughCache;

import java.util.List;

import static org.junit.Assert.assertTrue;

public class ListOfMultipleTypesSchemaTest {

    private JsonFactory jsonFactory = new JsonFactory(new ObjectMapper());
    private SchemaPassThroughCache schemaFactory = new SchemaPassThroughCache(jsonFactory);

    /*
        Schema and json tested correctly on http://jsonschemalint.com/ 
     */
    @Test
    public void canSuccessfullyValidateAListThatContainsObjectsAndSimpleTypes() throws Exception {
        JsonSchema schema = schemaFactory.getSchema(getClass().getClassLoader().getResource("list-multiple-types.json"));
        JsonNode json = jsonFactory.createJsonParser(getClass().getClassLoader().getResource("list-multiple-types-valid-document.json")).readValueAsTree();

        List<ErrorMessage> errors = schema.validate(json);

        assertTrue(errors.isEmpty());
    }

}
