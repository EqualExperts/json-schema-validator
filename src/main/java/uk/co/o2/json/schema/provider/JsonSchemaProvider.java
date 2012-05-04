package uk.co.o2.json.schema.provider;


import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import uk.co.o2.json.schema.ErrorMessage;
import uk.co.o2.json.schema.JsonSchema;
import uk.co.o2.json.schema.SchemaPassThroughCache;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.List;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class JsonSchemaProvider extends JacksonJsonProvider {

    private final SchemaPassThroughCache cache;
    private SchemaLookup schemaLookup;

    public JsonSchemaProvider(SchemaLookup schemaLookup) {
        cache = new SchemaPassThroughCache(new JsonFactory(new ObjectMapper()));
        this.schemaLookup = schemaLookup;
        this.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
    }


    @Override
    public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException {
        Schema schemaAnnotation = null;
        for (Annotation annotation : annotations) {
            if (annotation.annotationType().equals(Schema.class)) {
                schemaAnnotation = (Schema) annotation;
                break;
            }
        }

        if (schemaAnnotation != null) {
            try {
                ObjectMapper mapper = locateMapper(type, mediaType);
                JsonParser jp = mapper.getJsonFactory().createJsonParser(entityStream);
                jp.disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);
                URL schemaLocation = schemaLookup.getSchemaURL(schemaAnnotation.value());
                JsonSchema jsonSchema = cache.getSchema(schemaLocation);
                JsonNode jsonNode = mapper.readTree(jp);
                List<ErrorMessage> validationErrors = jsonSchema.validate(jsonNode);
                if (validationErrors.isEmpty()) {
                    return mapper.readValue(jsonNode, mapper.constructType(genericType));
                }

                throw new WebApplicationException(generateErrorMessage(validationErrors));
            } catch (final EOFException e) {
                throw new ValidationException("incompleteInput", e.getMessage());
            } catch (final JsonParseException e) {
                throw new ValidationException("parsingError", e.getMessage());
            }
        } else {
            return super.readFrom(type, genericType, annotations, mediaType, httpHeaders, entityStream);
        }
    }

    protected Response generateErrorMessage(List<ErrorMessage> validationErrors) {
        StringBuilder content = new StringBuilder();
        for (ErrorMessage error : validationErrors) {
            content.append(error.getLocation());
            content.append((": "));
            content.append(error.getMessage());
            content.append("\n");
        }

        return Response.status(Response.Status.BAD_REQUEST).type(MediaType.TEXT_PLAIN).entity(content.toString()).build();
    }
}