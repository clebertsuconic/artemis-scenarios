package org.apache.activemq.artemis.scenarios.helpers;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import net.datafaker.Faker;

/**
 * Utility class for text related methods
 */
@EqualsAndHashCode()
@Log4j2
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@ToString(callSuper = true)
public class TextHelper {
    /**
     * Generate random text message with specific size
     *
     * @param sizeOfMsgsKb the size in kilobytes to generate
     * @return the generated random message
     */
    public static String generateRandomMsgText(int sizeOfMsgsKb) {
        LOGGER.trace("Generating random message text");
        Faker faker = new Faker();
        String randomText;
        if (sizeOfMsgsKb <= 0) {
            LOGGER.trace("Generating random paragraph text");
            randomText = faker.lorem().paragraph();
        } else {
            LOGGER.trace("Generating random text with size of {} kb", sizeOfMsgsKb);
            randomText = faker.lorem().characters(getCharSizeInKb(sizeOfMsgsKb), true, true);
        }
        return randomText;
    }

    /**
     * Generate a random string with specific length
     *
     * @param length the length of the random string to be generated
     * @return the generated random string
     */
    public static String generateRandomString(int length) {
        LOGGER.trace("Generating random string with length of {}", length);
        Faker faker = new Faker();
        return faker.text().text(length);
    }

    /**
     * Get character size from kilobyte size
     *
     * @param kbSize size in kilobytes
     * @return size in char after conversion
     */
    private static int getCharSizeInKb(int kbSize) {
        LOGGER.trace("Converting Char size in KB");
        // Java chars are 2 bytes
        if (kbSize != 1) {
            kbSize = kbSize / 2;
        }
        int value = kbSize * 1024;
        LOGGER.trace("Char: {} is {} KB", value, kbSize);
        return value;
    }
}
