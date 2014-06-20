package uk.co.o2.json.schema;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;

class UnionSchema implements JsonSchema{
    private List<JsonSchema> nestedSchemas = new ArrayList<>();

    @Override
    public List<ErrorMessage> validate(JsonNode jsonDocument) {
        if (!isAcceptableType(jsonDocument)) {
            Set<String> nestedDescriptions = getNestedDescriptions();
            return ErrorMessage.singleError("", "Invalid type: must be one of: " + nestedDescriptions);
        }

        List<List<ErrorMessage>> nestedErrors = validateAgainstAllNestedSchemas(jsonDocument);
        return findSmallest(nestedErrors);
    }

    @Override
    public String getDescription() {
        return "union";
    }

    @Override
    public boolean isAcceptableType(JsonNode jsonDocument) {
        for (JsonSchema nested : nestedSchemas) {
            if (nested.isAcceptableType(jsonDocument)) {
                return true;
            }
        }
        return false;
    }

    private List<List<ErrorMessage>> validateAgainstAllNestedSchemas(JsonNode jsonDocumentToValidate) {
        List<List<ErrorMessage>> nestedErrors = new ArrayList<>();
        for (JsonSchema nestedSchema : nestedSchemas) {
            nestedErrors.add(nestedSchema.validate(jsonDocumentToValidate));
        }
        return nestedErrors;
    }

    private Set<String> getNestedDescriptions() {
        Set<String> nestedDescriptions = new LinkedHashSet<>();
        for (JsonSchema schema: nestedSchemas) {
            nestedDescriptions.add("\"" + schema.getDescription() + "\"");
        }
        return nestedDescriptions;
    }

    List<JsonSchema> getNestedSchemas() {
        return nestedSchemas;
    }

    //region these could be methods on Collection

    private static <T> List<T> findSmallest(List<List<T>> values) {
        List<T> leastNestedErrors = head(values);
        for (List<T> em : tail(values)) {
            leastNestedErrors = smaller(leastNestedErrors, em);
        }
        return leastNestedErrors;
    }

    private static <T> List<T> tail(List<T> list) {
        return list.subList(1, list.size());
    }

    private static <T> T head(List<T> list) {
        return list.get(0);
    }

    private static <T> List<T> smaller(List<T> a, List<T> b) {
        if (b.size() < a.size()) {
            return b;
        }
        return a;
    }

    //endregion
}
