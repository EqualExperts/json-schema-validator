package uk.co.o2.json.schema;

import javax.json.JsonNumber;
import javax.json.JsonString;
import javax.json.JsonValue;

enum SimpleType {
    STRING {
        @Override
        public String getValue(JsonValue node) {
            return ((JsonString)node).getString();
        }

        @Override
        public boolean matches(JsonValue node) {
            return node.getValueType() == JsonValue.ValueType.STRING;
        }
    },
    NUMBER {
        @Override
        public Number getValue(JsonValue node) {
            return ((JsonNumber)node).bigDecimalValue();
        }

        @Override
        public boolean matches(JsonValue node) {
            return node.getValueType() == JsonValue.ValueType.NUMBER;
        }
    },
    INTEGER {
        @Override
        public Integer getValue(JsonValue node) {
            return ((JsonNumber)node).intValue();
        }

        @Override
        public boolean matches(JsonValue node) {
            if(node.getValueType() != JsonValue.ValueType.NUMBER) {
                return false;
            } else {
                JsonNumber num = (JsonNumber) node;
                return num.isIntegral();
            }
        }
    },
    BOOLEAN {
        @Override
        public Boolean getValue(JsonValue node) {
            return node.getValueType() == JsonValue.ValueType.TRUE;
        }

        @Override
        public boolean matches(JsonValue node) {
            return node.getValueType() == JsonValue.ValueType.FALSE || node.getValueType() == JsonValue.ValueType.TRUE;
        }
    },
    NULL {
        @Override
        public Object getValue(JsonValue node) {
            throw new IllegalStateException("Cannot retrieve the value of a null node");
        }

        @Override
        public boolean matches(JsonValue node) {
            return node.getValueType() == JsonValue.ValueType.NULL;
        }
    },
    ANY {
        @Override
        public Object getValue(JsonValue node) {
            throw new IllegalStateException("Cannot meaningfully retrieve the value of an ANY node, as we don't have enough type information");
        }

        @Override
        public boolean matches(JsonValue node) {
            return true;
        }
    };

    public abstract Object getValue(JsonValue node);

    public abstract boolean matches(JsonValue node);
}