package uk.co.o2.json.schema.jaxrs;

import org.junit.Test;
import uk.co.o2.json.schema.ErrorMessage;

import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
public class JsonSchemaProviderTest {
    @Test
    public void readFrom_shouldNotPerformSchemaValidation_whenNoAnnotationsArePresent() throws Exception {
        SchemaLookup schemaLookup = mock(SchemaLookup.class);
        JsonSchemaProvider provider = new JsonSchemaProvider(schemaLookup);
        InputStream inputStream = new ByteArrayInputStream("{\"name\": \"fred\"}".getBytes("UTF-8"));
        Class clazz =  DummyClass.class;
        
        DummyClass result = (DummyClass) provider.readFrom(clazz, DummyClass.class, new Annotation[0], MediaType.APPLICATION_JSON_TYPE, new DummyMultiValueMap<String, String>(), inputStream);
        
        assertEquals("fred", result.getName());
        verifyNoMoreInteractions(schemaLookup);
    }

    @Test
    public void readFrom_shouldNotPerformSchemaValidation_whenOnlyOtherAnnotationsArePresent() throws Exception {
        SchemaLookup schemaLookup = mock(SchemaLookup.class);
        JsonSchemaProvider provider = new JsonSchemaProvider(schemaLookup);
        InputStream inputStream = new ByteArrayInputStream("{\"name\": \"fred\"}".getBytes("UTF-8"));
        Class clazz =  DummyClass.class;
        Annotation[] annotations = DummyClass.class.getMethod("generateUnrelatedAnnotations", String.class).getParameterAnnotations()[0];


        DummyClass result = (DummyClass) provider.readFrom(clazz, DummyClass.class, annotations, MediaType.APPLICATION_JSON_TYPE, new DummyMultiValueMap<String, String>(), inputStream);

        assertEquals("fred", result.getName());
        verify(schemaLookup, never()).getSchemaURL(anyString());
    }

    @Test
    public void readFrom_shouldValidateAgainstTheSchema_whenASchemaAnnotationIsPresent() throws Exception {
        SchemaLookup schemaLookup = mock(SchemaLookup.class);
        when(schemaLookup.getSchemaURL("someSchema")).thenReturn(this.getClass().getResource("/dummy-class-schema.json"));
        JsonSchemaProvider provider = new JsonSchemaProvider(schemaLookup);
        InputStream inputStream = new ByteArrayInputStream("{\"name\": \"fred\"}".getBytes("UTF-8"));
        Annotation[] annotations = DummyClass.class.getMethod("schemaAnnotation", String.class).getParameterAnnotations()[0];
        
        DummyClass result = (DummyClass) provider.readFrom((Class) DummyClass.class, DummyClass.class, annotations, MediaType.APPLICATION_JSON_TYPE, new DummyMultiValueMap<String, String>(), inputStream);
        
        assertEquals("fred", result.getName());
        verify(schemaLookup).getSchemaURL("someSchema");
    }
    
    @Test
    public void readFrom_shouldDelegateToGenerateErrorMessageAndThrowAWebApplicationException_whenTheJsonIsNotValidForTheSchema() throws Exception {
        SchemaLookup schemaLookup = mock(SchemaLookup.class);
        when(schemaLookup.getSchemaURL("someSchema")).thenReturn(this.getClass().getResource("/dummy-class-schema.json"));

        final Response expectedErrorMessage = Response.status(400).entity("Error Message").build();

        JsonSchemaProvider provider = new JsonSchemaProvider(schemaLookup) {
            @Override
            protected Response generateErrorMessage(List<ErrorMessage> validationErrors) {
                return expectedErrorMessage;
            }
        };

        InputStream inputStream = new ByteArrayInputStream("{\"name\": \"fred\", \"location\": \"Delaware\"}".getBytes("UTF-8"));
        Annotation[] annotations = DummyClass.class.getMethod("schemaAnnotation", String.class).getParameterAnnotations()[0];
        
        try {
            provider.readFrom((Class) DummyClass.class, DummyClass.class, annotations, MediaType.APPLICATION_JSON_TYPE, new DummyMultiValueMap<String, String>(), inputStream);
            fail("should have thrown a validation exception");
        } catch(WebApplicationException e) {
            assertSame(expectedErrorMessage, e.getResponse());
        }
    }

    @Test
    public void generateErrorMessage_shouldCreateATextErrorDocument() throws Exception {
        JsonSchemaProvider provider = new JsonSchemaProvider(mock(SchemaLookup.class));

        ErrorMessage errorA = new ErrorMessage("foo.bar", "errorAMessage");
        ErrorMessage errorB = new ErrorMessage("foo.baz", "errorBMessage");
        List<ErrorMessage> validationErrors = Arrays.asList(errorA, errorB);

        Response response = provider.generateErrorMessage(validationErrors);

        assertEquals(400, response.getStatus());
        assertEquals(MediaType.TEXT_PLAIN_TYPE, response.getMetadata().getFirst("Content-Type"));

        String entity = (String) response.getEntity();
        assertTrue(entity.contains(errorA.getLocation() + ": "));
        assertTrue(entity.contains(errorA.getMessage()));

        assertTrue(entity.contains(errorB.getLocation() + ": "));
        assertTrue(entity.contains(errorB.getMessage()));
    }

    @SuppressWarnings("UnusedDeclaration")
    static class DummyClass {
        String name;

        DummyClass() {}

        public String getName() {
            return name;
        }

        public void generateUnrelatedAnnotations(@PathParam("foo") String foo) {}

        public void schemaAnnotation(@Schema("someSchema") String foo) {}
    } 
    
    private static class DummyMultiValueMap<K, V> extends HashMap<K, List<V>> implements MultivaluedMap<K, V> {

        @Override
        public void putSingle(K key, V value) {
        }

        @Override
        public void add(K key, V value) {
        }

        @Override
        public V getFirst(K key) {
            return null;
        }
    } 
}
