package uk.co.o2.json.schema.jaxrs;

import uk.co.o2.json.schema.ErrorMessage;
import uk.co.o2.json.schema.JsonSchema;
import uk.co.o2.json.schema.SchemaPassThroughCache;

import javax.json.*;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.List;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JsonSchemaProvider implements MessageBodyReader<JsonStructure> {

    private final SchemaPassThroughCache cache;
    private SchemaLookup schemaLookup;

    public JsonSchemaProvider(SchemaLookup schemaLookup) {
        cache = new SchemaPassThroughCache(Json.createReaderFactory(null));
        this.schemaLookup = schemaLookup;
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        for (Annotation annotation : annotations) {
            if (annotation.annotationType().equals(Schema.class)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public JsonStructure readFrom(Class<JsonStructure> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException {
        Schema schemaAnnotation = null;
        for (Annotation annotation : annotations) {
            if (annotation.annotationType().equals(Schema.class)) {
                schemaAnnotation = (Schema) annotation;
                break;
            }
        }

        if (schemaAnnotation != null) {
            JsonReader reader = Json.createReader(entityStream);
            URL schemaLocation = schemaLookup.getSchemaURL(schemaAnnotation.value());
            JsonSchema jsonSchema = cache.getSchema(schemaLocation);
            JsonStructure jsonStructure = reader.read();
            List<ErrorMessage> validationErrors = jsonSchema.validate(jsonStructure);
            if (validationErrors.isEmpty()) {
                return jsonStructure;
            }

            throw new WebApplicationException(generateErrorMessage(validationErrors));
        } else {
            throw new WebApplicationException(400);
        }
    }

    protected Response generateErrorMessage(List<ErrorMessage> validationErrors) {
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        for (ErrorMessage error : validationErrors) {
            arrayBuilder.add(Json.createObjectBuilder().add(error.getLocation(), error.getMessage()));
        }
        JsonObject errors = Json.createObjectBuilder().add("errors", arrayBuilder.build()).build();
        return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON).entity(errors).build();
    }
}