package com.restmonkeys.logger;

import org.slf4j.MarkerFactory;
import org.slf4j.helpers.BasicMarker;
import org.slf4j.helpers.BasicMarkerFactory;

public enum LogLevel {
    INFO(1), DEBUG(2), WARN(3), ERROR(4), FALLBACK(100);
    private int level;

    LogLevel(int level) {
        this.level = level;
    }

    public int compare(LogLevel l) {
        if (this.level == l.level) return 0;
        if (this.level > l.level) return 1;
        return -1;
    }

    public void log(org.slf4j.Logger logger, Logger.LogEntity logEntity) {
        switch (this) {
            case INFO:
                logger.info(logEntity.toString());
                break;
            case DEBUG:
                logger.debug(logEntity.toString());
                break;
            case WARN:
                logger.warn(logEntity.toString());
                break;
            case ERROR:
                logger.error(logEntity.toString());
                break;
            case FALLBACK:
                logger.error(MarkerFactory.getMarker("FALLBACK"), logEntity.toString());
                break;
        }
    }
}
