package uk.co.o2.json.schema;

import javax.json.*;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.function.Function;

class JsonObjectSupport {
    static Optional<String> getOptionalString(JsonObject json, String fieldName) {
        return getOptionalJsonField(json::getJsonString, fieldName).map(JsonString::getString);
    }

    static Optional<JsonArray> getOptionalJsonArray(JsonObject json, String fieldName) {
        return getOptionalJsonField(json::getJsonArray, fieldName);
    }

    static Optional<BigDecimal> getOptionalBigDecimal(JsonObject json, String fieldName) {
        return getOptionalJsonNumber(json, fieldName).map(JsonNumber::bigDecimalValue);
    }

    static Optional<Integer> getOptionalInteger(JsonObject json, String fieldName) {
        return getOptionalJsonNumber(json, fieldName).map(JsonNumber::intValue);
    }

    static Optional<JsonObject> getOptionalJsonObject(JsonObject json, String fieldName) {
        return getOptionalJsonField(json::getJsonObject, fieldName);
    }

    private static Optional<JsonNumber> getOptionalJsonNumber(JsonObject json, String fieldName) {
        return getOptionalJsonField(json::getJsonNumber, fieldName);
    }

    static Optional<Boolean> getOptionalBoolean(JsonObject json, String fieldName) {
        if (!json.containsKey(fieldName)) {
            return Optional.empty();
        }
        return Optional.of(json.getBoolean(fieldName));
    }

    static Optional<JsonValue> getOptionalJsonValue(JsonObject schema, String fieldName) {
        Function<String, JsonValue> get = schema::get;
        return getOptionalJsonField(get, fieldName);
    }

    private static <T> Optional<T> getOptionalJsonField(Function<String, T> method, String fieldName) {
        return Optional.ofNullable(method.apply(fieldName));
    }
}
