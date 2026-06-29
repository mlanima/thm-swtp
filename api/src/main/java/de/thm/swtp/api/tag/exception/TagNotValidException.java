package de.thm.swtp.api.tag.exception;

import de.thm.swtp.api.common.LogSafe;

public class TagNotValidException extends RuntimeException {

    public TagNotValidException(final String tagName) {
        super("Tag '" + LogSafe.clean(tagName) + "' is not a valid tag");
    }
}
