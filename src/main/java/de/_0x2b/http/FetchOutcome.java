package de._0x2b.http;

/**
 * Result of an outbound GET: either the {@link FetchResult} body or the
 * {@link FetchFailure} explaining why there is none.
 */
public sealed interface FetchOutcome permits FetchResult, FetchFailure {
}
