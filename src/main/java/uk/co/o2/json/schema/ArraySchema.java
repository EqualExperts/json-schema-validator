package uk.co.o2.json.schema;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

import static uk.co.o2.json.schema.ErrorMessage.singleError;

class ArraySchema implements JsonSchema {

    private JsonSchema items;
    private int maxItems;
    private int minItems;

    ArraySchema() {
        SimpleTypeSchema items = new SimpleTypeSchema();
        items.setType( SimpleType.ANY);
        setItems(items);
    }

    @Override
    public List<ErrorMessage> validate(JsonNode jsonDocument) {
        List<ErrorMessage> results = new ArrayList<ErrorMessage>();
        if (!jsonDocument.isArray()) {
            return singleError("", "Invalid type: must be an array");
        }
        if ((maxItems != 0) && (jsonDocument.size() > maxItems)) {
            return singleError("", "Current array size of %d is greater than allowed maximum array size of %d", jsonDocument.size(), maxItems);
        }

        if ((minItems != 0) && (jsonDocument.size() < minItems)) {
            return singleError("", "Current array size of %d is less than allowed minimum array size of %d", jsonDocument.size(), minItems);
        }

        int index = 0;
        for(JsonNode item : jsonDocument) {
            results.addAll(generateNestedErrorMessages(index++, items.validate(item)));
        }
        return results;
    }

    private List<ErrorMessage> generateNestedErrorMessages(int index, List<ErrorMessage> errorMessages) {
        List<ErrorMessage> nestedResults = new ArrayList<ErrorMessage>();
        String pathPrefix = "[" + index + "]";
        for(ErrorMessage error: errorMessages) {
            nestedResults.add(new ErrorMessage(pathPrefix, error));
        }
        return nestedResults;
    }

    void setItems(JsonSchema items) {
        this.items = items;
    }

    JsonSchema getItems() {
        return items;
    }

    void setMaxItems(int maxItems) {
        this.maxItems = maxItems;
    }

    int getMaxItems() {
        return maxItems;
    }

    void setMinItems(int minItems) {
        this.minItems = minItems;
    }

    int getMinItems() {
        return minItems;
    }
}
