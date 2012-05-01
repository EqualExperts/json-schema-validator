package uk.co.o2.json.schema.provider;

import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
public class JsonSchemaProviderTest {
    @Test
    public void readFrom_shouldNotPerformSchemaValidationWhenNoSchemaAnnotationPresent() throws Exception {
        SchemaLookup schemaLookup = mock(SchemaLookup.class);
        JsonSchemaProvider provider = new JsonSchemaProvider(schemaLookup);
        InputStream inputStream = new ByteArrayInputStream("{\"name\": \"fred\"}".getBytes("UTF-8"));
        Class clazz =  DummyClass.class;
        
        DummyClass result = (DummyClass) provider.readFrom(clazz, DummyClass.class, new Annotation[0], MediaType.APPLICATION_JSON_TYPE, new DummyMultiValueMap<String, String>(), inputStream);
        
        assertEquals("fred", result.getName());
        verify(schemaLookup, never()).getSchemaURL(anyString());
    }
    
    @Test
    public void readFrom_shouldValidateAgainstTheSchemaWhenSchemaAnnotationIsPresent() throws Exception {
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
    public void readFrom_shouldThrowAValidationExceptionWhenTheJsonIsNotValidForTheSchema() throws Exception {
        SchemaLookup schemaLookup = mock(SchemaLookup.class);
        when(schemaLookup.getSchemaURL("someSchema")).thenReturn(this.getClass().getResource("/dummy-class-schema.json"));
        JsonSchemaProvider provider = new JsonSchemaProvider(schemaLookup);
        InputStream inputStream = new ByteArrayInputStream("{\"name\": \"fred\", \"location\": \"Delaware\"}".getBytes("UTF-8"));
        Annotation[] annotations = DummyClass.class.getMethod("schemaAnnotation", String.class).getParameterAnnotations()[0];
        
        try {
            provider.readFrom((Class) DummyClass.class, DummyClass.class, annotations, MediaType.APPLICATION_JSON_TYPE, new DummyMultiValueMap<String, String>(), inputStream);
            fail("should have thrown a validation exception");
        } catch(ValidationException e) {
            assertTrue(e.getErrors().containsKey("location"));
        }
        
    }
    
    static class DummyClass {
        String name;

        DummyClass() {}

        public String getName() {
            return name;
        }

        @SuppressWarnings("UnusedDeclaration")
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
