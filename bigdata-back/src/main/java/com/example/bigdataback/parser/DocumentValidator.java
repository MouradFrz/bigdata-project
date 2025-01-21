package com.example.bigdataback.parser;

import org.bson.Document;

import java.util.List;

public class DocumentValidator {

    public static boolean containsInvalidParts(Document document) {
        for (String key : document.keySet()) {
            Object value = document.get(key);

            if (value instanceof Document) {
                if (isEmptyDocument((Document) value)) {
                    return true;
                }
                if (containsInvalidParts((Document) value)) {
                    return true;
                }
            } else if (value instanceof List) {
                for (Object item : (List<?>) value) {
                    if (item instanceof Document && isEmptyDocument((Document) item)) {
                        return true;
                    }
                    if (item instanceof Document && containsInvalidParts((Document) item)) {
                        return true;
                    }
                }
            } else if (value == null) {
                return true;
            }
        }
        return false;
    }

    private static boolean isEmptyDocument(Document document) {
        return document.isEmpty();
    }
}

