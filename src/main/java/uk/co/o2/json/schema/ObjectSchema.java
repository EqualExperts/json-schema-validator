package uk.co.o2.json.schema;

import javax.json.JsonObject;
import javax.json.JsonValue;
import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toSet;
import static uk.co.o2.json.schema.ErrorMessage.singleError;

class ObjectSchema implements JsonSchema {
    public static final JsonSchema ALLOW_ALL_ADDITIONAL_PROPERTIES = (it) -> emptyList();

    public static final JsonSchema FORBID_ANY_ADDITIONAL_PROPERTIES = (it) -> singleError("", "Unexpected property");

    private List<Property> properties = new ArrayList<>();
    
    List<Property> getProperties() {
        return properties;
    }

    private JsonSchema additionalProperties = ALLOW_ALL_ADDITIONAL_PROPERTIES;

    void setAdditionalProperties(JsonSchema additionalProperties) {
        this.additionalProperties = additionalProperties;
    }

    JsonSchema getAdditionalProperties() {
        return additionalProperties;
    }

    @Override
    public List<ErrorMessage> validate(JsonValue jsonDocumentToValidate) {
        List<ErrorMessage> results = new ArrayList<>();
        if (jsonDocumentToValidate.getValueType() != JsonValue.ValueType.OBJECT) {
            return singleError("", "Invalid type: must be an object");
        }
        JsonObject jsonObject = (JsonObject) jsonDocumentToValidate;

        properties.stream()
                .filter((property) -> !jsonObject.containsKey(property.getName()))
                .filter(Property::isRequired)
                .map((property) -> new ErrorMessage(property.getName(), "Missing required property " + property.getName()))
                .forEach(results::add);

        properties.stream()
                .filter((property) -> jsonObject.containsKey(property.getName()))
                .flatMap((property) -> property.getNestedSchema().validate(jsonObject.get(property.getName())).stream().map((it) -> new ErrorMessage(property.getName(), it)))
                .forEach(results::add);

        Set<String> visitedPropertyNames = properties.stream().map(Property::getName).collect(toSet());

        jsonObject.entrySet().stream()
                .filter((e) -> !visitedPropertyNames.contains(e.getKey()))
                .flatMap((e) -> additionalProperties.validate(e.getValue()).stream().map((it) -> new ErrorMessage(e.getKey(), it)))
                .forEach(results::add);

        return results;
    }

    static class Property {
        private String name;
        private boolean required;
        private JsonSchema nestedSchema;

        Property() {
            SimpleTypeSchema any = new SimpleTypeSchema();
            any.setType(SimpleType.ANY);
            nestedSchema = any;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isRequired() {
            return required;
        }

        public void setRequired(boolean required) {
            this.required = required;
        }

        public JsonSchema getNestedSchema() {
            return nestedSchema;
        }

        public void setNestedSchema(JsonSchema nestedSchema) {
            this.nestedSchema = nestedSchema;
        }
    }
}