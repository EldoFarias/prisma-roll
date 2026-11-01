package com.farias.prismaroll.game

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/*
 * Pure rules of the sky track (no Android). The track runs forward along the distance "s"; in the 3D
 * world a point (x, y, s) is drawn at (x, y, -s). The ball rolls forward on its own; the player only
 * steers it left and right. Falling off the track or hitting an obstacle ends the run.
 */

/** What a piece of track does when the ball rolls on it. */
object SegKind {
    const val NORMAL = 0
    /** Speed pad: faster for the next [Roll.BOOST_LEN] units. */
    const val BOOST = 1
    /** Jump pad: launches the ball over a big gap. */
    const val JUMP = 2
    /** Checkered finish line. */
    const val FINISH = 3
    /** Stepping stone (small tile). */
    const val STONE = 4
    /** Glass tile (looks fragile, only decoration). */
    const val GLASS = 5
}

/**
 * A straight piece of track from [s0] to [s0] + [len]. Its centre goes from [x0] to [x1] and its top
 * from [h0] to [h1] (slants and ramps), [w] wide.
 */
class Seg(
    val s0: Float, val len: Float,
    val x0: Float, val x1: Float,
    val w: Float,
    val h0: Float, val h1: Float,
    val kind: Int = SegKind.NORMAL,
    /** Colour variant for the renderer (0/1 alternate, 2 = accent). */
    val tone: Int = 0,
) {
    val s1 get() = s0 + len
    fun cx(s: Float) = x0 + (x1 - x0) * ((s - s0) / len)
    fun top(s: Float) = h0 + (h1 - h0) * ((s - s0) / len)
    val slope get() = (h1 - h0) / len
    val slant get() = (x1 - x0) / len
}

object ObsKind {
    /** A still block. */
    const val BLOCK = 0
    /** A block sliding left and right. */
    const val SLIDER = 1
    /** A bar spinning around its centre, low over the track. */
    const val SPINNER = 2
    /** A hammer swinging from a frame overhead (moves like a slider). */
    const val HAMMER = 3
    /** A wall that rises and falls (open when up). */
    const val GATE = 4
    /** A coin to grab (not an obstacle). */
    const val COIN = 5
}

/**
 * Something on the track at distance [s]. Sizes: [w] across, [d] along, [h] tall, standing on [y].
 * Moving ones use [amp] (units, or arm length for spinners), [freq] (rad/s) and [phase].
 */
class Obs(
    val kind: Int,
    val x: Float, val s: Float, val y: Float,
    val w: Float, val d: Float, val h: Float,
    val amp: Float = 0f, val freq: Float = 0f, val phase: Float = 0f,
) {
    /** Centre x at time [t] (sliders and hammers move). */
    fun cx(t: Float): Float = when (kind) {
        ObsKind.SLIDER, ObsKind.HAMMER -> x + amp * sin(freq * t + phase)
        else -> x
    }

    /** Spinner bar angle at time [t]. */
    fun angle(t: Float) = phase + freq * t

    /** Gate: how far up it is (0 closed, 1 fully open) at time [t]. */
    fun open(t: Float): Float = (0.5f + 0.9f * sin(freq * t + phase)).coerceIn(0f, 1f)

    /** Gate bottom height at time [t]. */
    fun gateBottom(t: Float) = y + open(t) * (h + 0.25f)

    /** How far (units) the ball surface is from this obstacle; negative = touching. */
    fun gap(bx: Float, bs: Float, by: Float, t: Float): Float {
        val r = Roll.BALL_R
        when (kind) {
            ObsKind.COIN -> return 99f
            ObsKind.SPINNER -> {
                if (by - r > y + 0.75f) return 99f
                val a = angle(t)
                val dx = cos(a); val ds = sin(a)
                // Distance from the ball to the bar (a segment through the centre, both arms).
                val px = bx - x; val ps = bs - s
                val along = (px * dx + ps * ds).coerceIn(-amp, amp)
                val qx = px - along * dx; val qs = ps - along * ds
                return sqrt(qx * qx + qs * qs) - r - 0.2f
            }
            else -> {
                val bottom = if (kind == ObsKind.GATE) gateBottom(t) else y
                if (by - r > bottom + h || by + r < bottom) return 99f
                val ex = max(abs(bx - cx(t)) - w / 2f, 0f)
                val es = max(abs(bs - s) - d / 2f, 0f)
                return sqrt(ex * ex + es * es) - r
            }
        }
    }
}

class RollLevel(
    val number: Int,
    val segs: List<Seg>,
    val obs: List<Obs>,
    /** The safe line the level was built around: x at distance s (pairs s, x). */
    val pathS: FloatArray,
    val pathX: FloatArray,
    /** Where the finish line is. */
    val finish: Float,
    /** Base rolling speed (units/s). */
    val speed: Float,
    val boss: Boolean = false,
    val endless: Boolean = false,
) {
    /** Mechanics that show up in this level (for the "new!" notice). */
    val mechanics: Set<String> by lazy {
        val m = HashSet<String>()
        for (o in obs) when (o.kind) {
            ObsKind.SLIDER -> m.add("slider"); ObsKind.SPINNER -> m.add("spinner")
            ObsKind.HAMMER -> m.add("hammer"); ObsKind.GATE -> m.add("gate")
            ObsKind.COIN -> m.add("coins")
        }
        for (g in segs) when (g.kind) {
            SegKind.JUMP -> m.add("jump"); SegKind.BOOST -> m.add("boost"); SegKind.STONE -> m.add("stones")
        }
        m
    }

    /** Longest segment (bounds the support search). */
    val maxLen: Float = segs.maxOfOrNull { it.len } ?: 1f
    /** Coins in the level. */
    val coins: Int = obs.count { it.kind == ObsKind.COIN }

    /** The safe line's x at distance [s]. */
    fun path(s: Float): Float {
        val n = pathS.size
        if (n == 0) return 0f
        if (s <= pathS[0]) return pathX[0]
        if (s >= pathS[n - 1]) return pathX[n - 1]
        var lo = 0; var hi = n - 1
        while (hi - lo > 1) { val m = (lo + hi) ushr 1; if (pathS[m] <= s) lo = m else hi = m }
        val u = (s - pathS[lo]) / max(1e-4f, pathS[hi] - pathS[lo])
        return pathX[lo] + (pathX[hi] - pathX[lo]) * u
    }

    /** Index of the last segment starting at or before [s] (-1 if none). */
    fun segIndex(s: Float): Int {
        var lo = 0; var hi = segs.size - 1; var ans = -1
        while (lo <= hi) { val m = (lo + hi) ushr 1; if (segs[m].s0 <= s) { ans = m; lo = m + 1 } else hi = m - 1 }
        return ans
    }

    /** Index of the first obstacle at or after [s]. */
    fun obsIndex(s: Float): Int {
        var lo = 0; var hi = obs.size
        while (lo < hi) { val m = (lo + hi) ushr 1; if (obs[m].s < s) lo = m + 1 else hi = m }
        return lo
    }

    /** The highest track top under (x, s), or null if there's only sky. */
    fun support(s: Float, x: Float): Seg? {
        var i = segIndex(s)
        var best: Seg? = null
        while (i >= 0) {
            val g = segs[i]
            if (g.s0 < s - maxLen) break
            if (s <= g.s1 && abs(x - g.cx(s)) <= g.w / 2f) {
                if (best == null || g.top(s) > best.top(s)) best = g
            }
            i--
        }
        return best
    }
}

object Roll {
    const val BALL_R = 0.36f
    const val GRAVITY = 32f
    /** Track slab thickness (for drawing and side hits). */
    const val THICK = 0.55f
    /** Fastest the ball can move sideways (units/s), chasing the finger. */
    const val LAT_MAX = 11f
    /** Sideways speed the safe line never needs more than (comfortable for a person). */
    const val HUMAN_LAT = 5.5f
    const val JUMP_V = 11.5f
    const val BOOST_MUL = 1.32f
    const val BOOST_LEN = 32f
    /** Below the last ground by this much = fell off. */
    const val FALL_DEPTH = 6f
    /** Highest step the ball rolls down without taking off; landings this far below the top still count. */
    const val SNAP = 0.4f
}

// ------------------------------------------------------------------------------------ simulation

sealed class RollEvent {
    data class Coin(val index: Int) : RollEvent()
    /** Took off from a ramp or jump pad ([big] = jump pad). */
    data class Jump(val big: Boolean) : RollEvent()
    /** Touched down after a jump; [impact] = vertical speed. */
    data class Land(val impact: Float) : RollEvent()
    object Boost : RollEvent()
    /** Passed very close to an obstacle (slow motion, points). */
    data class Near(val obs: Int, val points: Int) : RollEvent()
    /** Hit obstacle [obs] (or the side of the track when -1). */
    data class Crash(val obs: Int) : RollEvent()
    /** A shield saved the ball: obstacle [obs] breaks. */
    data class Shield(val obs: Int) : RollEvent()
    object Fall : RollEvent()
    object Win : RollEvent()
}

/** The ball on the track over time. The player sets [targetX]; [step] moves the ball. */
class RollSim(val level: RollLevel) {
    var x = level.path(0f)
        private set
    var s = 0f
        private set
    var y = (level.support(0f, x)?.top(0f) ?: 0f) + Roll.BALL_R
        private set
    var vy = 0f
        private set
    var vz = level.speed
        private set
    /** Where the finger wants the ball. */
    var targetX = x
    /** Time since the start (stops while waiting for the first touch). */
    var time = 0f
        private set
    var started = false
    var grounded = true
        private set
    var lastGroundY = y - Roll.BALL_R
        private set
    var dead = false
        private set
    var fell = false
        private set
    var won = false
        private set
    var score = 0
        private set
    var coins = 0
        private set
    /** Hits it can survive (bought with a video): the obstacle breaks. */
    var shields = 0
    /** No obstacle hits until this time (after a revive or a shield). */
    var ghostUntil = 0f
        private set
    /** Total rolled angle (for drawing the ball). */
    var spin = 0f
        private set
    var lean = 0f
        private set
    private var boostUntil = -1f
    val boosted get() = s < boostUntil
    private val taken = BooleanArray(level.obs.size)
    private val nearDone = BooleanArray(level.obs.size)
    private val broken = BooleanArray(level.obs.size)
    private var airborneFrom = 0f
    private var wasJumpPad = false

    fun coinTaken(i: Int) = taken[i]
    fun broken(i: Int) = broken[i]

    /** 0..1 to the finish. */
    val progress get() = (s / level.finish).coerceIn(0f, 1f)

    fun step(dt: Float, ev: MutableList<RollEvent>) {
        if (dead) return
        if (!started) return
        time += dt
        val lv = level
        // Forward speed: base, faster on boosts, slowing after the finish.
        val want = if (won) lv.speed * 0.45f else lv.speed * (if (boosted) Roll.BOOST_MUL else 1f)
        vz += (want - vz) * min(1f, dt * (if (won) 1.6f else 5f))
        s += vz * dt
        spin += vz * dt / Roll.BALL_R
        // Sideways: chase the finger, limited speed.
        val dx = (targetX - x).coerceIn(-Roll.LAT_MAX * dt, Roll.LAT_MAX * dt)
        x += dx
        lean += (dx / dt / Roll.LAT_MAX - lean) * min(1f, dt * 10f)

        vy -= Roll.GRAVITY * dt
        y += vy * dt
        val g = lv.support(s, x)
        val bottom = y - Roll.BALL_R
        if (g != null) {
            val top = g.top(s)
            if (grounded) {
                if (bottom <= top + Roll.SNAP && bottom >= top - Roll.SNAP) {
                    y = top + Roll.BALL_R
                    vy = g.slope * vz
                    onGround(g, ev)
                } else if (bottom < top - Roll.SNAP) {
                    crash(-1, ev); return
                } else {
                    takeOff(ev)
                }
            } else if (vy <= 0f && bottom <= top && bottom >= top - Roll.SNAP - (-vy * dt)) {
                y = top + Roll.BALL_R
                val impact = -vy
                vy = g.slope * vz
                grounded = true
                ev.add(RollEvent.Land(impact))
                onGround(g, ev)
            } else if (bottom < top - Roll.SNAP && y > top - Roll.THICK - Roll.BALL_R && !grounded && vy > 0f) {
                // Coming up under the track (rare): bonk.
                vy = 0f
            } else if (bottom < top - Roll.SNAP && y > top - Roll.THICK) {
                crash(-1, ev); return
            }
        } else if (grounded) {
            takeOff(ev)
        }
        if (!grounded && y < lastGroundY - Roll.FALL_DEPTH) {
            dead = true
            fell = true
            ev.add(RollEvent.Fall)
            return
        }

        // Obstacles and coins near the ball.
        if (!won) {
            var i = lv.obsIndex(s - 4f)
            while (i < lv.obs.size) {
                val o = lv.obs[i]
                if (o.s > s + 4f) break
                if (o.kind == ObsKind.COIN) {
                    if (!taken[i]) {
                        val ddx = x - o.x; val dds = s - o.s; val ddy = y - o.y
                        if (ddx * ddx + dds * dds + ddy * ddy < 0.8f) {
                            taken[i] = true
                            coins++
                            score += 5
                            ev.add(RollEvent.Coin(i))
                        }
                    }
                } else if (!broken[i]) {
                    val gp = o.gap(x, s, y, time)
                    if (gp < 0f && time >= ghostUntil) {
                        if (shields > 0) {
                            shields--
                            broken[i] = true
                            ghostUntil = time + 0.6f
                            ev.add(RollEvent.Shield(i))
                            i++
                            continue
                        }
                        crash(i, ev); return
                    }
                    if (gp in 0f..0.3f && !nearDone[i]) {
                        nearDone[i] = true
                        score += 10
                        ev.add(RollEvent.Near(i, 10))
                    }
                }
                i++
            }
        }
        if (!won && s >= lv.finish) {
            won = true
            score += 50
            ev.add(RollEvent.Win)
        }
    }

    private fun onGround(g: Seg, ev: MutableList<RollEvent>) {
        lastGroundY = y - Roll.BALL_R
        when (g.kind) {
            SegKind.JUMP -> {
                vy = Roll.JUMP_V
                grounded = false
                wasJumpPad = true
                airborneFrom = s
                ev.add(RollEvent.Jump(true))
            }
            SegKind.BOOST -> {
                if (!boosted) ev.add(RollEvent.Boost)
                boostUntil = s + Roll.BOOST_LEN
            }
        }
    }

    private fun takeOff(ev: MutableList<RollEvent>) {
        grounded = false
        airborneFrom = s
        if (vy > 2.2f) ev.add(RollEvent.Jump(false))
    }

    private fun crash(i: Int, ev: MutableList<RollEvent>) {
        dead = true
        ev.add(RollEvent.Crash(i))
    }

    /**
     * Second chance after a video: back on the safe line a little before where it died, with the clock
     * set to when the safe line passes there (so moving obstacles are where they were planned to be).
     * Waits for the next touch.
     */
    fun revive(times: FloatArray) {
        if (!dead) return
        var rs = (s - 5f).coerceAtLeast(0f)
        // Find a spot on solid track, far from obstacles.
        var tries = 0
        while (rs > 0f && tries < 40) {
            val px = level.path(rs)
            val g = level.support(rs, px)
            var clear = g != null && g.kind == SegKind.NORMAL && g.slope == 0f
            if (clear) {
                var i = level.obsIndex(rs - 3f)
                while (i < level.obs.size && level.obs[i].s < rs + 6f) {
                    if (level.obs[i].kind != ObsKind.COIN) { clear = false; break }
                    i++
                }
            }
            if (clear) break
            rs -= 1f
            tries++
        }
        rs = rs.coerceAtLeast(0f)
        s = rs
        x = level.path(rs)
        targetX = x
        val g = level.support(rs, x)
        y = (g?.top(rs) ?: lastGroundY) + Roll.BALL_R
        lastGroundY = y - Roll.BALL_R
        vy = 0f
        vz = level.speed
        boostUntil = -1f
        grounded = true
        dead = false
        fell = false
        time = timeAt(times, rs)
        ghostUntil = time + 1.2f
        started = false
    }

    companion object {
        /** Planned time at distance [s] from a table built by [RollLevels.timeTable]. */
        fun timeAt(times: FloatArray, s: Float): Float {
            val k = s / RollLevels.TIME_STEP
            val i = k.toInt().coerceIn(0, times.size - 2)
            val u = (k - i).coerceIn(0f, 1f)
            return times[i] + (times[i + 1] - times[i]) * u
        }
    }
}

// ------------------------------------------------------------------------------------ levels

/** Endless, deterministic levels: the same number always builds the same track. */
object RollLevels {

    /** 0 = first levels, 1 = fully hard (around level 100). */
    fun difficulty(n: Int): Float = ((n - 1) / 100f).coerceIn(0f, 1f)

    fun isBoss(n: Int): Boolean = n >= 10 && n % 10 == 0

    fun speedFor(n: Int, d: Float) = 9.5f + 4.5f * d + if (isBoss(n)) 0.6f else 0f

    fun lengthFor(n: Int): Float = when {
        n <= 2 -> 150f
        isBoss(n) -> min(260f + n * 3.2f, 720f)
        else -> min(170f + n * 2.6f, 560f)
    }

    // First level each mechanic can appear in.
    const val RAMPS_FROM = 2
    const val BLOCKS_FROM = 2
    const val GAPS_FROM = 4
    const val BOOST_FROM = 5
    const val NARROW_FROM = 6
    const val WALL_FROM = 8
    const val SLIDER_FROM = 10
    const val STONES_FROM = 14
    const val JUMP_FROM = 18
    const val SPINNER_FROM = 22
    const val HAMMER_FROM = 26
    const val GATE_FROM = 32

    const val TIME_STEP = 0.5f

    /** Daily challenge: harder than usual, the same for everyone that day. */
    fun daily(day: Int): RollLevel = level(35 + day % 50, salt = day * 2_654_435_761L)

    /** Endless mode: checked chunks of rising difficulty glued into one long run. */
    fun endless(seed: Long): RollLevel {
        val b = Builder(1, 0.15f, Random(seed), speedFor(30, 0.3f), endless = true)
        b.start()
        var n = 4
        while (b.s < 2600f) {
            b.d = difficulty(n)
            b.n = n
            b.chunk()
            if (b.s > n * 30f) n += 2
        }
        b.finish()
        val lv = b.build(0, false, true)
        return if (RollBot.beats(lv)) lv else level(40, seed)
    }

    /**
     * Builds level [number]. Every candidate is played by [RollBot] steering like a person; a level is only
     * kept if the bot finishes it, so no level is impossible.
     */
    fun level(number: Int, salt: Long = 0L): RollLevel {
        val n = number.coerceAtLeast(1)
        var easing = 0f
        for (attempt in 0 until 30) {
            val lv = build(n, (difficulty(n) - easing).coerceAtLeast(0f), salt + attempt * 104_729L)
            if (RollBot.beats(lv)) return lv
            if (attempt % 3 == 2) easing += 0.1f
        }
        return build(1, 0f, salt)
    }

    private fun build(n: Int, d: Float, salt: Long): RollLevel {
        val boss = isBoss(n)
        val rnd = Random(n * 7_919L + 13 + salt)
        val dd = (d + if (boss) 0.12f else 0f).coerceAtMost(1f)
        val b = Builder(n, dd, rnd, speedFor(n, d))
        b.start()
        val len = lengthFor(n)
        while (b.s < len) b.chunk()
        b.finish()
        return b.build(n, boss, false)
    }

    /** Planned time at each [TIME_STEP] of distance, following the speed pads. */
    fun timeTable(lv: RollLevel): FloatArray {
        val n = ((lv.finish + 40f) / TIME_STEP).toInt() + 2
        val t = FloatArray(n)
        // Same speed rule as the sim: eases towards the target speed.
        var v = lv.speed
        var s = 0f
        var time = 0f
        var boostUntil = -1f
        var k = 1
        val dt = 1f / 120f
        var guard = 0
        while (k < n && guard++ < 2_000_000) {
            val g = lv.support(s, lv.path(s))
            if (g != null && g.kind == SegKind.BOOST) boostUntil = s + Roll.BOOST_LEN
            val want = lv.speed * (if (s < boostUntil) Roll.BOOST_MUL else 1f)
            v += (want - v) * min(1f, dt * 5f)
            s += v * dt
            time += dt
            while (k < n && k * TIME_STEP <= s) { t[k] = time - (s - k * TIME_STEP) / v; k++ }
        }
        return t
    }

    /** Lays out the track piece by piece around a safe line. */
    private class Builder(var n: Int, var d: Float, val rnd: Random, val speed: Float, val endless: Boolean = false) {
        val segs = ArrayList<Seg>()
        val obs = ArrayList<Obs>()
        val ps = ArrayList<Float>()
        val px = ArrayList<Float>()
        /** Moving obstacles waiting for the time table. */
        val moving = ArrayList<Obs>()
        var s = 0f
        var x = 0f   // safe line
        var c = 0f   // track centre
        var h = 0f   // track height
        var w = 4.2f
        var tone = 0
        var finishS = 0f
        var lastKind = -1

        fun point(ss: Float, xx: Float) {
            if (ps.isNotEmpty() && ss <= ps.last()) { px[px.size - 1] = xx; return }
            ps.add(ss); px.add(xx)
        }

        /** Sideways distance the safe line may move over [len] units of track. */
        fun reach(len: Float) = len * Roll.HUMAN_LAT / (speed * Roll.BOOST_MUL)

        fun seg(len: Float, x1: Float = c, h1: Float = h, ww: Float = w, kind: Int = SegKind.NORMAL) {
            segs.add(Seg(s, len, c, x1, ww, h, h1, kind, if (kind == SegKind.NORMAL) tone else 2))
            tone = 1 - tone
        }

        fun start() {
            w = 4.4f
            seg(14f)
            point(0f, 0f)
            s = 14f
            point(s, 0f)
        }

        fun finish() {
            // Calm run-up, the checkered line, then track to roll out on.
            w = max(w, 4.4f)
            straight(10f, coins = false)
            finishS = s + 1f
            seg(2f, kind = SegKind.FINISH)
            s += 2f
            seg(40f)
            s += 40f
            point(s, x)
        }

        fun coinsAlong(len: Float, from: Float = s, every: Float = 1.7f) {
            var k = from + 1f
            while (k < from + len - 0.5f) {
                obs.add(Obs(ObsKind.COIN, x, k, h + 0.55f, 0.5f, 0.5f, 0.5f))
                k += every
            }
        }

        fun straight(len: Float, coins: Boolean = rnd.nextFloat() < 0.4f) {
            // The safe line may drift to a new spot on the track (and collect a coin line there).
            val room = (w / 2f - 0.7f).coerceAtLeast(0f)
            val nx = (c + (rnd.nextFloat() * 2f - 1f) * room).let { x + (it - x).coerceIn(-reach(len * 0.6f), reach(len * 0.6f)) }
            seg(len)
            point(s + len * 0.15f, x)
            x = nx
            point(s + len * 0.45f, x)
            if (coins) coinsAlong(len * 0.5f, s + len * 0.45f)
            s += len
            point(s, x)
        }

        fun chunk() {
            val r = rnd.nextFloat()
            val options = ArrayList<Int>()
            options.add(0); options.add(1)
            if (n >= RAMPS_FROM) options.add(2)
            if (n >= BLOCKS_FROM) { options.add(3); options.add(3) }
            if (n >= GAPS_FROM) options.add(4)
            if (n >= BOOST_FROM && lastKind != 5) options.add(5)
            if (n >= NARROW_FROM) options.add(6)
            if (n >= WALL_FROM) options.add(7)
            if (n >= SLIDER_FROM) { options.add(8); if (d > 0.3f) options.add(8) }
            if (n >= STONES_FROM) options.add(9)
            if (n >= JUMP_FROM) options.add(10)
            if (n >= SPINNER_FROM) options.add(11)
            if (n >= HAMMER_FROM) options.add(12)
            if (n >= GATE_FROM) options.add(13)
            // Easier picks show up less as the difficulty rises.
            var k = options[(r * options.size).toInt().coerceAtMost(options.size - 1)]
            if ((k == 0 || k == 1) && rnd.nextFloat() < d * 0.7f) k = options[rnd.nextInt(options.size)]
            if (k == lastKind && rnd.nextFloat() < 0.6f) k = options[rnd.nextInt(options.size)]
            lastKind = k
            // Width of normal track shrinks with difficulty.
            w = 4.4f - 1.4f * d + rnd.nextFloat() * 0.6f
            when (k) {
                0 -> straight(8f + rnd.nextFloat() * 8f, coins = true)
                1 -> slant()
                2 -> ramp()
                3 -> slalom()
                4 -> gapJump()
                5 -> boost()
                6 -> narrow()
                7 -> wallHole()
                8 -> sliders()
                9 -> stones()
                10 -> jumpPad()
                11 -> spinner()
                12 -> hammers()
                13 -> gates()
            }
            // A breather between harder pieces.
            if (k >= 3 && rnd.nextFloat() < 0.7f - d * 0.4f) straight(5f + rnd.nextFloat() * 5f)
        }

        /** The whole track shifts sideways (diagonal piece). */
        fun slant() {
            val len = 10f + rnd.nextFloat() * 10f
            val dx = (rnd.nextFloat() * 2f - 1f) * min(reach(len) * 0.9f, 4f)
            point(s, x)
            seg(len, x1 = c + dx)
            c += dx
            x += dx
            coinsAlongSlant(len, dx)
            s += len
            point(s, x)
        }

        fun coinsAlongSlant(len: Float, dx: Float) {
            if (rnd.nextFloat() > 0.5f) return
            var k = 1.5f
            while (k < len - 1f) {
                obs.add(Obs(ObsKind.COIN, x - dx + dx * (k / len), s + k, h + 0.55f, 0.5f, 0.5f, 0.5f))
                k += 1.8f
            }
        }

        /** Up or down a slope (a hop at the top when steep). */
        fun ramp() {
            val len = 8f + rnd.nextFloat() * 6f
            val up = h < 6f && (h < -6f || rnd.nextBoolean())
            val dh = (if (up) 1f else -1f) * (1f + rnd.nextFloat() * (1.2f + 1.6f * d))
            point(s, x)
            seg(len, h1 = h + dh)
            h += dh
            s += len
            point(s, x)
        }

        /** Blocks on alternating sides: weave between them. */
        fun slalom() {
            val count = 2 + (rnd.nextFloat() * (2f + 3f * d)).toInt()
            val wide = max(w, 4.2f)
            val bwMax = wide * (0.45f + 0.12f * d)
            val lead = 0.45f + Roll.BALL_R + 0.3f
            // Room to cross from one side to the other between two blocks.
            val spacing = 2f * lead + bwMax * speed * Roll.BOOST_MUL / Roll.HUMAN_LAT * 1.1f
            val len = spacing * (count + 1)
            val save = w
            w = wide
            seg(len)
            w = save
            var side = if (rnd.nextBoolean()) 1f else -1f
            var ss = s + spacing * 0.5f
            point(s, x)
            for (i in 0 until count) {
                ss += spacing
                // Block on one side, the safe line on the other.
                val bw = wide * (0.45f + 0.12f * d)
                val bx = c + side * (wide / 2f - bw / 2f)
                val freeX = c - side * (wide / 2f - (wide - bw) / 2f)
                obs.add(Obs(ObsKind.BLOCK, bx, ss, h, bw, 0.9f, 0.9f + rnd.nextFloat() * 0.5f))
                // Be on the free side from a bit before the block until a bit after.
                val maxMove = reach(spacing - 2f * lead)
                x = x + (freeX - x).coerceIn(-maxMove, maxMove)
                point(ss - lead, x)
                point(ss + lead, x)
                side = -side
            }
            s += len
            point(s, x)
        }

        /** Ramp up, a gap in the sky, land further on. */
        fun gapJump() {
            val rampLen = 4f
            val rise = 0.9f + rnd.nextFloat() * 0.6f
            point(s, x)
            seg(rampLen, h1 = h + rise)
            s += rampLen
            val vy0 = rise / rampLen * speed
            val drop = rise + (rnd.nextFloat() * 1.5f)
            // Air time until the ball is [drop] below the take-off point.
            val t = (vy0 + sqrt(vy0 * vy0 + 2f * Roll.GRAVITY * drop)) / Roll.GRAVITY
            val dist = speed * t
            val gap = (dist * (0.55f + 0.2f * d)).coerceAtLeast(1.2f)
            s += gap
            h = h + rise - drop
            val land = 7f
            // The landing can be shifted sideways a little.
            val shift = (rnd.nextFloat() * 2f - 1f) * min(reach(gap + 2f) * 0.7f, 1.4f)
            c += shift
            point(s - gap, x)
            x += shift
            point(s + 1f, x)
            seg(land, ww = max(w, 3.6f))
            s += land
            point(s, x)
        }

        fun boost() {
            val save = w
            w = max(w, 3.6f)
            seg(2.5f, kind = SegKind.BOOST)
            s += 2.5f
            w = save
            straight(10f, coins = true)
        }

        /** A narrow bridge, maybe zig-zagging. */
        fun narrow() {
            val parts = 1 + (rnd.nextFloat() * (1f + 2f * d)).toInt()
            val bw = 1.55f - 0.3f * d
            point(s, x)
            // Line up with the bridge first, over a short approach.
            val approach = 4f
            seg(approach)
            val maxMove = reach(approach)
            val aim = c
            x += (aim - x).coerceIn(-maxMove, maxMove)
            c = x
            s += approach
            point(s, x)
            for (i in 0 until parts) {
                val len = 7f + rnd.nextFloat() * 6f
                val dx = if (parts == 1 && rnd.nextBoolean()) 0f else (rnd.nextFloat() * 2f - 1f) * min(reach(len) * 0.8f, 2.6f)
                seg(len, x1 = c + dx, ww = bw)
                c += dx
                x = c
                s += len
                point(s, x)
            }
        }

        /** A wall across the track with one hole. */
        fun wallHole() {
            val wide = max(w, 4f)
            val hole = 1.55f - 0.25f * d
            val len = 12f
            val save = w
            w = wide
            seg(len)
            w = save
            val ws = s + 8f
            val maxMove = reach(6f)
            val hx = (c + (rnd.nextFloat() * 2f - 1f) * (wide / 2f - hole / 2f - 0.1f)).let { x + (it - x).coerceIn(-maxMove, maxMove) }
            val left0 = c - wide / 2f - 0.3f
            val right1 = c + wide / 2f + 0.3f
            val lw = (hx - hole / 2f) - left0
            val rw = right1 - (hx + hole / 2f)
            if (lw > 0.1f) obs.add(Obs(ObsKind.BLOCK, left0 + lw / 2f, ws, h, lw, 0.6f, 1.4f))
            if (rw > 0.1f) obs.add(Obs(ObsKind.BLOCK, right1 - rw / 2f, ws, h, rw, 0.6f, 1.4f))
            point(s + 1.5f, x)
            x = hx
            point(ws - 1.2f, x)
            point(ws + 1.2f, x)
            s += len
            point(s, x)
        }

        /** Blocks sliding across a wide track. */
        fun sliders() {
            val wide = max(w + 0.6f, 4.6f)
            val count = 1 + (rnd.nextFloat() * (1.5f + 2f * d)).toInt()
            val spacing = 6f
            val len = spacing * (count + 1)
            val save = w
            w = wide
            seg(len)
            w = save
            var ss = s + 2f
            point(s, x)
            for (i in 0 until count) {
                ss += spacing
                val bw = 1.1f + rnd.nextFloat() * 0.6f
                val amp = wide / 2f - bw / 2f
                moving.add(Obs(ObsKind.SLIDER, c, ss, h, bw, 0.9f, 0.9f, amp, 1.4f + rnd.nextFloat() * (0.8f + 1.2f * d), rnd.nextFloat() * 6.28f))
            }
            s += len
            point(s, x)
        }

        /** Small tiles with little gaps, offset sideways: hop from stone to stone. */
        fun stones() {
            val count = 4 + (rnd.nextFloat() * (3f + 4f * d)).toInt()
            val tile = 2.6f
            val gapLen = 0.7f + 0.5f * d
            val tw = 1.7f - 0.2f * d
            point(s, x)
            for (i in 0 until count) {
                // Cross over between the end of one stone and the start of the next.
                val maxMove = if (i == 0) 0f else min(reach(gapLen + 1.6f) * 0.9f, tw * 0.55f)
                val nx = c + (rnd.nextFloat() * 2f - 1f) * 1.3f
                val cx = x + (nx - x).coerceIn(-maxMove, maxMove)
                x = cx
                point(s + 0.8f, x)
                segs.add(Seg(s, tile, cx, cx, tw, h, h, SegKind.STONE, i % 2))
                s += tile
                point(s - 0.8f, x)
                s += gapLen
            }
            c = x
            seg(5f)
            s += 5f
            point(s, x)
        }

        /** Jump pad, a big gap, land on a lower platform. */
        fun jumpPad() {
            val save = w
            w = max(w, 3.6f)
            seg(4f)
            s += 4f
            seg(2f, kind = SegKind.JUMP)
            s += 2f
            val drop = rnd.nextFloat() * 3f
            val t = (Roll.JUMP_V + sqrt(Roll.JUMP_V * Roll.JUMP_V + 2f * Roll.GRAVITY * drop)) / Roll.GRAVITY
            val dist = speed * t
            val gap = dist * (0.6f + 0.15f * d)
            point(s, x)
            s += gap
            h -= drop
            val shift = (rnd.nextFloat() * 2f - 1f) * min(reach(gap) * 0.7f, 2.5f)
            c += shift
            x += shift
            point(s + 1f, x)
            seg(9f, ww = max(w, 4f))
            s += 9f
            w = save
            point(s, x)
        }

        /** A wide pad with a bar spinning in the middle. */
        fun spinner() {
            val size = 6.4f
            val approach = 6f
            val arm = 2.5f + 0.4f * d
            point(s, x)
            seg(approach + size, ww = size)
            val cs = s + approach + size / 2f
            // Go round the bar on one side.
            val side = if (rnd.nextBoolean()) 1f else -1f
            val want = c + side * 1.9f
            x += (want - x).coerceIn(-reach(approach - 1f), reach(approach - 1f))
            point(s + approach - 1f, x)
            moving.add(Obs(ObsKind.SPINNER, c, cs, h, 0.3f, 0.3f, 0.6f, arm, (1.6f + 1.4f * d) * (if (rnd.nextBoolean()) 1f else -1f), rnd.nextFloat() * 6.28f))
            s += approach + size
            point(s, x)
        }

        /** Hammers swinging across the track. */
        fun hammers() {
            val wide = max(w, 4.2f)
            val count = 1 + (rnd.nextFloat() * (1f + 2f * d)).toInt()
            val spacing = 7f
            val len = spacing * (count + 1)
            seg(len, ww = wide)
            var ss = s + 2f
            for (i in 0 until count) {
                ss += spacing
                moving.add(Obs(ObsKind.HAMMER, c, ss, h, 1.2f, 0.9f, 1.3f, wide / 2f - 0.4f, 2.2f + 0.8f * d, rnd.nextFloat() * 6.28f))
            }
            point(s, x)
            s += len
            point(s, x)
        }

        /** Walls rising and falling across the whole track. */
        fun gates() {
            val count = 1 + (rnd.nextFloat() * (1f + d)).toInt()
            val spacing = 8f
            val len = spacing * (count + 1)
            seg(len)
            var ss = s + 2f
            for (i in 0 until count) {
                ss += spacing
                moving.add(Obs(ObsKind.GATE, c, ss, h, w + 0.6f, 0.6f, 1.3f, 0f, 2.4f + 1.2f * d, rnd.nextFloat() * 6.28f))
            }
            point(s, x)
            s += len
            point(s, x)
        }

        fun build(number: Int, boss: Boolean, endless: Boolean): RollLevel {
            segs.sortBy { it.s0 }
            // The safe line must never ask more than a person can steer: smooth out anything too sharp.
            val sArr = ps.toFloatArray()
            val xArr = px.toFloatArray()
            val base = RollLevel(number, segs, emptyList(), sArr, xArr, finishS, speed, boss, endless)
            // Moving obstacles: choose phases so the safe line is clear when the ball passes.
            val times = timeTable(base)
            val placed = ArrayList<Obs>(obs)
            for (o in moving) {
                var best: Obs? = null
                for (tryN in 0 until 24) {
                    val ph = if (tryN == 0) o.phase else rnd.nextFloat() * 6.28f
                    val cand = Obs(o.kind, o.x, o.s, o.y, o.w, o.d, o.h, o.amp, o.freq, ph)
                    if (clearFor(cand, base, times)) { best = cand; break }
                }
                if (best != null) placed.add(best)
            }
            // Coins sitting inside a block are moved out of the way (dropped).
            val final = placed.filter { o ->
                o.kind != ObsKind.COIN || placed.none { b -> b.kind == ObsKind.BLOCK && abs(b.x - o.x) < b.w / 2f + 0.4f && abs(b.s - o.s) < b.d / 2f + 0.6f }
            }.sortedBy { it.s }
            return RollLevel(number, segs, final, sArr, xArr, finishS, speed, boss, endless)
        }

        /** Does the ball on the safe line keep a margin from [o] all the way past it? */
        fun clearFor(o: Obs, lv: RollLevel, times: FloatArray): Boolean {
            val reach = if (o.kind == ObsKind.SPINNER) o.amp + 1f else max(o.d, 1f) + 1f
            var ss = o.s - reach
            while (ss <= o.s + reach) {
                val t = RollSim.timeAt(times, ss)
                // Check the ball both on time and a little early/late (people aren't exact).
                for (dt in floatArrayOf(-0.06f, 0f, 0.06f)) {
                    val gp = o.gap(lv.path(ss), ss, o.y + Roll.BALL_R, t + dt)
                    if (gp < 0.25f) return false
                }
                ss += 0.2f
            }
            return true
        }
    }
}

/**
 * A careful player: steers towards the safe line a little ahead, no faster than the ball can move.
 * Used to check every level before it's played.
 */
object RollBot {
    fun beats(level: RollLevel): Boolean {
        val sim = RollSim(level)
        sim.started = true
        val ev = ArrayList<RollEvent>()
        val dt = 1f / 120f
        var t = 0f
        while (!sim.won && !sim.dead && t < 400f) {
            steer(sim)
            ev.clear()
            sim.step(dt, ev)
            t += dt
        }
        return sim.won
    }

    fun steer(sim: RollSim) {
        sim.targetX = sim.level.path(sim.s + sim.vz * 0.08f)
    }
}
