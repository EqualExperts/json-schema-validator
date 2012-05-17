package uk.co.o2.json.schema

import java.util.concurrent.ConcurrentMap
import org.codehaus.jackson.JsonFactory
import org.junit.Test
import org.gmock.WithGMock

@WithGMock
class SchemaPassThroughCacheTest {
    JsonFactory factory = new JsonFactory()
    SchemaPassThroughCache cache = new SchemaPassThroughCache(factory)

    @Test
    void registeredSchemas_mustBeStoredInAThreadSafeMap() {
        assert ConcurrentMap.isAssignableFrom(cache.registeredSchemas.getClass())
        assert cache.registeredSchemas.getClass().name.startsWith('java.util.concurrent.')
    }

    @Test
    void getSchema_retrievesRegisteredSchema() {
        ArraySchema expectedSchema = new ArraySchema()
        URL expectedLocation = "http://example.com/mySchema".toURL()
        cache.registerSchema(expectedLocation, expectedSchema)

        assert cache.getSchema(expectedLocation) == expectedSchema
    }

    @Test
    void getSchema_shouldCompileSchema_givenNoCorrespondingRegisteredSchema() {
        ArraySchema expectedSchema = new ArraySchema()
        URL expectedLocation = "http://example.com/mySchema".toURL()
        def schemaCompilerMock = mock(SchemaCompiler, constructor(cache, factory))
        schemaCompilerMock.parse(match {
            cache.registerSchema(it, expectedSchema)
            it == expectedLocation
        }).returns(expectedSchema)

        play {
            assert cache.getSchema(expectedLocation) == expectedSchema
        }
    }


}