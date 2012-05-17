package uk.co.o2.json.schema

import groovy.transform.PackageScope
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import javax.xml.bind.DatatypeConverter
import org.codehaus.jackson.JsonNode

@PackageScope
class SimpleTypeSchema implements JsonSchema {
    SimpleType type = SimpleType.ANY
    Pattern pattern
    String format
    Integer maxLength
    int minLength
    Number minimum
    Number maximum
    boolean exclusiveMinimum
    boolean exclusiveMaximum
    List<JsonNode> enumeration

    @Override
    List<ErrorMessage> validate(JsonNode node) {
        List<ErrorMessage> results = []
        if (!type.matches(node)) {
            results << new ErrorMessage("", "Invalid type: must be of type " + type.name().toLowerCase())
        } else {
            validatePattern(node, results)
            validateFormat(node, results)
            validateRange(node, results)
            validateLength(node, results)
            validateNodeValueIsFromEnumeratedList(node, results)
        }
        results
    }

    void setEnumeration(List<JsonNode> enumeration) {
        if ([SimpleType.NULL, SimpleType.ANY].contains(type)) {
            throw new IllegalArgumentException("enumeration not allowed for Null or Any types")
        }
        validateEnumElementsOfSameType(enumeration)
        this.enumeration = enumeration
    }

    void setExclusiveMinimum(boolean exclusiveMinimum) {
        validateTypeNumberOrIntegerFor("exclusiveMinimum", type)
        this.exclusiveMinimum = exclusiveMinimum
    }

    void setExclusiveMaximum(boolean exclusiveMaximum) {
        validateTypeNumberOrIntegerFor("exclusiveMaximum", type)
        this.exclusiveMaximum = exclusiveMaximum
    }

    void setMinimum(Number minimum) {
        validateTypeNumberOrIntegerFor("minimum", type)
        this.minimum = minimum
    }

    void setMaximum(Number maximum) {
        validateTypeNumberOrIntegerFor("maximum", type)
        this.maximum = maximum
    }

    private def validateEnumElementsOfSameType(List<JsonNode> values) {
        values.each {
            if (!type.matches(it)) {
                throw new IllegalArgumentException("values in enum must be of type ${type}")
            }
        }
    }

    private static void validateTypeNumberOrIntegerFor(String fieldName, SimpleType type) {
        /*
            Code coverage reports that lines calling this method are skipped if it is an instance method with a simpler signature.
            We don't know why.
         */
        if (![SimpleType.INTEGER, SimpleType.NUMBER].contains(type)) {
            throw new IllegalArgumentException("${fieldName} can only be used for Integer or Number types")
        }
    }

    void setPattern(Pattern pattern) {
        validatePatternAndType(pattern, type)
        this.pattern = pattern
    }

    void setType(SimpleType type) {
        validateFormatAndType(format, type)
        validatePatternAndType(pattern, type)
        this.type = type
    }

    void setFormat(String format) {
        validateFormatAndType(format, type)
        this.format = format
    }

    void setMaxLength(int maxLength) {
        if (type != SimpleType.STRING) {
            throw new IllegalArgumentException("maxLength can only be used for type: String")
        }
        this.maxLength = maxLength;
    }

    void setMinLength(int minLength) {
        if (type != SimpleType.STRING) {
            throw new IllegalArgumentException("minLength can only be used for type: String")
        }
        this.minLength = minLength;
    }

    private static void validatePatternAndType(Pattern pattern, SimpleType type) {
        if (type != SimpleType.STRING && pattern) {
            throw new IllegalArgumentException("Regex patterns are only legal for type string")
        }
    }

    private static void validateFormatAndType(String format, SimpleType type) {
        FormatValidator formatValidator = formatValidators[format]
        if (formatValidator && (!formatValidator.isCompatibleType(type))) {
            throw new IllegalArgumentException("Format ${format} is not valid for type ${type.name().toLowerCase()}")
        }
    }

    private def validateNodeValueIsFromEnumeratedList(JsonNode node, List<ErrorMessage> results) {
        if (enumeration && !enumeration.contains(node)) {
            results << new ErrorMessage("", "Value ${node.toString()} must be one of: ${enumeration}")
        }
    }

    private def validateLength(JsonNode node, List<ErrorMessage> results) {
        if (minLength) {
            String value = type.getValue(node)
            if (value.length() < minLength) {
                results << new ErrorMessage("", "Value '${node.textValue}' must be greater or equal to ${minLength} characters")
            }
        }
        if (maxLength != null) {
            String value = type.getValue(node)
            if (value.length() > maxLength) {
                results << new ErrorMessage("", "Value '${node.textValue}' must be less or equal to ${maxLength} characters")
            }
        }
    }

    private def validateRange(JsonNode node, List<ErrorMessage> results) {
        if (minimum != null) {
            Number value = type.getValue(node) as Number
            if (exclusiveMinimum && value <= minimum) {
                results << new ErrorMessage("", "Value '${node}' must be greater than ${minimum} when exclusiveMinimum is true")
            } else if (value < minimum) {
                results << new ErrorMessage("", "Value '${node}' must be greater or equal to ${minimum}")
            }
        }

        if (maximum != null) {
            Number value = type.getValue(node) as Number
            if (exclusiveMaximum && value >= maximum) {
                results << new ErrorMessage("", "Value '${node}' must be less than ${maximum} when exclusiveMaximum is true")
            } else if (value > maximum) {
                results << new ErrorMessage("", "Value '${node}' must be less than or equal to ${maximum}")
            }
        }
    }

    private def validateFormat(JsonNode node, List<ErrorMessage> results) {
        if (format) {
            FormatValidator formatValidator = formatValidators[format]
            if (formatValidator && !formatValidator.isValid(node)) {
                results << new ErrorMessage("", "Value '${node.textValue}' is not a valid ${format}")
            }
        }
    }

    private def validatePattern(JsonNode node, List<ErrorMessage> results) {
        if (pattern) {
            String value = type.getValue(node)
            if (!pattern.matcher(value).matches()) {
                results << new ErrorMessage("", "String value '${value}' does not match regex '${pattern.pattern()}'")
            }
        }
    }

    private static interface FormatValidator {
        boolean isValid(JsonNode node)

        boolean isCompatibleType(SimpleType type)
    }

    private static Map<String, FormatValidator> formatValidators = [
            'date-time': new FormatValidator() {
                @Override
                boolean isValid(JsonNode node) {
                    String value = SimpleType.STRING.getValue(node)
                    try {
                        DatatypeConverter.parseDateTime(value)
                        true
                    } catch (IllegalArgumentException e) {
                        false
                    }
                }

                @Override
                boolean isCompatibleType(SimpleType type) {
                    type == SimpleType.STRING
                }
            },
            date: new FormatValidator() {
                @Override
                boolean isValid(JsonNode node) {
                    String value = SimpleType.STRING.getValue(node)
                    SimpleDateFormat format = new SimpleDateFormat('yyyy-MM-dd')
                    ParsePosition position = new ParsePosition(0)

                    Date result = format.parse(value, position)

                    result && (position.index == value.length())
                }

                @Override
                boolean isCompatibleType(SimpleType type) {
                    type == SimpleType.STRING
                }
            },
            time: new FormatValidator() {
                @Override
                boolean isValid(JsonNode node) {
                    String value = SimpleType.STRING.getValue(node)
                    SimpleDateFormat format = new SimpleDateFormat('HH:mm:ss')
                    ParsePosition position = new ParsePosition(0)

                    Date result = format.parse(value, position)

                    result && (position.index == value.length())
                }

                @Override
                boolean isCompatibleType(SimpleType type) {
                    type == SimpleType.STRING
                }
            },
            'utc-millisec': new FormatValidator() {
                @Override
                boolean isValid(JsonNode node) {
                    true
                }

                @Override
                boolean isCompatibleType(SimpleType type) {
                    [SimpleType.INTEGER, SimpleType.NUMBER].contains(type)
                }

            },
            'regex': new FormatValidator() {
                @Override
                boolean isValid(JsonNode node) {
                    String value = SimpleType.STRING.getValue(node)
                    try {
                        Pattern.compile(value)
                        true
                    } catch (PatternSyntaxException e) {
                        false
                    }
                }

                @Override
                boolean isCompatibleType(SimpleType type) {
                    type == SimpleType.STRING
                }
            },
            uri: new FormatValidator() {
                @SuppressWarnings("GroovyResultOfObjectAllocationIgnored")
                @Override
                boolean isValid(JsonNode node) {
                    String value = SimpleType.STRING.getValue(node)
                    try {
                        new URI(value)
                        true
                    } catch (URISyntaxException e) {
                        false
                    }
                }

                @Override
                boolean isCompatibleType(SimpleType type) {
                    type == SimpleType.STRING
                }
            }
    ]
}