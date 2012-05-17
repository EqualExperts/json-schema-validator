package uk.co.o2.json.schema

import org.codehaus.jackson.JsonNode
import groovy.transform.PackageScope

@PackageScope
class ArraySchema implements JsonSchema {

    JsonSchema items = new SimpleTypeSchema(type: SimpleType.ANY)
    int maxItems
    int minItems
    boolean uniqueItems = false

    @Override
    List<ErrorMessage> validate(JsonNode jsonDocument) {
        List<ErrorMessage> results = []
        if (!jsonDocument.array) {
            return [new ErrorMessage('', "Invalid type: must be an array")]
        }
        if (maxItems && jsonDocument.elements.size() > maxItems) {
            results << new ErrorMessage('', "Current array size of ${jsonDocument.elements.size()} is greater than allowed maximum array size of ${maxItems}")
        }

        if (minItems && jsonDocument.elements.size() < minItems) {
            results << new ErrorMessage('', "Current array size of ${jsonDocument.elements.size()} is less than allowed minimum array size of ${minItems}")
        }

        if (uniqueItems) {
            Set<JsonNode> previousNodes = [].toSet()
            jsonDocument.eachWithIndex {JsonNode item, int index ->
                if (previousNodes.contains(item)) {
                    results << new ErrorMessage("[$index]", "Duplicate array element")
                } else {
                    previousNodes << item
                }
            }
        }

        jsonDocument.eachWithIndex {JsonNode item, int index ->
            results += items.validate(item).collect { new ErrorMessage("[$index]", it) }
        }
        results
    }
}
