package uk.co.o2.json.schema;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.json.JsonString;
import javax.json.JsonValue;

class SimpleTypeSchema implements JsonSchema {
    private SimpleType type = SimpleType.ANY;
    private Pattern pattern;
    private String format;
    private int maxLength;
    private int minLength;
    private Number minimum;
    private Number maximum;
    private boolean exclusiveMinimum;
    private boolean exclusiveMaximum;
    private List<JsonValue> enumeration;

    @Override
    public List<ErrorMessage> validate(JsonValue node) {
        List<ErrorMessage> results = new ArrayList<>();
        if (!type.matches(node)) {
            results.add(new ErrorMessage("", "Invalid type: must be of type " + type.name().toLowerCase()));
        } else {
            validatePattern(node, results);
            validateFormat(node, results);
            validateRange(node, results);
            validateLength(node, results);
            validateNodeValueIsFromEnumeratedList(node, results);
        }
        return results;
    }

    void setEnumeration(List<JsonValue> enumeration) {
        if (EnumSet.of(SimpleType.NULL, SimpleType.ANY).contains(type)) {
            throw new IllegalArgumentException("enumeration not allowed for Null or Any types");
        }
        validateEnumElementsOfSameType(enumeration);
        this.enumeration = enumeration;
    }

    void setExclusiveMinimum(boolean exclusiveMinimum) {
        validateTypeNumberOrIntegerFor("exclusiveMinimum");
        this.exclusiveMinimum = exclusiveMinimum;
    }

    void setExclusiveMaximum(boolean exclusiveMaximum) {
        validateTypeNumberOrIntegerFor("exclusiveMaximum");
        this.exclusiveMaximum = exclusiveMaximum;
    }

    void setMinimum(Number minimum) {
        validateTypeNumberOrIntegerFor("minimum");
        this.minimum = minimum;
    }

    void setMaximum(Number maximum) {
        validateTypeNumberOrIntegerFor("maximum");
        this.maximum = maximum;
    }

    private void validateEnumElementsOfSameType(List<JsonValue> values) {
        for (JsonValue value : values) {
            if (!type.matches(value)) {
                throw new IllegalArgumentException("values in enum must be of type " + type);
            }
        }
    }

    private void validateTypeNumberOrIntegerFor(String fieldName) {
        if (!EnumSet.of(SimpleType.INTEGER, SimpleType.NUMBER).contains(type)) {
            throw new IllegalArgumentException(fieldName + " can only be used for Integer or Number types");
        }
    }

    void setPattern(Pattern pattern) {
        validatePatternAndType(pattern, type);
        this.pattern = pattern;
    }

    void setType(SimpleType type) {
        validateFormatAndType(format, type);
        validatePatternAndType(pattern, type);
        this.type = type;
    }

    void setFormat(String format) {
        validateFormatAndType(format, type);
        this.format = format;
    }

    void setMaxLength(int maxLength) {
        if (type != SimpleType.STRING) {
            throw new IllegalArgumentException("maxLength can only be used for type: String");
        }
        this.maxLength = maxLength;
    }

    void setMinLength(int minLength) {
        if (type != SimpleType.STRING) {
            throw new IllegalArgumentException("minLength can only be used for type: String");
        }
        this.minLength = minLength;
    }

    private static void validatePatternAndType(Pattern pattern, SimpleType type) {
        if ((type != SimpleType.STRING) && (pattern != null)) {
            throw new IllegalArgumentException("Regex patterns are only legal for type string");
        }
    }

    private static void validateFormatAndType(String format, SimpleType type) {
        FormatValidator formatValidator = formatValidators.get(format);
        if ((formatValidator != null) && (!formatValidator.isCompatibleType(type))) {
            throw new IllegalArgumentException("Format " + format + " is not valid for type " + type.name().toLowerCase());
        }
    }

    private void validateNodeValueIsFromEnumeratedList(JsonValue node, List<ErrorMessage> results) {
        if ((enumeration!= null) && !enumeration.contains(node)) {
            results.add(new ErrorMessage("", "Value " + node.toString() + " must be one of: " + enumeration.toString()));
        }
    }

    private void validateLength(JsonValue node, List<ErrorMessage> results) {
        if (minLength > 0) {
            String value = type.getValue(node).toString();
            if (value.length() < minLength) {
                results.add(new ErrorMessage("", "Value '" + ((JsonString)node).getString() + "' must be greater or equal to " + minLength + " characters"));
            }
        }
        if (maxLength > 0) {
            String value = type.getValue(node).toString();
            if (value.length() > maxLength) {
                results.add(new ErrorMessage("", String.format("Value '%s' must be less or equal to %d characters", ((JsonString)node).getString(), maxLength)));
            }
        }
    }

    private void validateRange(JsonValue node, List<ErrorMessage> results) {
        if (this.minimum != null) {
            String nodeValueAsString = type.getValue(node).toString();
            BigDecimal value = new BigDecimal(nodeValueAsString);
            BigDecimal minimum = new BigDecimal(this.minimum.toString());
            if (exclusiveMinimum && (value.compareTo(minimum) < 1)) {
                results.add(new ErrorMessage("", "Value '" + nodeValueAsString + "' must be greater than " + minimum + " when exclusiveMinimum is true"));
            } else if (value.compareTo(minimum) < 0) {
                results.add(new ErrorMessage("", "Value '" + nodeValueAsString + "' must be greater or equal to " + minimum));
            }
        }

        if (this.maximum != null) {
            String nodeValueAsString = type.getValue(node).toString();
            BigDecimal value = new BigDecimal(nodeValueAsString);
            BigDecimal maximum = new BigDecimal(this.maximum.toString());
            if (exclusiveMaximum && value.compareTo(maximum) >= 0) {
                results.add(new ErrorMessage("", "Value '" + nodeValueAsString + "' must be less than " + maximum + " when exclusiveMaximum is true"));
            } else if (value.compareTo(maximum) > 0) {
                results.add(new ErrorMessage("", "Value '" + nodeValueAsString + "' must be less than or equal to " + maximum));
            }
        }
    }

    private void validateFormat(JsonValue node, List<ErrorMessage> results) {
        if (format != null) {
            FormatValidator formatValidator = formatValidators.get(format);
            if (formatValidator!= null && !formatValidator.isValid(node)) {
                results.add(new ErrorMessage("", "Value '" + ((JsonString)node).getString() + "' is not a valid " + format));
            }
        }
    }

    private void validatePattern(JsonValue node, List<ErrorMessage> results) {
        if (pattern != null) {
            String value = type.getValue(node).toString();
            if (!pattern.matcher(value).matches()) {
                results.add(new ErrorMessage("", "String value '" + value + "' does not match regex '" + pattern.pattern() + "'"));
            }
        }
    }

    SimpleType getType() {
        return type;
    }

    Pattern getPattern() {
        return pattern;
    }

    String getFormat() {
        return format;
    }

    int getMaxLength() {
        return maxLength;
    }

    int getMinLength() {
        return minLength;
    }

    Number getMinimum() {
        return minimum;
    }

    Number getMaximum() {
        return maximum;
    }

    boolean isExclusiveMinimum() {
        return exclusiveMinimum;
    }

    boolean isExclusiveMaximum() {
        return exclusiveMaximum;
    }

    List<JsonValue> getEnumeration() {
        return enumeration;
    }

    private static interface FormatValidator {

        boolean isValid(JsonValue node);
        boolean isCompatibleType(SimpleType type);

    }

    private static Map<String, FormatValidator> formatValidators = Collections.unmodifiableMap(new HashMap<String, FormatValidator>() {{
        put("date-time", new FormatValidator() {
            @Override
            public boolean isValid(JsonValue node) {
                String value = SimpleType.STRING.getValue(node).toString();
                try {
                    OffsetDateTime.parse(value);
                    return true;
                } catch (DateTimeParseException ignore) {
                    return false;
                }
            }

            @Override
            public boolean isCompatibleType(SimpleType type) {
                return type == SimpleType.STRING;
            }
        });
        put("date", new FormatValidator() {
            @Override
            public boolean isValid(JsonValue node) {
                String value = SimpleType.STRING.getValue(node).toString();
                try {
                    LocalDate.parse(value);
                    return true;
                } catch (DateTimeParseException ignore) {
                    return false;
                }
            }

            @Override
            public boolean isCompatibleType(SimpleType type) {
                return type == SimpleType.STRING;
            }
        });
        put("time", new FormatValidator() {
            @Override
            public boolean isValid(JsonValue node) {
                String value = SimpleType.STRING.getValue(node).toString();
                try {
                    LocalTime.parse(value);
                    return true;
                } catch (DateTimeParseException ignore) {
                    return false;
                }
            }

            @Override
            public boolean isCompatibleType(SimpleType type) {
                return type == SimpleType.STRING;
            }
        });
        put("utc-millisec", new FormatValidator() {
            @Override
            public boolean isValid(JsonValue node) {
                return true;
            }

            @Override
            public boolean isCompatibleType(SimpleType type) {
                return EnumSet.of(SimpleType.INTEGER, SimpleType.NUMBER).contains(type);
            }

        });
        put("regex", new FormatValidator() {
            @Override
            public boolean isValid(JsonValue node) {
                String value = SimpleType.STRING.getValue(node).toString();
                try {
                    //noinspection ResultOfMethodCallIgnored
                    Pattern.compile(value);
                    return true;
                } catch (PatternSyntaxException e) {
                    return false;
                }
            }

            @Override
            public boolean isCompatibleType(SimpleType type) {
                return type == SimpleType.STRING;
            }
        });
        put("uri", new FormatValidator() {
            @Override
            public boolean isValid(JsonValue node) {
                String value = SimpleType.STRING.getValue(node).toString();
                try {
                    new URI(value);
                    return true;
                } catch (URISyntaxException e) {
                    return false;
                }
            }

            @Override
            public boolean isCompatibleType(SimpleType type) {
                return type == SimpleType.STRING;
            }
        });
    }});
}