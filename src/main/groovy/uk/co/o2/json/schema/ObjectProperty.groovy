package uk.co.o2.json.schema

import groovy.transform.PackageScope

@PackageScope
class ObjectProperty {
    String name
    boolean required
    JsonSchema nestedSchema = new SimpleTypeSchema(type: SimpleType.ANY)
}
