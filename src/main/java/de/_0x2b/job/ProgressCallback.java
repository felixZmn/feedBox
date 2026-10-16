package de._0x2b.job;

/**
 * Optional progress sink for long-running feed refresh / OPML import work.
 */
public interface ProgressCallback {

    void setTotal(int total);

    void itemSucceeded();

    void itemFailed();

    /**
     * Record a bounded per-row OPML outcome (implementations may drop excess).
     * Default is a no-op so refresh jobs need not implement it.
     */
    default void recordOutcome(ImportRowOutcome outcome) {
    }

    /** No-op callback for callers that do not track progress. */
    ProgressCallback NONE = new ProgressCallback() {
        @Override
        public void setTotal(int total) {
        }

        @Override
        public void itemSucceeded() {
        }

        @Override
        public void itemFailed() {
        }
    };
}
