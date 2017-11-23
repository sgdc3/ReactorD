package com.github.sgdc3.reactord;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

public class ConsoleFilter extends Filter<ILoggingEvent> {

    public ConsoleFilter() {
    }

    @Override
    public FilterReply decide(ILoggingEvent event) {
        if (event.getMessage().contains("Received unknown packet of type")) {
            return FilterReply.DENY;
        } else {
            return FilterReply.NEUTRAL;
        }
    }
}
