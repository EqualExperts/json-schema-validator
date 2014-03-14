package uk.co.o2.json.schema;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import javax.json.*;
import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.Assert.assertThat;
import static uk.co.o2.json.schema.JsonObjectSupport.*;

public class JsonObjectSupportTest {
    @Test
    public void getOptionalString_shouldReturnAnOptionalWithAValue_givenAFieldThatExists() throws Exception {
        String expectedValue = "bar";
        JsonObject json = Json.createObjectBuilder().add("foo", expectedValue).build();

        Optional<String> result = getOptionalString(json, "foo");

        assertThat(result.isPresent(), CoreMatchers.is(true));
        assertThat(result.get(), CoreMatchers.is(expectedValue));
    }

    @Test
    public void getOptionalString_shouldReturnAnOptionalWithAValue_givenAFieldWithAnEmptyStringValue() throws Exception {
        String expectedValue = "";
        JsonObject json = Json.createObjectBuilder().add("foo", expectedValue).build();

        Optional<String> result = getOptionalString(json, "foo");

        assertThat(result.isPresent(), CoreMatchers.is(true));
        assertThat(result.get(), CoreMatchers.is(expectedValue));
    }

    @Test
    public void getOptionalString_shouldReturnAnOptionalWithoutAValue_givenAFieldIsNotPresent() throws Exception {
        JsonObject json = Json.createObjectBuilder().build();

        Optional<String> result = getOptionalString(json, "foo");

        assertThat(result.isPresent(), CoreMatchers.is(false));
    }

    @Test
    public void getOptionalJsonArray_shouldReturnAnOptionalWithAValue_givenAFieldThatExists() throws Exception {
        JsonArray expectedValue = Json.createArrayBuilder().add("bar").add("baz").build();
        JsonObject json = Json.createObjectBuilder().add("foo", expectedValue).build();

        Optional<JsonArray> result = getOptionalJsonArray(json, "foo");

        assertThat(result.isPresent(), CoreMatchers.is(true));
        assertThat(result.get(), CoreMatchers.is(expectedValue));
    }

    @Test
    public void getOptionalJsonArray_shouldReturnAnOptionalWithAValue_givenAFieldWithAnEmptyArrayValue() throws Exception {
        JsonArray expectedValue = Json.createArrayBuilder().build();
        JsonObject json = Json.createObjectBuilder().add("foo", expectedValue).build();

        Optional<JsonArray> result = getOptionalJsonArray(json, "foo");

        assertThat(result.isPresent(), CoreMatchers.is(true));
        assertThat(result.get(), CoreMatchers.is(expectedValue));
    }

    @Test
    public void getOptionalJsonArray_shouldReturnAnOptionalWithoutAValue_givenAFieldIsNotPresent() throws Exception {
        JsonObject json = Json.createObjectBuilder().build();

        Optional<JsonArray> result = getOptionalJsonArray(json, "foo");

        assertThat(result.isPresent(), CoreMatchers.is(false));
    }

    @Test
    public void getOptionalBigDecimal_shouldReturnAnOptionalWithAValue_givenAFieldThatExists() throws Exception {
        BigDecimal expectedValue = new BigDecimal(123.45);
        JsonObject json = Json.createObjectBuilder().add("foo", expectedValue).build();

        Optional<BigDecimal> result = getOptionalBigDecimal(json, "foo");

        assertThat(result.isPresent(), CoreMatchers.is(true));
        assertThat(result.get(), CoreMatchers.is(expectedValue));
    }

    @Test
    public void getOptionalBigDecimal_shouldReturnAnOptionalWithoutAValue_givenAFieldIsNotPresent() throws Exception {
        JsonObject json = Json.createObjectBuilder().build();

        Optional<BigDecimal> result = getOptionalBigDecimal(json, "foo");

        assertThat(result.isPresent(), CoreMatchers.is(false));
    }

    @Test
    public void getOptionalInteger_shouldReturnAnOptionalWithAValue_givenAFieldThatExists() throws Exception {
        int expectedValue = 1337;
        JsonObject json = Json.createObjectBuilder().add("foo", expectedValue).build();

        Optional<Integer> result = getOptionalInteger(json, "foo");

        assertThat(result.isPresent(), CoreMatchers.is(true));
        assertThat(result.get(), CoreMatchers.is(expectedValue));
    }

    @Test
    public void getOptionalInteger_shouldReturnAnOptionalWithoutAValue_givenAFieldIsNotPresent() throws Exception {
        JsonObject json = Json.createObjectBuilder().build();

        Optional<Integer> result = getOptionalInteger(json, "foo");

        assertThat(result.isPresent(), CoreMatchers.is(false));
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void getOptionalBoolean_shouldReturnAnOptionalWithAValue_givenAFieldWithAValueOfTrue() throws Exception {
        boolean expectedValue = true;
        JsonObject json = Json.createObjectBuilder().add("foo", expectedValue).build();

        Optional<Boolean> result = getOptionalBoolean(json, "foo");

        assertThat(result.isPresent(), CoreMatchers.is(true));
        assertThat(result.get(), CoreMatchers.is(expectedValue));
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void getOptionalBoolean_shouldReturnAnOptionalWithAValue_givenAFieldWithAValueOfFalse() throws Exception {
        boolean expectedValue = false;
        JsonObject json = Json.createObjectBuilder().add("foo", expectedValue).build();

        Optional<Boolean> result = getOptionalBoolean(json, "foo");

        assertThat(result.isPresent(), CoreMatchers.is(true));
        assertThat(result.get(), CoreMatchers.is(expectedValue));
    }

    @Test
    public void getOptionalBoolean_shouldReturnAnOptionalWithoutAValue_givenAFieldIsNotPresent() throws Exception {
        JsonObject json = Json.createObjectBuilder().build();

        Optional<Boolean> result = getOptionalBoolean(json, "foo");

        assertThat(result.isPresent(), CoreMatchers.is(false));
    }

    @Test
    public void getOptionalJsonValue_shouldReturnAnOptionalWithAValue_givenAFieldIsPresent() throws Exception {
        JsonObject json = Json.createObjectBuilder().add("foo", "bar").build();

        Optional<JsonValue> result = getOptionalJsonValue(json, "foo");

        assertThat(result.isPresent(), CoreMatchers.is(true));
        assertThat(result.get().getValueType(), CoreMatchers.is(JsonValue.ValueType.STRING));
        assertThat(((JsonString) result.get()).getString(), CoreMatchers.is("bar"));
    }

    @Test
    public void getOptionalJsonObject_shouldReturnAnOptionalWithAValue_givenAFieldThatExists() throws Exception {
        JsonObject expectedValue = Json.createObjectBuilder().add("bar", "baz").build();
        JsonObject json = Json.createObjectBuilder().add("foo", expectedValue).build();

        Optional<JsonObject> result = getOptionalJsonObject(json, "foo");

        assertThat(result.isPresent(), CoreMatchers.is(true));
        assertThat(result.get(), CoreMatchers.is(expectedValue));
    }

    @Test
    public void getOptionalJsonObject_shouldReturnAnOptionalWithAnEmptyValue_givenAFieldWithAnValue() throws Exception {
        JsonObject expectedValue = Json.createObjectBuilder().build();
        JsonObject json = Json.createObjectBuilder().add("foo", expectedValue).build();

        Optional<JsonObject> result = getOptionalJsonObject(json, "foo");

        assertThat(result.isPresent(), CoreMatchers.is(true));
        assertThat(result.get().isEmpty(), CoreMatchers.is(true));
    }

    @Test
    public void getOptionalJsonObject_shouldReturnAnOptionalWithoutAValue_givenAFieldIsNotPresent() throws Exception {
        JsonObject json = Json.createObjectBuilder().build();

        Optional<JsonObject> result = getOptionalJsonObject(json, "foo");

        assertThat(result.isPresent(), CoreMatchers.is(false));
    }
}