package com.zenith.database;

import java.time.Instant;

public class TablistTextDatabase extends LockingDatabase {
    public TablistTextDatabase(final QueryExecutor queryExecutor, final RedisClient redisClient) {
        super(queryExecutor, redisClient);
    }

    @Override
    public String getLockKey() {
        return "TablistText";
    }

    @Override
    public Instant getLastEntryTime() {
        return null;
    }

    @Override
    public void subscribeEvents() {

    }
}
