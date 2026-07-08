package de.thm.swtp.api.moderation.exception;

import de.thm.swtp.api.common.LogSafe;

public class ContentNotValidException extends RuntimeException {

    public ContentNotValidException(final String fieldName) {
        super("Content in field '" + LogSafe.clean(fieldName) + "' is not appropriate");
    }
}
