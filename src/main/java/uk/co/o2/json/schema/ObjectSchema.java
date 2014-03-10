package uk.co.o2.json.schema;

import javax.json.JsonObject;
import javax.json.JsonValue;
import java.util.*;

import static java.util.Collections.emptyList;
import static uk.co.o2.json.schema.ErrorMessage.singleError;

class ObjectSchema implements JsonSchema {
    public static final JsonSchema ALLOW_ALL_ADDITIONAL_PROPERTIES = new JsonSchema() {
        @Override
        public List<ErrorMessage> validate(JsonValue jsonDocumentToValidate) {
            return emptyList();
        }
    };

    public static final JsonSchema FORBID_ANY_ADDITIONAL_PROPERTIES = new JsonSchema() {
        @Override
        public List<ErrorMessage> validate(JsonValue jsonDocumentToValidate) {
            return singleError("", "Unexpected property");
        }
    };

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
        Set<String> visitedPropertyNames = new HashSet<>();

        for (Property property : properties) {
            if (!jsonObject.containsKey(property.getName())) {
                if (property.isRequired()) {
                    results.add(new ErrorMessage(property.getName(), "Missing required property " + property.getName()));
                }
            } else {
                JsonValue propertyValue = jsonObject.get(property.getName());
                for (ErrorMessage nestedMessage : property.getNestedSchema().validate(propertyValue)) {
                    results.add(new ErrorMessage(property.getName(), nestedMessage));
                }
            }
            visitedPropertyNames.add(property.getName());
        }


        for (Map.Entry<String, JsonValue> entry : jsonObject.entrySet()) {
            if (!visitedPropertyNames.contains(entry.getKey())) {
                for (ErrorMessage it : additionalProperties.validate(entry.getValue())) {
                    results.add(new ErrorMessage(entry.getKey(), it));
                }
            }
        }

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