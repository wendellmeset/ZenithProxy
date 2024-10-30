package com.zenith.database;

import com.zenith.event.proxy.RedisRestartEvent;
import org.jdbi.v3.core.HandleConsumer;
import org.redisson.api.RBoundedBlockingQueue;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Shared.*;

public abstract class LiveDatabase extends LockingDatabase {

    private RBoundedBlockingQueue<String> queue;
    private final Object eventListener = new Object();
    private final AtomicBoolean redisRestarted = new AtomicBoolean(false);

    public LiveDatabase(final QueryExecutor queryExecutor, final RedisClient redisClient) {
        super(queryExecutor, redisClient);
    }

    @Override
    public void start() {
        super.start();
        EVENT_BUS.subscribe(eventListener, of(RedisRestartEvent.class, e -> redisRestarted.set(true)));
        queue = buildQueue();
    }

    @Override
    public void stop() {
        super.stop();
        EVENT_BUS.unsubscribe(eventListener);
        queue = null;
    }

    private RBoundedBlockingQueue<String> buildQueue() {
        final RBoundedBlockingQueue<String> q = redisClient.getRedissonClient().getBoundedBlockingQueue(getQueueKey());
        q.trySetCapacity(500);
        return q;
    }

    public void insert(final Instant instant, final Object pojo, final HandleConsumer query) {
        insert(instant, () -> liveQueueRunnable(pojo), query);
    }

    private String getQueueKey() {
        return getLockKey() + "Queue";
    }

    // todo: should we not extend locking database queue system so we can move live impl out of locking database?
    // todo: refactor locking database class into hierarchy

    void liveQueueRunnable(Object pojo) {
        try {
            if (queue == null || redisRestarted.getAndSet(false)) {
                queue = buildQueue();
            }
            String json = OBJECT_MAPPER.writeValueAsString(pojo);
            queue.offerAsync(json).thenAcceptAsync((success) -> {
                if (!success) {
                    DATABASE_LOG.warn("{} reached capacity, flushing queue", getQueueKey());
                    queue.clear();
                }
            });
        } catch (final Exception e) {
            DATABASE_LOG.error("Failed to offer record to: {}", getQueueKey(), e);
        }
    }

}
