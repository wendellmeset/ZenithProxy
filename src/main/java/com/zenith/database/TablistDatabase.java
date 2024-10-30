package com.zenith.database;

import com.zenith.event.proxy.DatabaseTickEvent;

import java.time.Instant;
import java.time.ZoneOffset;

import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.EVENT_BUS;

public class TablistDatabase extends LockingDatabase {
    public TablistDatabase(final QueryExecutor queryExecutor, final RedisClient redisClient) {
        super(queryExecutor, redisClient);
    }

    @Override
    public void subscribeEvents() {
        EVENT_BUS.subscribe(this,
            DatabaseTickEvent.class, this::handleTickEvent
        );
    }

    @Override
    public String getLockKey() {
        return "Tablist";
    }

    @Override
    public Instant getLastEntryTime() {
        return Instant.now();
    }

    private void handleTickEvent(DatabaseTickEvent event) {
        // we aren't using the queue based insert system here so we need to check if we have the lock manually
        if (this.lockAcquired.get()) {
            syncTablist();
        }
    }

    private void syncTablist() {
        try (var handle = this.queryExecutor.jdbi().open()) {
            handle.inTransaction(transaction -> {
                transaction.createUpdate("LOCK TABLE tablist;").execute();
                transaction.createUpdate("DELETE FROM tablist;").execute();
                var batch = transaction.prepareBatch("INSERT INTO tablist (player_name, player_uuid, time) VALUES (:player_name, :player_uuid, :time);");
                for (var entry : CACHE.getTabListCache().getEntries()) {
                    batch
                        .bind("player_name", entry.getName())
                        .bind("player_uuid", entry.getProfileId())
                        .bind("time", Instant.now().atOffset(ZoneOffset.UTC))
                        .add();
                }
                return batch.execute();
            });
        }
    }
}
