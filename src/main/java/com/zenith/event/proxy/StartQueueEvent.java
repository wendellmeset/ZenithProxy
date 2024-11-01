package com.zenith.event.proxy;

import java.time.Duration;

public record StartQueueEvent(boolean wasOnline, Duration wasOnlineDuration) { }
