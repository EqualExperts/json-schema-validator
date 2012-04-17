package uk.co.o2.json.schema;

import org.codehaus.jackson.JsonNode;

enum SimpleType {
    STRING {
        @Override
        public String getValue(JsonNode node) {
            return node.getTextValue();
        }

        @Override
        public boolean matches(JsonNode node) {
            return node.isTextual();
        }
    },
    NUMBER {
        @Override
        public Number getValue(JsonNode node) {
            return node.getNumberValue();
        }

        @Override
        public boolean matches(JsonNode node) {
            return node.isNumber();
        }
    },
    INTEGER {
        @Override
        public Integer getValue(JsonNode node) {
            return node.getIntValue();
        }

        @Override
        public boolean matches(JsonNode node) {
            return node.isIntegralNumber();
        }
    },
    BOOLEAN {
        @Override
        public Boolean getValue(JsonNode node) {
            return node.getBooleanValue();
        }

        @Override
        public boolean matches(JsonNode node) {
            return node.isBoolean();
        }
    },
    NULL {
        @Override
        public Object getValue(JsonNode node) {
            throw new IllegalStateException("Cannot retrieve the value of a null node");
        }

        @Override
        public boolean matches(JsonNode node) {
            return node.isNull();
        }
    },
    ANY {
        @Override
        public Object getValue(JsonNode node) {
            throw new IllegalStateException("Cannot meaningfully retrieve the value of an ANY node, as we don't have enough type information");
        }

        @Override
        public boolean matches(JsonNode node) {
            return true;
        }
    };

    public abstract Object getValue(JsonNode node);

    public abstract boolean matches(JsonNode node);
}