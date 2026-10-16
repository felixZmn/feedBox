package de._0x2b.resource.dto;

/**
 * One sign-in option on the login panel. Structured as a list so additional
 * IdPs can be added later without changing the login layout.
 */
public record AuthProviderDto(String id, String label, String href) {
}
