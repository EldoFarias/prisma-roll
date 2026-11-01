package com.farias.prismaroll.game

import java.util.concurrent.Executors

/**
 * Levels are generated and checked by the bot, which takes a few milliseconds on a phone. This keeps
 * the next ones ready (built on a background thread) so starting a level never stutters.
 */
object LevelCache {
    private val worker = Executors.newSingleThreadExecutor { r -> Thread(r, "levels").apply { isDaemon = true; priority = Thread.MIN_PRIORITY } }
    private val cache = LinkedHashMap<Long, RollLevel>(16, 0.75f, true)
    private val pending = HashSet<Long>()

    private fun key(n: Int, salt: Long) = n.toLong() * 1_000_003L + salt

    /** The level, from the cache when ready, else built now. */
    fun get(n: Int, salt: Long = 0L, build: () -> RollLevel = { RollLevels.level(n, salt) }): RollLevel {
        val k = key(n, salt)
        synchronized(this) { cache[k]?.let { return it } }
        val lv = build()
        put(k, lv)
        return lv
    }

    fun daily(day: Int): RollLevel = get(-day, 7L) { RollLevels.daily(day) }

    // Endless tracks are long to build: always keep the next one ready.
    @Volatile private var nextEndless: RollLevel? = null
    @Volatile private var buildingEndless = false

    fun prefetchEndless() {
        synchronized(this) {
            if (nextEndless != null || buildingEndless) return
            buildingEndless = true
        }
        worker.execute {
            val lv = RollLevels.endless(System.nanoTime())
            synchronized(this) { nextEndless = lv; buildingEndless = false }
        }
    }

    /** A fresh endless track (ready one if prefetched), and starts building the next. */
    fun endless(): RollLevel {
        val lv = synchronized(this) { nextEndless.also { nextEndless = null } } ?: RollLevels.endless(System.nanoTime())
        prefetchEndless()
        return lv
    }

    /** Builds a level in the background if it isn't ready yet. */
    fun prefetch(n: Int) {
        val k = key(n, 0L)
        synchronized(this) {
            if (cache.containsKey(k) || !pending.add(k)) return
        }
        worker.execute {
            val lv = RollLevels.level(n)
            put(k, lv)
            synchronized(this) { pending.remove(k) }
        }
    }

    /** Returns a ready level if there is one (never blocks). */
    fun ready(n: Int): RollLevel? = synchronized(this) { cache[key(n, 0L)] }

    private fun put(k: Long, lv: RollLevel) = synchronized(this) {
        cache[k] = lv
        while (cache.size > 12) cache.remove(cache.keys.first())
    }
}
