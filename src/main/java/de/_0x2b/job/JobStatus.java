package de._0x2b.job;

public enum JobStatus {
    RUNNING,
    COMPLETED,
    FAILED;

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED;
    }
}
