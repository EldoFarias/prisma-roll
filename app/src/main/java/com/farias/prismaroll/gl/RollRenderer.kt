package com.farias.prismaroll.gl

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.farias.prismaroll.game.ObsKind
import com.farias.prismaroll.game.Roll
import com.farias.prismaroll.game.RollGame
import com.farias.prismaroll.game.SegKind
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/** Draws a [RollGame] with OpenGL ES 3.0 and advances it every frame. */
class RollRenderer(private val game: RollGame) : GLSurfaceView.Renderer {

    companion object {
        /** The sky is rendered at 1/BG_DIV of the screen size. */
        const val BG_DIV = 3
        /** Boxes (track pieces, obstacles, posts) drawn per frame at most. */
        const val MAX_BOXES = 520
        /** How far ahead and behind the ball the track is drawn. */
        const val AHEAD = 85f
        const val BEHIND = 12f

        // Box kinds understood by the track shader.
        const val K_TRACK = 0f
        const val K_BOOST = 10f
        const val K_JUMP = 11f
        const val K_FINISH = 12f
        const val K_STONE = 13f
        const val K_DANGER = 20f
        const val K_METAL = 21f
        const val K_GATE = 22f
        const val K_COLUMN = 23f
    }

    private lateinit var lit: Program
    private lateinit var litBoxes: Program
    private lateinit var litParts: Program
    private lateinit var litBall: Program
    private lateinit var shadow: Program
    private lateinit var bg: Program
    private lateinit var pts: Program
    private lateinit var cloudProg: Program
    private lateinit var blit: Program
    private lateinit var sky: Program

    /** Read every frame: detailed scenery on/off. */
    var detailed: () -> Boolean = { true }
    /** Called (GL thread) when the phone can't keep up with the detailed scenery. */
    var onSlow: (() -> Unit)? = null
    private var slowTime = 0f
    private var watchTime = 0f
    // The sky is drawn into a small texture and stretched: soft, and full resolution was far too heavy.
    private val bgFbo = IntArray(1)
    private val bgTex = IntArray(1)
    private var bgW = 0
    private var bgH = 0
    private var bgFrame = 0
    private var bgValid = false
    private lateinit var box: Mesh
    private lateinit var sphere: Mesh
    private lateinit var disc: Mesh
    private lateinit var screen: Mesh

    private val proj = FloatArray(16)
    private val view = FloatArray(16)
    private val vp = FloatArray(16)
    private val model = FloatArray(16)
    private val vec = FloatArray(4)
    private val out4 = FloatArray(4)
    private val eye = FloatArray(3)

    private var width = 1
    private var height = 1
    private var aspect = 0.5f
    private var lastNanos = 0L

    // Particle stream: pos(3) colour(4) size(1)
    private val ptVao = IntArray(1)
    private val ptVbo = IntArray(1)
    private lateinit var ptBuf: FloatBuffer
    private val ptCapacity = 2200
    private val ptScratch = FloatArray(2200 * 8)

    // Instanced boxes: one draw call for the whole track and every box obstacle.
    private val boxVbo = IntArray(1)
    private val boxData = FloatArray(MAX_BOXES * Mesh.BOX_FLOATS)
    private lateinit var boxBuf: FloatBuffer
    private var boxCount = 0
    // Glowing pads (additive pass), same layout.
    private val glowData = FloatArray(420 * Mesh.BOX_FLOATS)
    private var glowCount = 0

    // Scratch colours (no allocations while drawing)
    private val zero = floatArrayOf(0f, 0f, 0f)
    private val white = floatArrayOf(1f, 1f, 1f)
    private val grayA = FloatArray(3)
    private val grayB = FloatArray(3)
    private val cloudA = FloatArray(3)
    private val cloudB = FloatArray(3)
    private val metal = floatArrayOf(0.62f, 0.66f, 0.74f)
    private val boostC = floatArrayOf(0.25f, 0.85f, 1f)
    private val jumpC = floatArrayOf(1f, 0.62f, 0.15f)
    private val finishC = floatArrayOf(1f, 1f, 1f)
    private val coinGold = floatArrayOf(1f, 0.8f, 0.22f)
    private val coinDark = floatArrayOf(0.7f, 0.42f, 0.05f)
    private val coinEmis = floatArrayOf(0.25f, 0.16f, 0.02f)
    private val coinGlow = floatArrayOf(1f, 0.8f, 0.3f)
    private val fireTrail = floatArrayOf(1f, 0.45f, 0.1f)
    private val blueTrail = floatArrayOf(0.3f, 0.6f, 1f)
    private val shieldC = floatArrayOf(0.35f, 0.7f, 1f)
    private val coinPos = FloatArray(3 * 48)
    private var coinCount = 0
    private val stoneC = FloatArray(3)
    private val trackB = FloatArray(3)

    // Colours derived from the current look, recomputed only when the look changes.
    private var lookCached: TowerLook? = null
    private val cSky = FloatArray(3)
    private val cGround = FloatArray(3)
    private val cFog = FloatArray(3)
    private val cCloudLight = FloatArray(3)
    private val cCloudShade = FloatArray(3)

    private fun cacheLook(t: TowerLook) {
        if (lookCached === t) return
        lookCached = t
        // Ambient light tinted by the theme (a white sky made everything milky).
        mixInto(cSky, t.bgTop, t.aurora[1], 0.45f)
        for (k in 0..2) cSky[k] = (cSky[k] * 1.6f + 0.12f).coerceAtMost(1f)
        mixInto(cGround, t.bgBottom, t.aurora[0], 0.4f)
        mixInto(cFog, t.bgBottom, t.bgTop, 0.5f)
        mixInto(cFog, cFog, t.aurora[0], 0.15f)
        mixInto(cCloudLight, t.env.cloud, white, 0.25f)
        mixInto(cCloudShade, t.env.cloud, t.bgBottom, 0.35f)
        mixInto(stoneC, t.ringB, white, 0.15f)
        mixInto(trackB, t.ringA, t.ringB, 0.35f)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        litBoxes = Program(Shaders.TRACK_VS, Shaders.lit("V_TRACK"))
        litParts = Program(Shaders.LIT_VS, Shaders.lit("V_TRACK"))
        litBall = Program(Shaders.LIT_VS, Shaders.lit("V_BALL"))
        lit = litBall
        shadow = Program(Shaders.LIT_VS, Shaders.SHADOW_FS)
        bg = Program(Shaders.BG_VS, Shaders.BG_FS)
        pts = Program(Shaders.PT_VS, Shaders.PT_FS)
        cloudProg = Program(Shaders.LIT_VS, Shaders.CLOUD_FS)
        blit = Program(Shaders.BG_VS, Shaders.BLIT_FS)
        sky = Program(Shaders.BG_VS, Shaders.SKY_FS)
        bgW = 0
        bgValid = false
        box = Mesh(Shapes.unitBox())
        sphere = Mesh(Shapes.sphere(Roll.BALL_R))
        disc = Mesh(Shapes.disc(1f))
        screen = Mesh(Shapes.screen())
        boxBuf = ByteBuffer.allocateDirect(boxData.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        GLES30.glGenBuffers(1, boxVbo, 0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, boxVbo[0])
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, boxData.size * 4, null, GLES30.GL_DYNAMIC_DRAW)
        GLES30.glBindVertexArray(0)
        lookCached = null

        ptBuf = ByteBuffer.allocateDirect(ptCapacity * 8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        GLES30.glGenVertexArrays(1, ptVao, 0)
        GLES30.glBindVertexArray(ptVao[0])
        GLES30.glGenBuffers(1, ptVbo, 0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, ptVbo[0])
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, ptCapacity * 8 * 4, null, GLES30.GL_DYNAMIC_DRAW)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 32, 0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 4, GLES30.GL_FLOAT, false, 32, 12)
        GLES30.glEnableVertexAttribArray(2)
        GLES30.glVertexAttribPointer(2, 1, GLES30.GL_FLOAT, false, 32, 28)
        GLES30.glBindVertexArray(0)

        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glDisable(GLES30.GL_CULL_FACE)
        lastNanos = 0L
    }

    override fun onSurfaceChanged(gl: GL10?, w: Int, h: Int) {
        width = max(1, w)
        height = max(1, h)
        GLES30.glViewport(0, 0, width, height)
        aspect = width.toFloat() / height
        setupBackgroundTarget()
    }

    private fun setupBackgroundTarget() {
        val w = max(1, width / BG_DIV)
        val h = max(1, height / BG_DIV)
        if (w == bgW && h == bgH) return
        if (bgW != 0) {
            GLES30.glDeleteFramebuffers(1, bgFbo, 0)
            GLES30.glDeleteTextures(1, bgTex, 0)
        }
        bgW = w
        bgH = h
        GLES30.glGenTextures(1, bgTex, 0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, bgTex[0])
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, w, h, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glGenFramebuffers(1, bgFbo, 0)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, bgFbo[0])
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, bgTex[0], 0)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        bgValid = false
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        val dt = if (lastNanos == 0L) 0f else (now - lastNanos) / 1e9f
        lastNanos = now
        game.update(dt)
        val rich = detailed()
        game.ambientOn = rich
        // Watchdog: if frames stay slow with the detailed scenery, switch it off for good.
        if (rich && dt > 0f) {
            watchTime += dt
            if (watchTime > 2.5f) {
                slowTime = if (dt > 0.026f) slowTime + dt else (slowTime - dt * 0.5f).coerceAtLeast(0f)
                if (slowTime > 1.5f) {
                    slowTime = 0f
                    onSlow?.invoke()
                }
            }
        } else {
            watchTime = 0f
            slowTime = 0f
        }

        val t = game.tower
        val sim = game.sim
        val lv = sim.level
        val time = game.clock
        val simT = sim.time
        cacheLook(t)

        // Camera: behind and above the ball, looking down the track; wider while boosting.
        val hfov = 46f + 7f * game.boostFx
        val vfov = Math.toDegrees(2.0 * atan(tan(Math.toRadians(hfov / 2.0)) / aspect)).toFloat().coerceIn(48f, 84f)
        Matrix.perspectiveM(proj, 0, vfov, aspect, 0.4f, 170f)
        val sh = game.shake
        val shx = sh * sin(time * 61f) * 0.5f
        val shy = sh * sin(time * 47f + 1f) * 0.35f
        val cz = -game.camS
        eye[0] = game.camX + shx; eye[1] = game.camY + 3.3f + shy; eye[2] = cz + 7.6f
        // The camera leans with tilted track.
        val roll = game.camRoll
        Matrix.setLookAtM(view, 0, eye[0], eye[1], eye[2], game.camX * 0.9f + shx * 0.5f, game.camY + 0.1f, cz - 9f, sin(roll), cos(roll), 0f)
        Matrix.multiplyMM(vp, 0, proj, 0, view, 0)

        GLES30.glClearColor(t.bgBottom[0], t.bgBottom[1], t.bgBottom[2], 1f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
        // The land below rises as the level goes on: the descent is felt.
        val prog = (0.12f + 0.75f * sim.progress).coerceIn(0f, 1f)
        if (rich) drawBackground(t, time, prog) else drawSky(t, time)

        frameTime = time
        frameLook = t

        // ---------------------------------------------------------- track and box obstacles (one call)
        boxCount = 0
        glowCount = 0
        val s0 = game.camS - BEHIND
        val s1 = game.camS + AHEAD
        var i = lv.segIndex(s1)
        while (i >= 0) {
            val g = lv.segs[i]
            if (g.s0 < s0 - lv.maxLen) break
            if (g.s1 >= s0) addSeg(g, t)
            i--
        }
        // Obstacles
        var nCoins = 0
        var oi = lv.obsIndex(s0 - 4f)
        while (oi < lv.obs.size) {
            val o = lv.obs[oi]
            if (o.s > s1) break
            if (!sim.broken(oi)) when (o.kind) {
                ObsKind.BLOCK, ObsKind.SLIDER -> addBox(o.cx(simT), o.y + o.h / 2f, o.s, o.w, o.h, o.d, 0f, 0f, t.danger, K_DANGER)
                ObsKind.GATE -> {
                    val b = o.gateBottom(simT)
                    addBox(o.x, b + o.h / 2f, o.s, o.w - 0.5f, o.h, o.d * 0.7f, 0f, 0f, t.danger, K_GATE)
                    for (e in 0..1) {
                        val px = o.x + (if (e == 0) -1f else 1f) * (o.w / 2f)
                        addBox(px, o.y + 1.4f, o.s, 0.32f, 2.8f, 0.5f, 0f, 0f, metal, K_METAL)
                    }
                    addBox(o.x, o.y + 2.9f, o.s, o.w + 0.32f, 0.26f, 0.5f, 0f, 0f, metal, K_METAL)
                }
                ObsKind.HAMMER -> {
                    for (e in 0..1) {
                        val px = o.x + (if (e == 0) -1f else 1f) * (o.amp + 0.9f)
                        addBox(px, o.y + 1.8f, o.s, 0.28f, 3.6f, 0.36f, 0f, 0f, metal, K_METAL)
                    }
                    addBox(o.x, o.y + HAMMER_PIVOT, o.s, 2f * o.amp + 2.1f, 0.3f, 0.4f, 0f, 0f, metal, K_METAL)
                }
                ObsKind.COIN -> if (!sim.coinTaken(oi) && nCoins < coinPos.size / 3) {
                    coinPos[nCoins * 3] = o.x; coinPos[nCoins * 3 + 1] = o.y + 0.08f * sin(time * 3f + o.s); coinPos[nCoins * 3 + 2] = -o.s
                    nCoins++
                }
            }
            oi++
        }
        coinCount = nCoins

        useLit(litBoxes)
        uploadBoxes(boxData, boxCount)
        lit.i("uInst", 1)
        material(0, 0, white, zero, zero, 80f, 0f)
        box.bindBoxInstances(boxVbo[0], 0)
        box.drawInstanced(boxCount)
        lit.i("uInst", 0)

        // Rotating parts: spinner bars and swinging hammers.
        useLit(litParts)
        oi = lv.obsIndex(s0 - 4f)
        while (oi < lv.obs.size) {
            val o = lv.obs[oi]
            if (o.s > s1) break
            if (!sim.broken(oi)) when (o.kind) {
                ObsKind.SPINNER -> {
                    val a = o.angle(simT)
                    partBox(o.x, o.y + 0.45f, -o.s, a, 0f, 2f * o.amp, 0.32f, 0.34f, t.danger, K_DANGER)
                    partBox(o.x, o.y + 0.5f, -o.s, a, 0f, 0.7f, 0.7f, 0.7f, metal, K_METAL)
                }
                ObsKind.HAMMER -> {
                    val off = (o.cx(simT) - o.x).coerceIn(-HAMMER_ARM * 0.95f, HAMMER_ARM * 0.95f)
                    val th = asin(off / HAMMER_ARM)
                    val px = o.x; val py = o.y + HAMMER_PIVOT
                    val hx = px + off; val hy = py - HAMMER_ARM * cos(th)
                    // Arm from the pivot to the head, then the head.
                    partBox((px + hx) / 2f, (py + hy) / 2f, -o.s, 0f, th, 0.16f, HAMMER_ARM, 0.16f, metal, K_METAL)
                    partBox(hx, hy + 0.1f, -o.s, 0f, th, o.w, o.h * 0.75f, o.d, t.danger, K_DANGER)
                }
            }
            oi++
        }

        // Ball
        useLit(litBall)
        val b = game.ball
        val sy = game.ballSy
        val sxz = game.ballSxz
        val dead = sim.dead
        val ballY = sim.y - Roll.BALL_R * (1f - sy)
        if (!(dead && !sim.fell)) {
            Matrix.setIdentityM(model, 0)
            Matrix.translateM(model, 0, sim.x, ballY, -sim.s)
            Matrix.rotateM(model, 0, -sim.lean * 12f, 0f, 0f, 1f)
            Matrix.scaleM(model, 0, sxz, sy, sxz)
            Matrix.rotateM(model, 0, -deg(sim.spin), 1f, 0f, 0f)
            material(2, b.style, if (dead) grayInto(grayA, b.c1) else b.c1, if (dead) grayInto(grayB, b.c2) else b.c2, zero, gloss = b.gloss, metal = b.metal)
            lit.m4("uModel", model)
            sphere.draw()
        }
        projectBall(sim.x, ballY, -sim.s)

        // Coins, spinning.
        for (c in 0 until coinCount) {
            Matrix.setIdentityM(model, 0)
            Matrix.translateM(model, 0, coinPos[c * 3], coinPos[c * 3 + 1], coinPos[c * 3 + 2])
            Matrix.rotateM(model, 0, time * 170f + coinPos[c * 3 + 2] * 20f, 0f, 1f, 0f)
            Matrix.scaleM(model, 0, 1.15f, 1.15f, 0.32f)
            material(2, BallStyle.METAL, coinGold, coinDark, coinEmis, gloss = 140f, metal = 1f)
            lit.m4("uModel", model)
            sphere.draw()
        }

        // ---------------------------------------------------------- transparent passes
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glDepthMask(false)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        // Soft shadow on the track under the ball (helps judge jumps).
        if (!dead || sim.fell) {
            val g = lv.support(sim.s, sim.x)
            if (g != null) {
                val top = g.topAt(sim.s, sim.x)
                val hgt = (sim.y - Roll.BALL_R - top).coerceAtLeast(0f)
                if (hgt < 8f) {
                    GLES30.glEnable(GLES30.GL_POLYGON_OFFSET_FILL)
                    GLES30.glPolygonOffset(-2f, -2f)
                    shadow.use()
                    shadow.m4("uVP", vp)
                    Matrix.setIdentityM(model, 0)
                    Matrix.translateM(model, 0, sim.x, top + 0.01f, -sim.s)
                    val sc = Roll.BALL_R * (1.15f + hgt * 0.12f)
                    Matrix.scaleM(model, 0, sc, 1f, sc)
                    shadow.m4("uModel", model)
                    shadow.f("uAlpha", 0.5f * (1f - hgt / 8f))
                    disc.draw()
                    GLES30.glDisable(GLES30.GL_POLYGON_OFFSET_FILL)
                }
            }
        }

        if (rich) drawClouds(t, time)

        // Additive glows: pads, the finish line, the shield bubble.
        GLES30.glBlendFunc(GLES30.GL_ONE, GLES30.GL_ONE)
        if (glowCount > 0) {
            useLit(litBoxes)
            lit.i("uInst", 1)
            lit.f("uAlpha", 0.35f + 0.15f * sin(time * 5f))
            material(3, 0, white, zero, zero, 1f, 0f)
            uploadBoxes(glowData, glowCount)
            box.bindBoxInstances(boxVbo[0], 0)
            box.drawInstanced(glowCount)
            lit.i("uInst", 0)
        }
        if (sim.shields > 0 && !dead) {
            useLit(litParts)
            Matrix.setIdentityM(model, 0)
            Matrix.translateM(model, 0, sim.x, sim.y, -sim.s)
            val sc = 1.55f + 0.06f * sin(time * 6f)
            Matrix.scaleM(model, 0, sc, sc, sc)
            material(3, 0, shieldC, zero, zero, 1f, 0f)
            lit.f("uAlpha", 0.45f)
            lit.m4("uModel", model)
            sphere.draw()
            lit.f("uAlpha", 1f)
        }

        drawParticles(ballY, dead)

        GLES30.glDepthMask(true)
        GLES30.glDisable(GLES30.GL_BLEND)
    }

    // ------------------------------------------------------------------ boxes

    /** A piece of track (plus its glow and a support column now and then). */
    private fun addSeg(g: com.farias.prismaroll.game.Seg, t: TowerLook) {
        val th = Roll.THICK
        val midS = g.s0 + g.len / 2f
        val cx = (g.x0 + g.x1) / 2f
        val top = (g.h0 + g.h1) / 2f
        val col: FloatArray
        val kind: Float
        when (g.kind) {
            SegKind.BOOST -> { col = boostC; kind = K_BOOST }
            SegKind.JUMP -> { col = jumpC; kind = K_JUMP }
            SegKind.FINISH -> { col = finishC; kind = K_FINISH }
            SegKind.STONE -> { col = stoneC; kind = K_STONE }
            else -> { col = if (g.tone == 0) t.ringA else trackB; kind = K_TRACK }
        }
        addBox(cx, top - th / 2f, midS, g.w, th, g.len, g.slant, g.slope, col, kind, g.bank)
        // Neon rails along both edges (additive glow).
        if (g.kind == SegKind.NORMAL || g.kind == SegKind.STONE) {
            for (e in 0..1) {
                val side = if (e == 0) -1f else 1f
                addGlow(cx + side * g.w / 2f, top + 0.03f + side * g.bank * g.w / 2f, midS, 0.16f, 0.12f, g.len * 0.98f, t.rim, g.slant, g.slope)
            }
        }
        when (g.kind) {
            SegKind.BOOST, SegKind.JUMP -> addGlow(cx, top + 0.02f, midS, g.w, 0.06f, g.len, col)
            SegKind.FINISH -> {
                // An arch over the line.
                for (e in 0..1) addBox(cx + (if (e == 0) -1f else 1f) * (g.w / 2f + 0.25f), top + 1.7f, midS, 0.4f, 3.4f, 0.4f, 0f, 0f, finishC, K_FINISH)
                addBox(cx, top + 3.55f, midS, g.w + 0.9f, 0.6f, 0.5f, 0f, 0f, finishC, K_FINISH)
                addGlow(cx, top + 3.55f, midS, g.w + 1f, 0.7f, 0.6f, coinGlow)
            }
        }
        // Tall columns down into the clouds under long pieces (sense of height).
        if (g.len >= 9f && g.kind == SegKind.NORMAL && ((g.s0 * 7.3f).toInt() and 1) == 0) {
            addBox(cx, top - th - 15f, midS, 0.7f, 30f, 0.7f, 0f, 0f, t.ringB, K_COLUMN)
        }
    }

    private fun addBox(x: Float, y: Float, s: Float, w: Float, h: Float, len: Float, slant: Float, slope: Float, c: FloatArray, kind: Float, bank: Float = 0f) {
        if (boxCount >= MAX_BOXES) return
        putBox(boxData, boxCount++, x, y, s, w, h, len, slant, slope, c, kind, bank)
    }

    private fun addGlow(x: Float, y: Float, s: Float, w: Float, h: Float, len: Float, c: FloatArray, slant: Float = 0f, slope: Float = 0f) {
        if (glowCount >= glowData.size / Mesh.BOX_FLOATS) return
        putBox(glowData, glowCount++, x, y, s, w * 1.04f, h, len * 1.04f, slant, slope, c, 0f, 0f)
    }

    private fun putBox(a: FloatArray, slot: Int, x: Float, y: Float, s: Float, w: Float, h: Float, len: Float, slant: Float, slope: Float, c: FloatArray, kind: Float, bank: Float) {
        var k = slot * Mesh.BOX_FLOATS
        a[k++] = x; a[k++] = y; a[k++] = -s; a[k++] = w
        a[k++] = h; a[k++] = len; a[k++] = slant; a[k++] = slope
        a[k++] = c[0]; a[k++] = c[1]; a[k++] = c[2]; a[k++] = kind
        a[k++] = bank; a[k++] = 0f; a[k++] = 0f; a[k] = 0f
    }

    private fun uploadBoxes(src: FloatArray, count: Int) {
        if (count <= 0) return
        val n = count * Mesh.BOX_FLOATS
        boxBuf.clear()
        boxBuf.put(src, 0, n).position(0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, boxVbo[0])
        GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0, n * 4, boxBuf)
    }

    /** A box turned by [yaw] (around Y) then [roll] (around Z), drawn alone. */
    private fun partBox(x: Float, y: Float, z: Float, yaw: Float, roll: Float, w: Float, h: Float, d: Float, c: FloatArray, kind: Float) {
        Matrix.setIdentityM(model, 0)
        Matrix.translateM(model, 0, x, y, z)
        if (yaw != 0f) Matrix.rotateM(model, 0, deg(yaw), 0f, 1f, 0f)
        if (roll != 0f) Matrix.rotateM(model, 0, deg(roll), 0f, 0f, 1f)
        Matrix.scaleM(model, 0, w, h, d)
        lit.f("uExtra", kind)
        material(0, 0, c, zero, zero, 100f, if (kind == K_METAL) 0.85f else 0f)
        lit.m4("uModel", model)
        box.draw()
    }

    private val HAMMER_PIVOT = 3.45f
    private val HAMMER_ARM = 2.8f

    // ------------------------------------------------------------------ helpers

    private var frameTime = 0f
    private lateinit var frameLook: TowerLook

    /** Switches to one of the lit variants and gives it this frame's shared uniforms. */
    private fun useLit(p: Program) {
        lit = p
        p.use()
        p.m4("uVP", vp)
        p.v3("uCam", eye)
        p.v3("uLight", 0.35f, 0.9f, 0.45f)
        p.v3("uSky", cSky)
        p.v3("uGround", cGround)
        p.v3("uRim", frameLook.rim)
        p.v3("uFog", cFog)
        p.v2("uFogRange", 60f, 150f)
        p.f("uTime", frameTime)
        p.f("uSpacing", 3f)
        p.f("uAlpha", 1f)
        p.f("uExtra", 0f)
        p.i("uInst", 0)
    }

    private fun material(kind: Int, pattern: Int, c: FloatArray, c2: FloatArray, emis: FloatArray, gloss: Float, metal: Float) {
        lit.i("uKind", kind)
        lit.i("uPattern", pattern)
        lit.v3("uColor", c)
        lit.v3("uColor2", c2)
        lit.v3("uEmissive", emis)
        lit.f("uGloss", gloss)
        lit.f("uMetal", metal)
    }

    private fun drawBackground(t: TowerLook, time: Float, prog: Float) {
        GLES30.glDepthMask(false)
        GLES30.glDisable(GLES30.GL_DEPTH_TEST)
        // Repaint the small sky texture every other frame (it moves slowly).
        if (!bgValid || bgFrame++ % 2 == 0 || t !== lastBgLook) {
            lastBgLook = t
            bgValid = true
            val e = t.env
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, bgFbo[0])
            GLES30.glViewport(0, 0, bgW, bgH)
            bg.use()
            bg.v3("uTop", t.bgTop)
            bg.v3("uBottom", t.bgBottom)
            bg.v3("uA1", t.aurora[0])
            bg.v3("uA2", t.aurora[1])
            bg.v3("uA3", t.aurora[2])
            bg.v3("uLand1", e.land1)
            bg.v3("uLand2", e.land2)
            bg.v3("uSun", e.sun)
            bg.v3("uCloud", e.cloud)
            bg.i("uScene", e.scene)
            bg.f("uTime", time)
            bg.f("uProg", prog)
            bg.f("uAspect", width.toFloat() / height)
            bg.f("uStars", if (t.stars) 1f else 0f)
            screen.draw()
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
            GLES30.glViewport(0, 0, width, height)
        }
        blit.use()
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, bgTex[0])
        blit.i("uTex", 0)
        screen.draw()
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthMask(true)
    }

    private var lastBgLook: TowerLook? = null

    private fun drawSky(t: TowerLook, time: Float) {
        GLES30.glDepthMask(false)
        GLES30.glDisable(GLES30.GL_DEPTH_TEST)
        sky.use()
        sky.v3("uTop", t.bgTop)
        sky.v3("uBottom", t.bgBottom)
        sky.v3("uA1", t.aurora[0])
        sky.v3("uA2", t.aurora[1])
        sky.v3("uA3", t.aurora[2])
        sky.f("uTime", time)
        sky.f("uAspect", width.toFloat() / height)
        sky.f("uStars", if (t.stars) 1f else 0f)
        screen.draw()
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthMask(true)
        bgValid = false
    }

    private fun h(i: Int, k: Int): Float {
        var n = i * 73856093 xor (k * 19349663)
        n = (n xor (n ushr 13)) * 1274126177
        return ((n xor (n ushr 16)) and 0xFFFF) / 65535f
    }

    /** Clouds beside and below the track, at fixed spots along it, so the speed is felt. */
    private fun drawClouds(t: TowerLook, time: Float) {
        val e = t.env
        cloudProg.use()
        cloudProg.m4("uVP", vp)
        val step = 6f
        val j0 = ((game.camS - BEHIND) / step).toInt()
        val j1 = ((game.camS + AHEAD) / step).toInt()
        val space = e.scene == Scene.SPACE
        for (j in j0..j1) {
            val s = j * step + h(j, 1) * 3f
            val side = if (h(j, 2) < 0.5f) -1f else 1f
            val below = h(j, 3) < 0.85f
            val x = game.camX + side * (if (below) 2f + h(j, 4) * 9f else 9f + h(j, 4) * 10f) + sin(time * 0.08f + j) * 0.8f
            val y = game.camY + (if (below) -5f - h(j, 5) * 9f else -2f + h(j, 5) * 7f)
            val z = -s
            val sc = 2.8f + h(j, 6) * 3.5f
            val dx = x - eye[0]; val dy = y - eye[1]; val dz = z - eye[2]
            val dist = sqrt(dx * dx + dy * dy + dz * dz)
            val f = (0.35f + (dist - 20f) / 60f).coerceIn(0.35f, 0.9f)
            Matrix.setIdentityM(model, 0)
            Matrix.translateM(model, 0, x, y, z)
            Matrix.rotateM(model, 0, -90f, 1f, 0f, 0f)
            Matrix.scaleM(model, 0, sc * 1.6f, 1f, sc)
            cloudProg.m4("uModel", model)
            cloudProg.v3("uColor", mixInto(cloudA, cCloudLight, cFog, f))
            cloudProg.v3("uColor2", mixInto(cloudB, cCloudShade, cFog, f))
            cloudProg.f("uAlpha", (if (space) 0.25f else 0.5f) * (1f - f * 0.5f))
            cloudProg.f("uSeed", h(j, 7) * 10f)
            disc.draw()
        }
    }

    private fun drawParticles(ballY: Float, dead: Boolean) {
        val pa = game.particles
        val o = ptScratch
        var n = 0
        fun put(x: Float, y: Float, z: Float, r: Float, g: Float, b: Float, a: Float, sz: Float) {
            var k = n * 8
            o[k++] = x; o[k++] = y; o[k++] = z; o[k++] = r; o[k++] = g; o[k++] = b; o[k++] = a; o[k] = sz
            n++
        }
        val sim = game.sim
        // Comet trail behind the ball.
        if (!dead && sim.started) {
            val style = game.trailStyle
            val boost = sim.boosted
            val tc = when {
                boost -> boostC
                style == com.farias.prismaroll.data.Trails.BLUE_FIRE -> blueTrail
                style == com.farias.prismaroll.data.Trails.GOLD -> coinGold
                style == com.farias.prismaroll.data.Trails.GHOST -> white
                else -> game.ball.trail
            }
            val count = game.trailCount
            val alphaK = if (style == com.farias.prismaroll.data.Trails.GHOST) 0.3f else if (style == com.farias.prismaroll.data.Trails.NEON) 0.85f else 0.55f
            val sizeK = if (style == com.farias.prismaroll.data.Trails.NEON || style == com.farias.prismaroll.data.Trails.GHOST) 1.4f else 1f
            for (idx in 0 until count - 1) {
                val k = idx.toFloat() / count
                val ts = game.trailS(idx)
                if (sim.s - ts < 0.15f) continue
                if (style == com.farias.prismaroll.data.Trails.RAINBOW && !boost) {
                    val hh = (idx / 18f + frameTime * 0.4f) % 1f
                    put(game.trailX(idx), game.trailY(idx), -ts, game.hueR(hh), game.hueG(hh), game.hueB(hh), 0.7f * k, Roll.BALL_R * (0.6f + 1.4f * k))
                } else {
                    put(game.trailX(idx), game.trailY(idx), -ts, tc[0], tc[1], tc[2], alphaK * k, Roll.BALL_R * (0.5f + 1.3f * k) * sizeK)
                }
            }
            // Soft glow around the ball.
            put(sim.x, ballY, -sim.s, tc[0], tc[1], tc[2], if (boost) 0.6f else 0.3f, Roll.BALL_R * (if (boost) 5f else 3.6f))
        }
        for (c in 0 until coinCount) {
            put(coinPos[c * 3], coinPos[c * 3 + 1], coinPos[c * 3 + 2], coinGlow[0], coinGlow[1], coinGlow[2], 0.4f, 0.85f)
        }
        val limit = min(pa.count, ptCapacity - n)
        for (i in 0 until limit) {
            val l = (pa.life[i] / pa.maxLife[i]).coerceIn(0f, 1f)
            put(pa.px[i], pa.py[i], pa.pz[i], pa.r[i], pa.g[i], pa.b[i], pa.a[i] * l, pa.size[i] * (0.5f + 0.5f * l) * 3f)
        }
        if (n == 0) return
        ptBuf.clear()
        ptBuf.put(o, 0, n * 8).position(0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, ptVbo[0])
        GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0, n * 32, ptBuf)
        pts.use()
        pts.m4("uVP", vp)
        pts.f("uScale", height * 1.1f)
        GLES30.glBindVertexArray(ptVao[0])
        GLES30.glDrawArrays(GLES30.GL_POINTS, 0, n)
        GLES30.glBindVertexArray(0)
    }

    private fun projectBall(x: Float, y: Float, z: Float) {
        vec[0] = x; vec[1] = y; vec[2] = z; vec[3] = 1f
        Matrix.multiplyMV(out4, 0, vp, 0, vec, 0)
        if (out4[3] != 0f) {
            game.ballScreenX = (out4[0] / out4[3]) * 0.5f + 0.5f
            game.ballScreenY = 1f - ((out4[1] / out4[3]) * 0.5f + 0.5f)
        }
    }

    private fun deg(r: Float) = Math.toDegrees(r.toDouble()).toFloat()
    private fun mixInto(out: FloatArray, a: FloatArray, b: FloatArray, t: Float): FloatArray {
        val r = a[0] + (b[0] - a[0]) * t; val g = a[1] + (b[1] - a[1]) * t; val bl = a[2] + (b[2] - a[2]) * t
        out[0] = r; out[1] = g; out[2] = bl
        return out
    }
    private fun grayInto(out: FloatArray, c: FloatArray): FloatArray {
        val l = c[0] * 0.3f + c[1] * 0.59f + c[2] * 0.11f
        out[0] = l * 0.6f; out[1] = l * 0.6f; out[2] = l * 0.65f
        return out
    }
}
