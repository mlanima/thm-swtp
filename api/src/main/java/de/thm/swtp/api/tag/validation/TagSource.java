package de.thm.swtp.api.tag.validation;

@FunctionalInterface
public interface TagSource {
    boolean tagExists(String tagName);
}
