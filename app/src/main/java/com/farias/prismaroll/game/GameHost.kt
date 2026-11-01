package com.farias.prismaroll.game

import android.content.Context
import android.opengl.GLSurfaceView
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.farias.prismaroll.R
import com.farias.prismaroll.audio.Haptic
import com.farias.prismaroll.audio.Haptics
import com.farias.prismaroll.audio.Sfx
import com.farias.prismaroll.audio.SoundManager
import com.farias.prismaroll.data.MissionType
import com.farias.prismaroll.data.Progress
import com.farias.prismaroll.gl.Looks
import java.util.Calendar
import java.util.TimeZone

/** A floating text over the 3D view ("+3", "PERFEITO!"), at a screen position 0..1. */
class Popup(val id: Long, val text: String, val x: Float, val y: Float, val kind: Int, val born: Long)

/**
 * Owns the [RollGame] (which lives on the GL thread) and mirrors what the UI needs as Compose state
 * on the main thread. Commands go to the GL thread through [GLSurfaceView.queueEvent].
 */
class GameHost(context: Context) : RollGame.Callbacks {

    enum class Phase { MENU, PLAYING, DEAD, WON }

    private val app = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val sound = SoundManager.get(app)
    val game = RollGame(this)
    var view: GLSurfaceView? = null

    var phase by mutableStateOf(Phase.MENU)
        private set
    var level by mutableIntStateOf(1)
        private set
    var daily by mutableStateOf(false)
        private set
    var progress by mutableFloatStateOf(0f)
        private set
    /** Score of the whole run (kept while the player keeps winning). */
    var score by mutableIntStateOf(0)
        private set
    private var runBase = 0
    /** Near misses in a row (resets on a calm stretch is not tracked: per level). */
    var combo by mutableIntStateOf(0)
        private set
    var boosting by mutableStateOf(false)
        private set
    var touched by mutableStateOf(false)
        private set
    var paused by mutableStateOf(false)
        private set
    /** Rewarded "continue" already used on this level. */
    var revived by mutableStateOf(false)
        private set
    /** Waiting for a touch to roll again after a revive. */
    var waiting by mutableStateOf(false)
        private set
    var newBest by mutableStateOf(false)
        private set
    /** Coins given for the last win (before any video bonus). */
    var lastReward by mutableIntStateOf(0)
        private set
    /** Progress (0..1) reached when the ball died. */
    var deathProgress by mutableFloatStateOf(0f)
        private set
    val popups = mutableStateListOf<Popup>()

    /** Endless mode run. */
    var endless by mutableStateOf(false)
        private set
    /** This level is a boss track. */
    var boss by mutableStateOf(false)
        private set
    /** Metres rolled in this level (the endless distance). */
    var depth by mutableIntStateOf(0)
        private set
    var shields by mutableIntStateOf(0)
        private set
    var coinsThisLevel by mutableIntStateOf(0)
        private set
    /** Coins in the whole level (for the "all coins" star). */
    var coinsTotal by mutableIntStateOf(0)
        private set
    /** String resource of a mechanic shown for the first time in this level (0 = none). */
    var notice by mutableIntStateOf(0)
        private set
    /** Deaths on the current level number (to offer the shield). */
    var failsHere by mutableIntStateOf(0)
        private set
    private var failsLevel = -1
    private var nearCount = 0
    private var jumps = 0
    private var boosts = 0
    private var popupId = 0L
    private var lastMetres = 0

    // ------------------------------------------------------------------ commands

    private fun gl(block: () -> Unit) {
        val v = view
        if (v != null) v.queueEvent(block) else block()
    }

    fun applyLooks(
        towerId: String = if (Progress.trialGames > 0 && Progress.trialTower.isNotEmpty()) Progress.trialTower else Progress.towerId,
        ballId: String = if (Progress.trialGames > 0 && Progress.trialBall.isNotEmpty()) Progress.trialBall else Progress.ballId,
        trailId: String = Progress.trailId,
    ) {
        val t = Looks.tower(towerId)
        val b = Looks.ball(ballId)
        val trail = com.farias.prismaroll.data.Trails.byId(trailId).style
        com.farias.prismaroll.audio.MusicPlayer.select(
            com.farias.prismaroll.audio.MusicPlayer.trackFor(towerId, boss && phase == Phase.PLAYING),
        )
        gl { game.tower = t; game.ball = b; game.trailStyle = trail }
    }

    fun showMenu() {
        phase = Phase.MENU
        paused = false
        popups.clear()
        applyLooks()
        gl { game.startAttract() }
    }

    fun play(n: Int, keepScore: Boolean = false) {
        daily = false
        endless = false
        level = n
        start(LevelCache.get(n), keepScore)
        LevelCache.prefetch(n + 1)
    }

    fun playDaily() {
        daily = true
        endless = false
        level = dayIndex()
        start(LevelCache.daily(dayIndex()), false)
        LevelCache.prefetch(Progress.level)
    }

    fun playEndless() {
        daily = false
        endless = true
        level = 0
        start(LevelCache.endless(), false)
    }

    fun retry() {
        when {
            endless -> playEndless()
            daily -> playDaily()
            else -> play(level)
        }
    }

    /** One extra shield for this level (rewarded video). */
    fun addShield() {
        shields += 1
        sound.play(Sfx.REVIVE, 0.8f)
        gl { game.sim.shields += 1 }
    }

    fun clearNotice() {
        notice = 0
    }

    fun next() = play(Progress.level, keepScore = true)

    private fun start(lv: RollLevel, keepScore: Boolean) {
        Progress.ensureMissionDay()
        runBase = if (keepScore) score else 0
        score = runBase
        progress = 0f
        combo = 0
        boosting = false
        touched = false
        paused = false
        revived = false
        waiting = false
        newBest = false
        nearCount = 0
        jumps = 0
        boosts = 0
        lastMetres = 0
        popups.clear()
        depth = 0
        endlessPaid = 0
        shields = 0
        coinsThisLevel = 0
        coinsTotal = lv.coins
        boss = lv.boss
        val key = if (endless) -1 else if (daily) -2 else level
        if (key != failsLevel) { failsLevel = key; failsHere = 0 }
        val mech = lv.mechanics
        fun first(k: String, res: Int): Boolean {
            if (k !in mech || k in Progress.seenMechanics) return false
            Progress.markMechanicSeen(k)
            notice = res
            return true
        }
        notice = 0
        first("bank", R.string.mech_bank) || first("boost", R.string.mech_boost) || first("jump", R.string.mech_jump) || first("slider", R.string.mech_slider) ||
            first("stones", R.string.mech_stones) || first("spinner", R.string.mech_spinner) || first("hammer", R.string.mech_hammer) ||
            first("gate", R.string.mech_gate)
        phase = Phase.PLAYING
        applyLooks()
        Progress.useTrialGame()
        gl { game.startPlay(lv) }
    }

    fun revive() {
        if (phase != Phase.DEAD) return
        revived = true
        waiting = true
        phase = Phase.PLAYING
        sound.play(Sfx.REVIVE)
        gl { game.revive() }
    }

    /** Stops drawing the 3D view while a screen covers it completely (the splash). */
    var renderPaused = false
        set(value) {
            field = value
            applyRenderMode()
        }

    fun applyRenderMode() {
        view?.renderMode = if (renderPaused) GLSurfaceView.RENDERMODE_WHEN_DIRTY else GLSurfaceView.RENDERMODE_CONTINUOUSLY
    }

    /** The finger moved [dx] world units sideways (0 = just put down). Starts the roll. */
    fun steer(dx: Float) {
        if (phase != Phase.PLAYING || paused) return
        if (!touched) touched = true
        if (waiting) waiting = false
        game.steer(dx) // thread-safe
    }

    fun pauseGame(p: Boolean) {
        if (phase != Phase.PLAYING && p) return
        paused = p
        gl { game.paused = p }
    }

    // ------------------------------------------------------------------ callbacks (GL thread)

    override fun onEvent(e: RollEvent, game: RollGame) {
        if (game.mode != RollGame.Mode.PLAY) return
        val sim = game.sim
        val sc = sim.score
        val prog = sim.progress
        val x = game.ballScreenX
        val y = game.ballScreenY
        val boost = sim.boosted
        val m = sim.s.toInt()
        main.post { handle(e, sc, prog, x, y, boost, m) }
    }

    /** Called from the GL thread now and then, so the HUD's progress bar moves between events. */
    override fun onTick(game: RollGame) {
        if (game.mode != RollGame.Mode.PLAY) return
        val prog = game.sim.progress
        val m = game.sim.s.toInt()
        val boost = game.sim.boosted
        main.post {
            if (phase == Phase.PLAYING) {
                progress = prog
                depth = m
                boosting = boost
            }
        }
    }

    private fun handle(e: RollEvent, simScore: Int, prog: Float, x: Float, y: Float, boost: Boolean, metres: Int) {
        if (phase != Phase.PLAYING) return
        score = runBase + simScore
        progress = prog
        boosting = boost
        depth = metres
        when (e) {
            is RollEvent.Coin -> {
                coinsThisLevel++
                Progress.addCoins(COIN_VALUE)
                sound.play(Sfx.COIN, 0.6f, 1.15f + 0.1f * (coinsThisLevel % 5))
                popup("+$COIN_VALUE", x, y - 0.05f, 2)
            }
            is RollEvent.Jump -> {
                jumps++
                sound.play(Sfx.WHOOSH, if (e.big) 1f else 0.6f, if (e.big) 1.2f else 1.5f)
                haptic(Haptic.TICK)
            }
            is RollEvent.Land -> {
                if (e.impact > 5f) {
                    sound.play(Sfx.BOUNCE, (e.impact / 20f).coerceIn(0.3f, 0.9f), 0.85f)
                    haptic(Haptic.PLACE)
                }
            }
            RollEvent.Boost -> {
                boosts++
                sound.play(Sfx.COMBO, 0.7f, 1.3f)
                haptic(Haptic.COMBO)
                popup("⚡", x, y - 0.08f, 4)
            }
            is RollEvent.Near -> {
                nearCount++
                combo++
                sound.play(Sfx.PASS, 0.7f, (0.9f + 0.08f * combo).coerceAtMost(1.8f))
                haptic(Haptic.TICK)
                popup(app.getString(R.string.near_miss), x, y - 0.06f, 1)
            }
            is RollEvent.Shield -> {
                shields = (shields - 1).coerceAtLeast(0)
                sound.play(Sfx.GLASS, 1f, 0.9f)
                haptic(Haptic.HEAVY)
                popup("🛡", x, y - 0.06f, 4)
            }
            is RollEvent.Crash, RollEvent.Fall -> {
                deathProgress = prog
                phase = Phase.DEAD
                sound.play(if (e is RollEvent.Crash) Sfx.GLASS else Sfx.WHOOSH, 1f, if (e is RollEvent.Crash) 0.7f else 0.6f)
                sound.play(Sfx.GAME_OVER, 0.8f)
                haptic(Haptic.HEAVY)
                finishStats(won = false)
            }
            RollEvent.Win -> {
                phase = Phase.WON
                sound.play(Sfx.RECORD)
                haptic(Haptic.COMBO)
                finishStats(won = true)
            }
        }
    }

    private fun finishStats(won: Boolean) {
        Progress.addStats(
            bosses = if (won && boss) 1 else 0,
            perfect = if (won && !endless && coinsTotal > 0 && coinsThisLevel >= coinsTotal) 1 else 0,
            coins = coinsThisLevel,
            glass = nearCount,
            portals = boosts,
        )
        coinsThisLevel = 0
        if (!won) failsHere += 1
        // "Rings" are metres rolled now; "smashed" are jumps; the best combo is near misses in one level.
        val metres = (depth - lastMetres).coerceAtLeast(0)
        lastMetres = depth
        Progress.onRings(metres, jumps, nearCount)
        Progress.missionEvent(MissionType.RINGS, metres)
        Progress.missionEvent(MissionType.SMASH, jumps)
        Progress.missionEvent(MissionType.SCORE, score, asMax = true)
        jumps = 0
        boosts = 0
        nearCount = 0
        newBest = Progress.onScore(score)
        if (endless) {
            newBest = Progress.onEndless(depth) || newBest
            // Coins for the distance (only the part not paid yet if the run was continued).
            val total = depth / 15 + if (won) 500 else 0
            lastReward = (total - endlessPaid).coerceAtLeast(0)
            endlessPaid = total
            Progress.addCoins(lastReward)
            return
        }
        if (won) {
            Progress.onLevelWon(level, daily)
            Progress.missionEvent(MissionType.LEVELS, 1)
            Progress.missionEvent(MissionType.STREAK, Progress.winStreak, asMax = true)
            lastReward = if (daily) Progress.onDailyWon()
            else (Progress.levelReward(level) * (if (boss) 3 else 1)).also { Progress.addCoins(it) }
        } else {
            lastReward = 0
        }
    }

    private var endlessPaid = 0

    /** Ends a lost attempt for good (the player gave up the "continue"). */
    fun giveUp() {
        if (phase == Phase.DEAD) Progress.onLevelLost()
    }

    private fun popup(text: String, x: Float, y: Float, kind: Int) {
        val now = SystemClock.uptimeMillis()
        popups.removeAll { now - it.born > 1200 }
        if (popups.size > 8) popups.removeAt(0)
        popups.add(Popup(popupId++, text, x, y, kind, now))
    }

    private fun haptic(kind: Haptic) {
        if (Progress.vibrationOn) Haptics.play(app, kind)
    }

    companion object {
        const val COIN_VALUE = 2

        /** Local calendar day number (same challenge for everyone on the same date). */
        fun dayIndex(): Int {
            val c = Calendar.getInstance()
            val offset = TimeZone.getDefault().getOffset(c.timeInMillis)
            return ((c.timeInMillis + offset) / 86_400_000L).toInt()
        }
    }
}
