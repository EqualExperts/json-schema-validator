package uk.co.o2.json.schema

import org.codehaus.jackson.JsonNode
import java.util.Map.Entry
import groovy.transform.PackageScope

@PackageScope
class ObjectSchema implements JsonSchema {
    static final JsonSchema ALLOW_ALL_ADDITIONAL_PROPERTIES = [validate: { [] }] as JsonSchema
    static final JsonSchema FORBID_ANY_ADDITIONAL_PROPERTIES = [
        validate: { [new ErrorMessage('', 'Unexpected property')] }
    ] as JsonSchema

    List<ObjectProperty> properties = []
    JsonSchema additionalProperties = ALLOW_ALL_ADDITIONAL_PROPERTIES

    @Override
    List<ErrorMessage> validate(JsonNode jsonDocumentToValidate) {
        List<ErrorMessage> results = []
        if (!jsonDocumentToValidate.object) {
            return [new ErrorMessage('', "Invalid type: must be an object")]
        }
        Set<String> visitedPropertyNames = []
        properties.each {ObjectProperty property ->
            if (!jsonDocumentToValidate.has(property.name)) {
                if (property.required) {
                    results << new ErrorMessage(property.name, "Missing required property ${property.name}")
                }
            } else {
                JsonNode propertyValue = jsonDocumentToValidate.get(property.name)
                results += property.nestedSchema.validate(propertyValue).collect { new ErrorMessage(property.name, it) }
            }
            visitedPropertyNames << property.name
        }

        jsonDocumentToValidate.fields.each {Entry<String, JsonNode> entry ->
            if (!visitedPropertyNames.contains(entry.key)) {
                results += additionalProperties.validate(entry.value).collect { new ErrorMessage(entry.key, it) }
            }
        }
        results
    }


}