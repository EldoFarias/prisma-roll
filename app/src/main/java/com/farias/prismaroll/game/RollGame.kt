package com.farias.prismaroll.game

import com.farias.prismaroll.gl.Ambient
import com.farias.prismaroll.gl.BallLook
import com.farias.prismaroll.gl.Looks
import com.farias.prismaroll.gl.TowerLook
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Everything the 3D view shows, updated on the GL thread every frame: the simulation, the camera and
 * all effects. Commands from the UI arrive through [GameHost] (queued on the GL thread).
 */
class RollGame(private val callbacks: Callbacks) {

    interface Callbacks {
        /** Called on the GL thread; implementations hop to the main thread. */
        fun onEvent(e: RollEvent, game: RollGame)
        /** About ten times a second while playing (progress bar, distance). */
        fun onTick(game: RollGame)
    }

    enum class Mode { ATTRACT, PLAY }

    var mode = Mode.ATTRACT
        private set
    var sim = RollSim(LevelCache.get(3))
        private set
    /** Planned time at each distance (for the revive). */
    private var times = RollLevels.timeTable(sim.level)
    /** The track theme (sky, colours, scenery). */
    var tower: TowerLook = Looks.towers[0]
    var ball: BallLook = Looks.balls[0]
    /** Trail style ([com.farias.prismaroll.data.Trails]). */
    var trailStyle = 0
    var paused = false

    /** Seconds since the game object started (drives shader animations). */
    var clock = 0f
        private set

    // Camera (world units; the renderer turns s into -z).
    var camX = 0f
        private set
    var camY = 2f
        private set
    var camS = 0f
        private set
    var shake = 0f
        private set
    /** Camera lean (radians) on tilted track. */
    var camRoll = 0f
        private set
    /** Extra field of view while boosting (0..1). */
    var boostFx = 0f
        private set
    var deathTime = -1f
        private set
    var winTime = -1f
        private set
    var sinceLand = 9f
        private set
    /** Obstacle that broke (shield) and when, for the renderer. */
    var brokeObs = -1
        private set
    var brokeAt = -10f
        private set

    val particles = Particles(1600)
    /** Recent ball positions for the comet trail: ring buffer, oldest first via [trailX]/[trailY]/[trailS]. */
    private val tx = FloatArray(TRAIL); private val ty = FloatArray(TRAIL); private val ts = FloatArray(TRAIL)
    var trailCount = 0
        private set
    private var trailHead = 0
    private fun ti(i: Int) = (trailHead - trailCount + i + TRAIL * 2) % TRAIL
    fun trailX(i: Int) = tx[ti(i)]
    fun trailY(i: Int) = ty[ti(i)]
    fun trailS(i: Int) = ts[ti(i)]

    /** Ball squash & stretch, updated every frame. */
    var ballSy = 1f
        private set
    var ballSxz = 1f
        private set

    /** Ball position on screen (0..1), for floating texts. */
    @Volatile var ballScreenX = 0.5f
    @Volatile var ballScreenY = 0.5f

    private val events = ArrayList<RollEvent>()
    private val rnd = Random(System.nanoTime())
    private val inputLock = Any()
    private var pendingSteer = 0f
    private var pendingStart = false
    /** The player has touched the screen at least once this level. */
    var touched = false
        private set

    // ------------------------------------------------------------------ commands (GL thread)

    fun startAttract() {
        mode = Mode.ATTRACT
        // Use a level that is already built (never stall the menu), and get the next ones ready.
        val pick = 8 + rnd.nextInt(40)
        val lv = LevelCache.ready(pick) ?: (8..48).firstNotNullOfOrNull { LevelCache.ready(it) } ?: LevelCache.get(8)
        begin(lv)
        sim.started = true
        LevelCache.prefetch(8 + rnd.nextInt(40))
    }

    fun startPlay(level: RollLevel, shields: Int = 0) {
        mode = Mode.PLAY
        begin(level)
        sim.shields = shields
    }

    /** Slow motion after a near miss: until this clock time, the game runs at [SLOW_SCALE]. */
    private var slowUntil = -1f
    private var lastSlow = -10f
    /** 0..1, how much slow motion is on (the renderer can use it for a vignette). */
    var slowMo = 0f
        private set

    private fun nearMiss() {
        if (mode != Mode.PLAY || clock - lastSlow < 3f) return
        lastSlow = clock
        slowUntil = clock + 0.45f
    }

    private fun begin(level: RollLevel) {
        sim = RollSim(level)
        times = RollLevels.timeTable(level)
        camX = sim.x * 0.6f
        camY = sim.y
        camS = sim.s
        particles.clear()
        trailCount = 0
        deathTime = -1f
        winTime = -1f
        sinceLand = 9f
        brokeObs = -1
        touched = false
        paused = false
        synchronized(inputLock) { pendingSteer = 0f; pendingStart = false }
    }

    fun revive() {
        if (!sim.dead) return
        sim.revive(times)
        deathTime = -1f
        trailCount = 0
        camY = sim.y
    }

    /** UI thread: the finger moved [dx] world units sideways (0 = just touched). */
    fun steer(dx: Float) {
        synchronized(inputLock) { pendingSteer += dx; pendingStart = true }
    }

    // ------------------------------------------------------------------ frame

    fun update(dtRaw: Float) {
        val real = dtRaw.coerceIn(0f, 1f / 20f)
        clock += real
        val slowTarget = if (clock < slowUntil) 1f else 0f
        slowMo += (slowTarget - slowMo) * min(1f, real * 14f)
        val dt = real * (1f - slowMo * (1f - SLOW_SCALE))
        val steer: Float
        val start: Boolean
        synchronized(inputLock) { steer = pendingSteer; start = pendingStart; pendingSteer = 0f; pendingStart = false }
        if (paused) return
        if (mode == Mode.PLAY) {
            if (start) {
                touched = true
                if (!sim.dead) sim.started = true
            }
            if (!sim.dead && !sim.won) sim.targetX = (sim.targetX + steer).coerceIn(sim.x - 6f, sim.x + 6f)
        }
        // Fixed sub-steps keep the physics identical at any frame rate.
        var left = dt
        while (left > 1e-5f) {
            val h = min(left, 1f / 120f)
            if (mode == Mode.ATTRACT) RollBot.steer(sim)
            events.clear()
            sim.step(h, events)
            handle(events)
            left -= h
        }
        sinceLand += dt
        if (mode == Mode.ATTRACT && (sim.won && clock - winTime > 2.5f || sim.dead && clock - deathTime > 1.4f)) startAttract()

        // Camera: behind the ball, a little to its side, smoothly; stops following a ball falling to its doom.
        val k = min(1f, dt * 6f)
        camS += (sim.s - camS) * min(1f, dt * 14f)
        camX += (sim.x * 0.65f - camX) * k
        val falling = !sim.grounded && sim.y < sim.lastGroundY - 1.5f
        val wantY = if (falling) camY else sim.y
        camY += (wantY - camY) * min(1f, dt * (if (sim.grounded) 5f else 2.5f))
        shake *= exp(-dt * 9f)
        val under = if (sim.grounded) sim.level.support(sim.s, sim.x) else null
        val wantRoll = if (under != null) -under.bank * 0.9f else camRoll * 0.98f
        camRoll += (wantRoll - camRoll) * min(1f, dt * 3f)
        boostFx += ((if (sim.boosted && !sim.dead) 1f else 0f) - boostFx) * min(1f, dt * 4f)

        // Trail
        if (sim.started && !sim.dead) {
            tx[trailHead] = sim.x; ty[trailHead] = sim.y; ts[trailHead] = sim.s
            trailHead = (trailHead + 1) % TRAIL
            if (trailCount < TRAIL) trailCount++
        }
        updateBallScale()
        if (!sim.dead && sim.started) {
            emitTrail()
            if (sim.boosted) emitBoost()
        }
        emitAmbient(dt)
        particles.update(dt)
        tickAcc += real
        if (tickAcc > 0.1f) {
            tickAcc = 0f
            callbacks.onTick(this)
        }
    }

    private var tickAcc = 0f

    private fun handle(list: List<RollEvent>) {
        for (e in list) {
            when (e) {
                is RollEvent.Coin -> {
                    val o = sim.level.obs[e.index]
                    burst(o.x, o.y, o.s, COIN_C, 16, 3.5f, 0.1f)
                }
                is RollEvent.Jump -> {
                    burst(sim.x, sim.y - Roll.BALL_R, sim.s, ball.trail, if (e.big) 26 else 10, if (e.big) 5f else 3f, 0.1f)
                }
                is RollEvent.Land -> {
                    sinceLand = 0f
                    if (e.impact > 4f) {
                        shake = maxOf(shake, (e.impact / 40f).coerceAtMost(0.3f))
                        dust(e.impact)
                    }
                }
                RollEvent.Boost -> burst(sim.x, sim.y, sim.s, BOOST_C, 30, 5f, 0.12f)
                is RollEvent.Near -> nearMiss()
                is RollEvent.Crash -> {
                    deathTime = clock
                    shake = 0.55f
                    shatter()
                }
                is RollEvent.Shield -> {
                    brokeObs = e.obs
                    brokeAt = clock
                    shake = 0.35f
                    val o = sim.level.obs[e.obs]
                    burst(o.cx(sim.time), o.y + 0.5f, o.s, tower.danger, 50, 7f, 0.16f)
                    burst(sim.x, sim.y, sim.s, SHIELD_C, 30, 5f, 0.12f)
                }
                RollEvent.Fall -> deathTime = clock
                RollEvent.Win -> {
                    winTime = clock
                    confetti()
                }
            }
            callbacks.onEvent(e, this)
        }
    }

    // ------------------------------------------------------------------ particles

    private fun burst(x: Float, y: Float, s: Float, c: FloatArray, n: Int, speed: Float, size: Float) {
        repeat(n) {
            val a = rnd.nextFloat() * 6.283f
            val up = 0.2f + rnd.nextFloat()
            particles.add(
                x, y, -s,
                cos(a) * speed * rnd.nextFloat(), up * speed, sin(a) * speed * rnd.nextFloat(),
                c[0], c[1], c[2], 1f,
                size + rnd.nextFloat() * size, 0.35f + rnd.nextFloat() * 0.4f, gravity = 18f,
            )
        }
    }

    private fun dust(impact: Float) {
        val n = (impact * 2.5f).toInt().coerceIn(8, 30)
        val c = tower.ringA
        repeat(n) {
            val a = rnd.nextFloat() * 6.283f
            val sp = 1.5f + rnd.nextFloat() * 3f
            particles.add(
                sim.x + cos(a) * 0.3f, sim.y - Roll.BALL_R + 0.05f, -sim.s + sin(a) * 0.3f,
                cos(a) * sp, 0.6f + rnd.nextFloat(), sin(a) * sp,
                0.6f + 0.4f * c[0], 0.6f + 0.4f * c[1], 0.6f + 0.4f * c[2], 0.8f,
                0.08f + rnd.nextFloat() * 0.08f, 0.35f + rnd.nextFloat() * 0.3f, gravity = 4f,
            )
        }
    }

    /** The ball bursts into shards of its own colours. */
    private fun shatter() {
        val c1 = ball.c1; val c2 = ball.c2; val tr = ball.trail
        repeat(90) {
            val a = rnd.nextFloat() * 6.283f
            val sp = 3f + rnd.nextFloat() * 8f
            val c = when (rnd.nextInt(3)) { 0 -> c1; 1 -> c2; else -> tr }
            particles.add(
                sim.x, sim.y, -sim.s,
                cos(a) * sp, 2f + rnd.nextFloat() * 7f, sin(a) * sp - sim.vz * 0.25f,
                c[0], c[1], c[2], 1f,
                0.1f + rnd.nextFloat() * 0.16f, 0.6f + rnd.nextFloat() * 0.6f, gravity = 22f,
            )
        }
    }

    private fun emitBoost() {
        repeat(2) {
            particles.add(
                sim.x + (rnd.nextFloat() - 0.5f) * 2.4f, sim.y + rnd.nextFloat() * 1.2f - 0.3f, -sim.s - 2f - rnd.nextFloat() * 4f,
                0f, 0f, sim.vz * 1.1f,
                BOOST_C[0], BOOST_C[1], BOOST_C[2], 0.8f,
                0.04f + rnd.nextFloat() * 0.03f, 0.35f, gravity = 0f,
            )
        }
    }

    private var trailAcc = 0f

    /** Extra particles for the fancy trails (stars, blue fire, gold dust, rainbow). */
    private fun emitTrail() {
        val rate = when (trailStyle) {
            com.farias.prismaroll.data.Trails.STARS -> 30f
            com.farias.prismaroll.data.Trails.BLUE_FIRE -> 70f
            com.farias.prismaroll.data.Trails.GOLD -> 40f
            com.farias.prismaroll.data.Trails.RAINBOW -> 25f
            else -> 0f
        }
        if (rate == 0f) return
        trailAcc += rate / 60f
        val bx = sim.x; val by = sim.y; val bz = -sim.s
        while (trailAcc >= 1f) {
            trailAcc -= 1f
            val a = rnd.nextFloat() * 6.283f
            val rr = Roll.BALL_R * 0.8f * rnd.nextFloat()
            when (trailStyle) {
                com.farias.prismaroll.data.Trails.STARS -> particles.add(
                    bx + rr * cos(a), by + rr * sin(a), bz + 0.3f, (rnd.nextFloat() - 0.5f) * 1.5f, 1f, 1.5f,
                    1f, 1f, 0.85f + 0.15f * rnd.nextFloat(), 1f, 0.06f + rnd.nextFloat() * 0.06f, 0.6f + rnd.nextFloat() * 0.4f, gravity = 2f,
                )
                com.farias.prismaroll.data.Trails.BLUE_FIRE -> {
                    val hot = rnd.nextFloat()
                    particles.add(
                        bx + rr * cos(a), by + rr * sin(a), bz + 0.2f,
                        (rnd.nextFloat() - 0.5f) * 1.2f, 1.5f + rnd.nextFloat() * 2f, 2f,
                        0.15f + 0.4f * hot, 0.5f + 0.4f * hot, 1f, 1f, 0.16f + rnd.nextFloat() * 0.12f, 0.25f + rnd.nextFloat() * 0.2f, gravity = -3f,
                    )
                }
                com.farias.prismaroll.data.Trails.GOLD -> particles.add(
                    bx + rr * cos(a), by + rr * sin(a), bz + 0.3f, (rnd.nextFloat() - 0.5f) * 0.8f, 0.5f, 0.8f,
                    1f, 0.78f + 0.2f * rnd.nextFloat(), 0.25f, 1f, 0.05f + rnd.nextFloat() * 0.05f, 0.8f, gravity = 3f,
                )
                else -> {
                    val h = (clock * 0.5f + rnd.nextFloat() * 0.2f) % 1f
                    particles.add(
                        bx + rr * cos(a), by + rr * sin(a), bz + 0.3f, (rnd.nextFloat() - 0.5f), 0.6f, 0.8f,
                        hueR(h), hueG(h), hueB(h), 1f, 0.07f + rnd.nextFloat() * 0.05f, 0.6f, gravity = 2f,
                    )
                }
            }
        }
    }

    private fun hueC(h: Float, off: Float) = (abs(((h * 6f + off) % 6f) - 3f) - 1f).coerceIn(0f, 1f)
    fun hueR(h: Float) = hueC(h, 0f)
    fun hueG(h: Float) = hueC(h, 4f)
    fun hueB(h: Float) = hueC(h, 2f)

    private var ambientAcc = 0f
    /** Air particles (part of the detailed scenery). */
    var ambientOn = true

    /** Snow, petals, embers, fireflies or sparkles drifting in the air, by theme. */
    private fun emitAmbient(dt: Float) {
        val kind = tower.env.ambient
        if (kind == Ambient.NONE || !ambientOn) return
        val rate = when (kind) { Ambient.SNOW -> 30f; Ambient.PETALS -> 10f; Ambient.EMBERS -> 16f; Ambient.FIREFLIES -> 7f; else -> 9f }
        ambientAcc += dt * rate
        val c = tower.env.ambientColor
        while (ambientAcc >= 1f) {
            ambientAcc -= 1f
            val x = camX + (rnd.nextFloat() - 0.5f) * 22f
            val z = -camS - 4f - rnd.nextFloat() * 30f
            val y = camY + (rnd.nextFloat() - 0.3f) * 10f
            when (kind) {
                Ambient.SNOW -> particles.add(x, camY + 7f, z, 0.3f, -1.6f - rnd.nextFloat(), 0f, c[0], c[1], c[2], 0.55f,
                    0.05f + rnd.nextFloat() * 0.06f, 6f, gravity = 0f)
                Ambient.PETALS -> particles.add(x, camY + 7f, z, 0.9f + rnd.nextFloat(), -1.2f, 0.3f, c[0], c[1], c[2], 0.7f,
                    0.09f + rnd.nextFloat() * 0.06f, 7f, gravity = 0f)
                Ambient.EMBERS -> particles.add(x, camY - 8f, z, (rnd.nextFloat() - 0.5f) * 0.6f, 2.5f + rnd.nextFloat() * 2f, 0f,
                    c[0], c[1] * (0.6f + 0.4f * rnd.nextFloat()), c[2], 0.9f, 0.05f + rnd.nextFloat() * 0.07f, 6f, gravity = 0f)
                Ambient.FIREFLIES -> particles.add(x, y, z, (rnd.nextFloat() - 0.5f) * 0.8f,
                    (rnd.nextFloat() - 0.5f) * 0.6f, 0f, c[0], c[1], c[2], 0.9f, 0.07f, 3f + rnd.nextFloat() * 2f, gravity = 0f)
                else -> particles.add(x, y, z, 0f, 0.3f, 0f, c[0], c[1], c[2], 0.8f,
                    0.04f + rnd.nextFloat() * 0.05f, 1.6f + rnd.nextFloat() * 1.5f, gravity = 0f)
            }
        }
    }

    private fun confetti() {
        val cols = listOf(tower.ringA, tower.ringB, ball.trail, COIN_C, floatArrayOf(1f, 1f, 1f))
        repeat(170) {
            val a = rnd.nextFloat() * 6.283f
            val sp = 3f + rnd.nextFloat() * 8f
            val c = cols[rnd.nextInt(cols.size)]
            particles.add(
                sim.x + (rnd.nextFloat() - 0.5f) * 4f, sim.y + 0.5f, -sim.s - 3f,
                cos(a) * sp, 6f + rnd.nextFloat() * 9f, sin(a) * sp * 0.6f - 2f,
                c[0], c[1], c[2], 1f,
                0.12f + rnd.nextFloat() * 0.14f, 1.2f + rnd.nextFloat() * 0.8f, gravity = 14f,
            )
        }
    }

    private fun updateBallScale() {
        val t = sinceLand
        val squash = 0.3f * exp(-t * 12f) * cos(t * 26f)
        val stretch = if (sim.grounded) 0f else (abs(sim.vy) / 18f).coerceIn(0f, 1f) * 0.14f
        ballSy = 1f - squash + stretch
        ballSxz = 1f + squash * 0.6f - stretch * 0.45f
    }

    companion object {
        const val SLOW_SCALE = 0.32f
        const val TRAIL = 18
        val TWO_PI = (2 * PI).toFloat()
        val COIN_C = floatArrayOf(1f, 0.82f, 0.25f)
        val BOOST_C = floatArrayOf(0.4f, 0.95f, 1f)
        val SHIELD_C = floatArrayOf(0.35f, 0.75f, 1f)
    }
}

/** A pool of glowing particles (positions, velocities, colours) updated on the CPU. */
class Particles(val max: Int) {
    val px = FloatArray(max); val py = FloatArray(max); val pz = FloatArray(max)
    val vx = FloatArray(max); val vy = FloatArray(max); val vz = FloatArray(max)
    val r = FloatArray(max); val g = FloatArray(max); val b = FloatArray(max); val a = FloatArray(max)
    val size = FloatArray(max); val life = FloatArray(max); val maxLife = FloatArray(max); val grav = FloatArray(max)
    var count = 0
        private set

    fun clear() { count = 0 }

    fun add(
        x: Float, y: Float, z: Float, vx0: Float, vy0: Float, vz0: Float,
        cr: Float, cg: Float, cb: Float, ca: Float, s: Float, l: Float, gravity: Float,
    ) {
        val i = if (count < max) count++ else kotlin.random.Random.nextInt(max)
        px[i] = x; py[i] = y; pz[i] = z
        vx[i] = vx0; vy[i] = vy0; vz[i] = vz0
        r[i] = cr; g[i] = cg; b[i] = cb; a[i] = ca
        size[i] = s; life[i] = l; maxLife[i] = l; grav[i] = gravity
    }

    fun update(dt: Float) {
        var i = 0
        val drag = exp(-dt * 1.8f)
        while (i < count) {
            life[i] -= dt
            if (life[i] <= 0f) {
                val last = count - 1
                if (i != last) copy(last, i)
                count--
                continue
            }
            vy[i] -= grav[i] * dt
            vx[i] *= drag; vz[i] *= drag
            px[i] += vx[i] * dt; py[i] += vy[i] * dt; pz[i] += vz[i] * dt
            i++
        }
    }

    private fun copy(from: Int, to: Int) {
        px[to] = px[from]; py[to] = py[from]; pz[to] = pz[from]
        vx[to] = vx[from]; vy[to] = vy[from]; vz[to] = vz[from]
        r[to] = r[from]; g[to] = g[from]; b[to] = b[from]; a[to] = a[from]
        size[to] = size[from]; life[to] = life[from]; maxLife[to] = maxLife[from]; grav[to] = grav[from]
    }
}
