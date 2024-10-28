package com.zenith.database;

import com.zenith.Proxy;
import com.zenith.event.proxy.DatabaseTickEvent;
import lombok.Getter;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.postgres.PostgresPlugin;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.zenith.Shared.*;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Getter
public class DatabaseManager {
    private QueueWaitDatabase queueWaitDatabase;
    private ConnectionsDatabase connectionsDatabase;
    private ChatDatabase chatDatabase;
    private DeathsDatabase deathsDatabase;
    private Jdbi jdbi;
    private QueueLengthDatabase queueLengthDatabase;
    private RestartsDatabase restartsDatabase;
    private PlayerCountDatabase playerCountDatabase;
    private TablistDatabase tablistDatabase;
    private PlaytimeDatabase playtimeDatabase;
    private QueryExecutor queryExecutor;
    private RedisClient redisClient;
    private ScheduledFuture<?> databaseTickFuture;

    public DatabaseManager() {

    }

    public void start() {
        try {
            this.queryExecutor = new QueryExecutor(getJdbi());
            if (CONFIG.database.queueWaitEnabled) {
                startQueueWaitDatabase();
            }
            if (CONFIG.database.connectionsEnabled) {
                startConnectionsDatabase();
            }
            if (CONFIG.database.chatsEnabled) {
                startChatsDatabase();
            }
            if (CONFIG.database.deathsEnabled) {
                startDeathsDatabase();
            }
            if (CONFIG.database.queueLengthEnabled) {
                startQueueLengthDatabase();
            }
            if (CONFIG.database.restartsEnabled) {
                startRestartsDatabase();
            }
            if (CONFIG.database.playerCountEnabled) {
                startPlayerCountDatabase();
            }
            if (CONFIG.database.tablistEnabled) {
                startTablistDatabase();
            }
            if (CONFIG.database.playtimeEnabled) {
                startPlaytimeDatabase();
            }
            if (databaseTickFuture != null) {
                databaseTickFuture.cancel(false);
            }
            databaseTickFuture = EXECUTOR
                .scheduleAtFixedRate(this::postDatabaseTick,
                                     DatabaseTickEvent.TICK_INTERVAL_SECONDS,
                                     DatabaseTickEvent.TICK_INTERVAL_SECONDS,
                                     TimeUnit.SECONDS);
        } catch (final Exception e) {
            DATABASE_LOG.error("Failed starting databases", e);
        }
    }

    public void stop() {
        try {
            if (nonNull(databaseTickFuture)) databaseTickFuture.cancel(false);
            stopQueueWaitDatabase();
            stopConnectionsDatabase();
            stopChatsDatabase();
            stopDeathsDatabase();
            stopQueueLengthDatabase();
            stopRestartsDatabase();
            stopPlayerCountDatabase();
            stopTablistDatabase();
            stopPlaytimeDatabase();
        } catch (final Exception e) {
            DATABASE_LOG.error("Failed stopping databases", e);
        }
    }

    public void postDatabaseTick() {
        try {
            // todo: there's a (uncommon) race condition here, if the proxy disconnects and the cache resets during the tick event
            //  the faster each handler executes the more unlikely that is to happen but its still possible
            if (Proxy.getInstance().isOnlineOn2b2tForAtLeastDuration(Duration.ofSeconds(30)))
                EVENT_BUS.post(DatabaseTickEvent.INSTANCE);
        } catch (final Throwable e) {
            DATABASE_LOG.error("Failed posting database tick event", e);
        }
    }

    public void startQueueWaitDatabase() {
        if (nonNull(this.queueWaitDatabase)) {
            this.queueWaitDatabase.start();
        } else {
            this.queueWaitDatabase = new QueueWaitDatabase(queryExecutor);
            this.queueWaitDatabase.start();
        }
    }

    public void stopQueueWaitDatabase() {
        if (nonNull(this.queueWaitDatabase)) {
            this.queueWaitDatabase.stop();
        }
    }

    public void startConnectionsDatabase() {
        if (nonNull(this.connectionsDatabase)) {
            this.connectionsDatabase.start();
        } else {
            this.connectionsDatabase = new ConnectionsDatabase(queryExecutor, getRedisClient());
            this.connectionsDatabase.start();
        }
    }

    public void stopConnectionsDatabase() {
        if (nonNull(this.connectionsDatabase)) {
            this.connectionsDatabase.stop();
        }
    }

    public void startChatsDatabase() {
        if (nonNull(this.chatDatabase)) {
            this.chatDatabase.start();
        } else {
            this.chatDatabase = new ChatDatabase(queryExecutor, getRedisClient());
            this.chatDatabase.start();
        }
    }

    public void stopChatsDatabase() {
        if (nonNull(this.chatDatabase)) {
            this.chatDatabase.stop();
        }
    }

    public void startDeathsDatabase() {
        if (nonNull(this.deathsDatabase)) {
            this.deathsDatabase.start();
        } else {
            this.deathsDatabase = new DeathsDatabase(queryExecutor, getRedisClient());
            this.deathsDatabase.start();
        }
    }

    public void stopDeathsDatabase() {
        if (nonNull(this.deathsDatabase)) {
            this.deathsDatabase.stop();
        }
    }

    public void startQueueLengthDatabase() {
        if (nonNull(this.queueLengthDatabase)) {
            this.queueLengthDatabase.start();
        } else {
            this.queueLengthDatabase = new QueueLengthDatabase(queryExecutor, getRedisClient());
            this.queueLengthDatabase.start();
        }
    }

    public void stopQueueLengthDatabase() {
        if (nonNull(this.queueLengthDatabase)) {
            this.queueLengthDatabase.stop();
        }
    }

    public void startRestartsDatabase() {
        if (nonNull(this.restartsDatabase)) {
            this.restartsDatabase.start();
        } else {
            this.restartsDatabase = new RestartsDatabase(queryExecutor, getRedisClient());
            this.restartsDatabase.start();
        }
    }

    public void stopRestartsDatabase() {
        if (nonNull(this.restartsDatabase)) {
            this.restartsDatabase.stop();
        }
    }

    public void startPlayerCountDatabase() {
        if (nonNull(this.playerCountDatabase)) {
            this.playerCountDatabase.start();
        } else {
            this.playerCountDatabase = new PlayerCountDatabase(queryExecutor, getRedisClient());
            this.playerCountDatabase.start();
        }
    }

    public void stopPlayerCountDatabase() {
        if (nonNull(this.playerCountDatabase)) {
            this.playerCountDatabase.stop();
        }
    }

    public void startTablistDatabase() {
        if (nonNull(this.tablistDatabase)) {
            this.tablistDatabase.start();
        } else {
            this.tablistDatabase = new TablistDatabase(queryExecutor, getRedisClient());
            this.tablistDatabase.start();
        }
    }

    public void stopTablistDatabase() {
        if (nonNull(this.tablistDatabase)) {
            this.tablistDatabase.stop();
        }
    }

    public void startPlaytimeDatabase() {
        if (nonNull(this.playtimeDatabase)) {
            this.playtimeDatabase.start();
        } else {
            this.playtimeDatabase = new PlaytimeDatabase(queryExecutor, getRedisClient());
            this.playtimeDatabase.start();
        }
    }

    public void stopPlaytimeDatabase() {
        if (nonNull(this.playtimeDatabase)) {
            this.playtimeDatabase.stop();
        }
    }

    private synchronized Jdbi getJdbi() {
        if (isNull(this.jdbi)) {
            this.jdbi = Jdbi.create(new HikariConnectionFactory(new ConnectionPool()));
            this.jdbi.installPlugin(new PostgresPlugin());
        }
        return jdbi;
    }

    private synchronized RedisClient getRedisClient() {
        if (isNull(this.redisClient)) this.redisClient = new RedisClient();
        return redisClient;
    }
}
