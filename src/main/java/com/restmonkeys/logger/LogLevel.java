package com.restmonkeys.logger;

public enum LogLevel {
    INFO(1), DEBUG(2), WARN(3), ERROR(4);
    private int level;

    LogLevel(int level) {
        this.level = level;
    }

    public int compare(LogLevel l) {
        if (this.level == l.level) return 0;
        if (this.level > l.level) return 1;
        return -1;
    }
}
