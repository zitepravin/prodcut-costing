package com.automotive.costing.common.exception;

/** Thrown when a requested entity does not exist. Maps to HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Convenience constructor used by services: {@code new ResourceNotFoundException("Entity", id)}.
     */
    public ResourceNotFoundException(String entityType, Object id) {
        super(entityType + " not found with id: " + id);
    }

    public static ResourceNotFoundException of(String entityType, Object id) {
        return new ResourceNotFoundException(entityType, id);
    }
}
