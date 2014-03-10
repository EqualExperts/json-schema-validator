package uk.co.o2.json.schema;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import uk.co.o2.json.schema.SchemaPassThroughCache.SchemaCompilerFactory;

import javax.json.Json;
import javax.json.JsonReaderFactory;
import java.net.URL;
import java.util.concurrent.ConcurrentMap;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SchemaPassThroughCacheTest {
    private JsonReaderFactory factory = Json.createReaderFactory(null);
    private SchemaPassThroughCache cache = new SchemaPassThroughCache(factory);

    @Test
    public void registeredSchemas_mustBeStoredInAThreadSafeMap() throws Exception {
        assertTrue(ConcurrentMap.class.isAssignableFrom(cache.registeredSchemas.getClass()));
        assertTrue(cache.registeredSchemas.getClass().getName().startsWith("java.util.concurrent."));
    }

    @Test
    public void getSchema_retrievesRegisteredSchema() throws Exception {
        ArraySchema expectedSchema = new ArraySchema();
        URL expectedLocation = new URL("http://example.com/mySchema");
        cache.registerSchema(expectedLocation, expectedSchema);

        assertSame(expectedSchema, cache.getSchema(expectedLocation));
    }

    @Test
    public void getSchema_shouldCompileSchema_givenNoCorrespondingRegisteredSchema() throws Exception {
        final ArraySchema expectedSchema = new ArraySchema();
        final URL expectedLocation = new URL("http://example.com/mySchema");

        SchemaCompiler schemaCompilerMock = mock(SchemaCompiler.class);
        when(schemaCompilerMock.parse(same(expectedLocation))).thenAnswer(new Answer<JsonSchema>() {
            @Override
            public JsonSchema answer(InvocationOnMock invocation) throws Throwable {
                cache.registerSchema((URL) invocation.getArguments()[0], expectedSchema);
                return expectedSchema;
            }
        });

        SchemaCompilerFactory schemaCompilerFactoryMock = mock(SchemaCompilerFactory.class);
        when(schemaCompilerFactoryMock.create()).thenReturn(schemaCompilerMock);
        cache.setSchemaCompilerFactory(schemaCompilerFactoryMock);

        JsonSchema result = cache.getSchema(expectedLocation);

        assertSame(expectedSchema, result);
    }
}