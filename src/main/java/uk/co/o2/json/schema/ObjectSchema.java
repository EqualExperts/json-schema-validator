package uk.co.o2.json.schema;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;

import static java.util.Collections.emptyList;
import static uk.co.o2.json.schema.ErrorMessage.singleError;

class ObjectSchema implements JsonSchema {
    public static final JsonSchema ALLOW_ALL_ADDITIONAL_PROPERTIES = new JsonSchema() {
        @Override
        public List<ErrorMessage> validate(JsonNode jsonDocumentToValidate) {
            return emptyList();
        }

        @Override
        public String getDescription() {
            return "";
        }
    };

    public static final JsonSchema FORBID_ANY_ADDITIONAL_PROPERTIES = new JsonSchema() {
        @Override
        public List<ErrorMessage> validate(JsonNode jsonDocumentToValidate) {
            return singleError("", "Unexpected property");
        }

        @Override
        public String getDescription() {
            return "";
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
    public List<ErrorMessage> validate(JsonNode jsonDocumentToValidate) {
        List<ErrorMessage> results = new ArrayList<>();
        if (!jsonDocumentToValidate.isObject()) {
            return singleError("", "Invalid type: must be an object");
        }
        Set<String> visitedPropertyNames = new HashSet<>();

        for (Property property : properties) {
            if (!jsonDocumentToValidate.has(property.getName())) {
                if (property.isRequired()) {
                    results.add(new ErrorMessage(property.getName(), "Missing required property " + property.getName()));
                }
            } else {
                JsonNode propertyValue = jsonDocumentToValidate.get(property.getName());
                for (ErrorMessage nestedMessage : property.getNestedSchema().validate(propertyValue)) {
                    results.add(new ErrorMessage(property.getName(), nestedMessage));
                }
            }
            visitedPropertyNames.add(property.getName());
        }


        for (Iterator<Map.Entry<String, JsonNode>> iterator = jsonDocumentToValidate.fields(); iterator.hasNext();) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            if (!visitedPropertyNames.contains(entry.getKey())) {
                for (ErrorMessage it : additionalProperties.validate(entry.getValue())) {
                    results.add(new ErrorMessage(entry.getKey(), it));
                }
            }
        }

        return results;
    }

    @Override
    public String getDescription() {
        return "object";
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