package uk.co.o2.json.schema;

import org.junit.Test;

import static org.junit.Assert.*;

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

    @SuppressWarnings("EqualsBetweenInconvertibleTypes")
    @Test
    public void equals_shouldReturnFalse_givenAnObjectThatIsNotAnErrorMessage() throws Exception {
        ErrorMessage em = new ErrorMessage("", "blah");

        boolean result = em.equals("blah");

        assertFalse(result);
    }

    @Test
    public void equals_shouldReturnFalse_givenAnErrorMessageWithADifferentLocation() throws Exception {
        String aCommonErrorMessage = "blah";
        ErrorMessage em = new ErrorMessage("theRightLocation", aCommonErrorMessage);
        ErrorMessage other = new ErrorMessage("theWrongLocation", aCommonErrorMessage);

        boolean result = em.equals(other);

        assertFalse(result);
    }

    @Test
    public void equals_shouldReturnFalse_givenAnErrorMessageWithADifferentMessage() throws Exception {
        String aCommonLocation = "aCommonLocation";
        ErrorMessage em = new ErrorMessage(aCommonLocation, "the right error message");
        ErrorMessage other = new ErrorMessage(aCommonLocation, "a different error message");

        boolean result = em.equals(other);

        assertFalse(result);
    }

    @Test
    public void equals_shouldReturnTrue_givenAnErrorMessageWithTheSameLocationAndMessage() throws Exception {
        String aCommonLocation = "aCommonLocation";
        String aCommonMessage = "aCommonMessage";
        ErrorMessage em = new ErrorMessage(aCommonLocation, aCommonMessage);
        ErrorMessage other = new ErrorMessage(aCommonLocation, aCommonMessage);

        boolean result = em.equals(other);

        assertTrue(result);
    }

    @Test
    public void hashCode_shouldUsuallyReturnADifferentValue_givenAnErrorMessageWithADifferentLocation() throws Exception {
        String aCommonErrorMessage = "blah";
        ErrorMessage one = new ErrorMessage("theRightLocation", aCommonErrorMessage);
        ErrorMessage theOther = new ErrorMessage("theWrongLocation", aCommonErrorMessage);

        int oneHashCode = one.hashCode();
        int theOtherHashCode = theOther.hashCode();

        assertNotEquals(oneHashCode, theOtherHashCode);
    }

    @Test
    public void hashCode_shouldUsuallyReturnADifferentValue_givenAnErrorMessageWithADifferentMessage() throws Exception {
        String aCommonLocation = "aCommonLocation";
        ErrorMessage one = new ErrorMessage(aCommonLocation, "the right error message");
        ErrorMessage theOther = new ErrorMessage(aCommonLocation, "a different error message");

        int oneHashCode = one.hashCode();
        int theOtherHashCode = theOther.hashCode();

        assertNotEquals(oneHashCode, theOtherHashCode);
    }

    @Test
    public void hashCode_shouldAlwaysReturnTheSameValue_givenAnErrorMessageWithTheSameLocationAndMessage() throws Exception {
        String aCommonLocation = "aCommonLocation";
        String aCommonMessage = "aCommonMessage";
        ErrorMessage one = new ErrorMessage(aCommonLocation, aCommonMessage);
        ErrorMessage theOther = new ErrorMessage(aCommonLocation, aCommonMessage);

        int oneHashCode = one.hashCode();
        int theOtherHashCode = theOther.hashCode();

        assertEquals(oneHashCode, theOtherHashCode);
    }
}