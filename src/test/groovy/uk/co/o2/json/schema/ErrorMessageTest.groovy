package uk.co.o2.json.schema

import org.junit.Test


class ErrorMessageTest {
    @Test
    void constructor_shouldPrependAParentLocationToProvidedErrorMessageAndPreserveMessage_givenAPrefixAndAnErrorMessage() {
        String expectedMessage = 'expected error message'
        String originalLocation = 'someLocation'
        ErrorMessage originalMessage = new ErrorMessage(originalLocation, expectedMessage)

        ErrorMessage derivedMessage = new ErrorMessage('prefix', originalMessage)

        assert derivedMessage.location == 'prefix.someLocation'
        assert derivedMessage.message == expectedMessage
        assert originalMessage.location == originalLocation
        assert originalMessage.message == expectedMessage
    }

    @Test
    void constructor_shouldNotAddATrailingDotInTheParentLocation_givenAChildErrorMessageWithAnEmptyLocation() {
        ErrorMessage originalMessage = new ErrorMessage('', 'some message')

        ErrorMessage derivedMessage = new ErrorMessage('prefix', originalMessage)

        assert derivedMessage.location == 'prefix'
    }

    @Test
    void constructor_shouldProperlyAppendAPropertyName_givenAChildErrorMessageThatStartsWithAnArrayIndex() {
        ErrorMessage originalMessage = new ErrorMessage('[7]', 'some message')

        ErrorMessage derivedMessage = new ErrorMessage('prefix', originalMessage)

        assert derivedMessage.location == 'prefix[7]'
    }
}