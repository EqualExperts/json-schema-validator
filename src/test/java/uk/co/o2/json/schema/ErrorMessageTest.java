package uk.co.o2.json.schema;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ErrorMessageTest {
    @Test
    public void constructor_shouldPrependAParentLocationToProvidedErrorMessageAndPreserveMessage_givenAPrefixAndAnErrorMessage() throws Exception {
        String expectedMessage = "expected error message";
        String originalLocation = "someLocation";
        ErrorMessage originalMessage = new ErrorMessage(originalLocation, expectedMessage);

        ErrorMessage derivedMessage = new ErrorMessage("prefix", originalMessage);

        assertEquals("prefix.someLocation", derivedMessage.getLocation());
        assertEquals(expectedMessage, derivedMessage.getMessage());
        assertEquals(originalLocation, originalMessage.getLocation());
        assertEquals(expectedMessage, originalMessage.getMessage());
    }

    @Test
    public void constructor_shouldNotAddATrailingDotInTheParentLocation_givenAChildErrorMessageWithAnEmptyLocation() throws Exception {
        ErrorMessage originalMessage = new ErrorMessage("", "some message");

        ErrorMessage derivedMessage = new ErrorMessage("prefix", originalMessage);

        assertEquals("prefix", derivedMessage.getLocation());
    }

    @Test
    public void constructor_shouldProperlyAppendAPropertyName_givenAChildErrorMessageThatStartsWithAnArrayIndex() throws Exception {
        ErrorMessage originalMessage = new ErrorMessage("[7]", "some message");

        ErrorMessage derivedMessage = new ErrorMessage("prefix", originalMessage);

        assertEquals("prefix[7]", derivedMessage.getLocation());
    }

    @Test
    public void toString_shouldReturnAStringThatContainsTheErrorLocationAndErrorMessage() throws Exception {
        String expectedLocation = "location";
        String expectedMessage = "some message";
        ErrorMessage originalMessage = new ErrorMessage(expectedLocation, expectedMessage);

        String result = originalMessage.toString();

        assertTrue(result.startsWith(expectedLocation + ":"));
        assertTrue(result.contains(expectedMessage));
    }
}