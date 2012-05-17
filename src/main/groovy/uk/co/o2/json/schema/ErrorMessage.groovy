package uk.co.o2.json.schema

final class ErrorMessage {
    final String location
    final String message

    ErrorMessage(String location, String message) {
        this.location = location
        this.message = message
    }

    ErrorMessage(String locationPrefix, ErrorMessage nestedMessage) {
        String separator = nestedMessage.separatorNecessary ? '.' : ''
        this.location = locationPrefix + separator + nestedMessage.location
        this.message = nestedMessage.message
    }

    private boolean isSeparatorNecessary() {
        return (location && !location.startsWith('['))
    }

    @Override
    String toString() {
        "(${location}): ${message}"
    }
}