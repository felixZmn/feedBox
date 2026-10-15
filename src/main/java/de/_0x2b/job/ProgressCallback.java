package de._0x2b.job;

/**
 * Optional progress sink for long-running feed refresh / OPML import work.
 */
public interface ProgressCallback {

    void setTotal(int total);

    void itemSucceeded();

    void itemFailed();

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
