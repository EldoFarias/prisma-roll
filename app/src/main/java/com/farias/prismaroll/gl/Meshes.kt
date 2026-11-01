package com.farias.prismaroll.gl

import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Interleaved vertex data: position (3), normal (3), uv (2). Primitive arrays, no boxing. */
class MeshData {
    var v = FloatArray(8 * 1024)
        private set
    var i = IntArray(3 * 1024)
        private set
    var vSize = 0
        private set
    var iSize = 0
        private set
    val count get() = vSize / 8

    fun vert(x: Float, y: Float, z: Float, nx: Float, ny: Float, nz: Float, u: Float = 0f, w: Float = 0f): Int {
        if (vSize + 8 > v.size) v = v.copyOf(v.size * 2)
        val a = v
        var k = vSize
        a[k++] = x; a[k++] = y; a[k++] = z; a[k++] = nx; a[k++] = ny; a[k++] = nz; a[k++] = u; a[k++] = w
        vSize = k
        return count - 1
    }

    fun tri(a: Int, b: Int, c: Int) {
        if (iSize + 3 > i.size) i = i.copyOf(i.size * 2)
        i[iSize++] = a; i[iSize++] = b; i[iSize++] = c
    }

    fun quad(a: Int, b: Int, c: Int, d: Int) {
        tri(a, b, c); tri(a, c, d)
    }
}

/** A GPU mesh (VAO + buffers). Must be created and drawn on the GL thread. */
class Mesh(data: MeshData) {
    private val vao = IntArray(1)
    private val bufs = IntArray(2)
    private val indexCount = data.iSize

    init {
        val vb: FloatBuffer = ByteBuffer.allocateDirect(data.vSize * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        vb.put(data.v, 0, data.vSize).position(0)
        val ib: IntBuffer = ByteBuffer.allocateDirect(data.iSize * 4).order(ByteOrder.nativeOrder()).asIntBuffer()
        ib.put(data.i, 0, data.iSize).position(0)
        GLES30.glGenVertexArrays(1, vao, 0)
        GLES30.glBindVertexArray(vao[0])
        GLES30.glGenBuffers(2, bufs, 0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, bufs[0])
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, data.vSize * 4, vb, GLES30.GL_STATIC_DRAW)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, bufs[1])
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, data.iSize * 4, ib, GLES30.GL_STATIC_DRAW)
        val stride = 8 * 4
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, stride, 12)
        GLES30.glEnableVertexAttribArray(2)
        GLES30.glVertexAttribPointer(2, 2, GLES30.GL_FLOAT, false, stride, 24)
        GLES30.glBindVertexArray(0)
    }

    fun draw() {
        GLES30.glBindVertexArray(vao[0])
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, indexCount, GLES30.GL_UNSIGNED_INT, 0)
    }

    /**
     * Points attributes 3 (vec4: angle, y, scaleXZ, scaleY) and 4 (vec3 colour) at [instVbo], starting at
     * instance [first], one value per instance. Lets one draw call place many copies of this mesh.
     */
    fun bindInstances(instVbo: Int, first: Int) {
        GLES30.glBindVertexArray(vao[0])
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, instVbo)
        val stride = INST_FLOATS * 4
        val base = first * stride
        GLES30.glEnableVertexAttribArray(3)
        GLES30.glVertexAttribPointer(3, 4, GLES30.GL_FLOAT, false, stride, base)
        GLES30.glVertexAttribDivisor(3, 1)
        GLES30.glEnableVertexAttribArray(4)
        GLES30.glVertexAttribPointer(4, 3, GLES30.GL_FLOAT, false, stride, base + 16)
        GLES30.glVertexAttribDivisor(4, 1)
    }

    /**
     * Points attributes 3, 4, 5 and 6 (four vec4) at [instVbo] from instance [first]: the layout of the
     * track boxes (see Shaders.TRACK_VS).
     */
    fun bindBoxInstances(instVbo: Int, first: Int) {
        GLES30.glBindVertexArray(vao[0])
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, instVbo)
        val stride = BOX_FLOATS * 4
        val base = first * stride
        for (a in 0..3) {
            GLES30.glEnableVertexAttribArray(3 + a)
            GLES30.glVertexAttribPointer(3 + a, 4, GLES30.GL_FLOAT, false, stride, base + a * 16)
            GLES30.glVertexAttribDivisor(3 + a, 1)
        }
    }

    fun drawInstanced(count: Int) {
        if (count <= 0) return
        GLES30.glBindVertexArray(vao[0])
        GLES30.glDrawElementsInstanced(GLES30.GL_TRIANGLES, indexCount, GLES30.GL_UNSIGNED_INT, 0, count)
    }

    companion object {
        const val INST_FLOATS = 7
        const val BOX_FLOATS = 16
    }
}

/**
 * Shape builders. Angles follow the game: a point at angle θ sits at (r·sinθ, y, r·cosθ), so θ = 0
 * faces the camera (where the ball is) and rotating about +Y adds to θ.
 */
object Shapes {

    /**
     * A ring sector from angle 0 to [span], radii [rIn]..[rOut], height [h] centred on y = 0, with a
     * chamfer [b] on all four long edges (catches highlights for a premium look) and flat end caps.
     */
    fun wedge(span: Float, rIn: Float, rOut: Float, h: Float, b: Float, steps: Int = 8): MeshData {
        val m = MeshData()
        val top = h / 2f
        val bot = -h / 2f
        // Cross-section profile (r, y), counter-clockwise seen with r to the right and y up.
        val prof = arrayOf(
            floatArrayOf(rIn + b, top), floatArrayOf(rOut - b, top),
            floatArrayOf(rOut, top - b), floatArrayOf(rOut, bot + b),
            floatArrayOf(rOut - b, bot), floatArrayOf(rIn + b, bot),
            floatArrayOf(rIn, bot + b), floatArrayOf(rIn, top - b),
        )
        // Sweep each profile edge along the arc, flat-shaded per edge.
        for (e in prof.indices) {
            val p0 = prof[e]
            val p1 = prof[(e + 1) % prof.size]
            // The profile runs clockwise (r right, y up), so the outward normal is the edge
            // direction turned +90°.
            val dr = p1[0] - p0[0]
            val dy = p1[1] - p0[1]
            val len = sqrt(dr * dr + dy * dy)
            val nr = -dy / len
            val ny = dr / len
            var prevA = -1
            var prevB = -1
            for (s in 0..steps) {
                val a = span * s / steps
                val sa = sin(a)
                val ca = cos(a)
                val ia = m.vert(p0[0] * sa, p0[1], p0[0] * ca, nr * sa, ny, nr * ca, s.toFloat() / steps, p0[0])
                val ib = m.vert(p1[0] * sa, p1[1], p1[0] * ca, nr * sa, ny, nr * ca, s.toFloat() / steps, p1[0])
                if (prevA >= 0) m.quad(prevA, ia, ib, prevB)
                prevA = ia
                prevB = ib
            }
        }
        // End caps (convex octagon fan).
        for (end in 0..1) {
            val a = if (end == 0) 0f else span
            val sa = sin(a)
            val ca = cos(a)
            // Tangent direction of increasing angle is (cos a, 0, -sin a); cap at 0 faces -tangent.
            val sign = if (end == 0) -1f else 1f
            val nx = ca * sign
            val nz = -sa * sign
            val base = m.count
            for (p in prof) m.vert(p[0] * sa, p[1], p[0] * ca, nx, 0f, nz, 0f, p[0])
            for (k in 1 until prof.size - 1) {
                if (end == 0) m.tri(base, base + k + 1, base + k) else m.tri(base, base + k, base + k + 1)
            }
        }
        return m
    }

    /** Open cylinder of [radius] from y = [y0] to [y1], with a dome-ish cap on top. */
    fun cylinder(radius: Float, y0: Float, y1: Float, seg: Int = 64): MeshData {
        val m = MeshData()
        for (s in 0..seg) {
            val a = (2 * PI * s / seg).toFloat()
            val x = sin(a)
            val z = cos(a)
            m.vert(radius * x, y0, radius * z, x, 0f, z, s.toFloat() / seg, y0)
            m.vert(radius * x, y1, radius * z, x, 0f, z, s.toFloat() / seg, y1)
            if (s > 0) {
                val b = m.count - 4
                m.quad(b, b + 2, b + 3, b + 1)
            }
        }
        // Top cap
        val c = m.vert(0f, y1, 0f, 0f, 1f, 0f)
        val ring = m.count
        for (s in 0..seg) {
            val a = (2 * PI * s / seg).toFloat()
            m.vert(radius * sin(a), y1, radius * cos(a), sin(a) * 0.3f, 0.95f, cos(a) * 0.3f)
        }
        for (s in 0 until seg) m.tri(c, ring + s, ring + s + 1)
        return m
    }

    fun sphere(r: Float, lat: Int = 28, lon: Int = 40): MeshData {
        val m = MeshData()
        for (i in 0..lat) {
            val th = (PI * i / lat).toFloat()
            for (j in 0..lon) {
                val ph = (2 * PI * j / lon).toFloat()
                val x = sin(th) * sin(ph)
                val y = cos(th)
                val z = sin(th) * cos(ph)
                m.vert(r * x, r * y, r * z, x, y, z, j.toFloat() / lon, i.toFloat() / lat)
            }
        }
        val row = lon + 1
        for (i in 0 until lat) for (j in 0 until lon) {
            val a = i * row + j
            m.quad(a, a + row, a + row + 1, a + 1)
        }
        return m
    }

    // ------------------------------------------------------------------ the base the tower stands on

    private fun h1(x: Int): Float {
        var n = x * 374761393 + 668265263
        n = (n xor (n ushr 13)) * 1274126177
        return ((n xor (n ushr 16)) and 0xFFFFFF) / 16777215f
    }

    /** Smooth periodic value noise around the circle ([cells] per turn) and along y. */
    private fun ring(a: Float, y: Float, cells: Int, seed: Int): Float {
        val u = a / (2 * PI.toFloat()) * cells
        val i0 = kotlin.math.floor(u).toInt()
        val fu = u - i0
        val j0 = kotlin.math.floor(y).toInt()
        val fy = y - j0
        fun v(i: Int, j: Int) = h1(((i % cells) + cells) % cells + j * 131 + seed * 7919)
        val su = fu * fu * (3 - 2 * fu)
        val sy = fy * fy * (3 - 2 * fy)
        val a0 = v(i0, j0) + (v(i0 + 1, j0) - v(i0, j0)) * su
        val a1 = v(i0, j0 + 1) + (v(i0 + 1, j0 + 1) - v(i0, j0 + 1)) * su
        return a0 + (a1 - a0) * sy
    }

    private fun rockNoise(a: Float, y: Float, seed: Int): Float =
        ring(a, y * 0.18f, 7, seed) * 0.55f + ring(a, y * 0.45f, 17, seed + 1) * 0.3f + ring(a, y * 1.1f, 37, seed + 2) * 0.15f

    /**
     * What the tower stands on, top at y = 0: a rocky mountain peak, a castle keep on a rock, or a floating
     * asteroid. The flat top (radius [plaza]) is where the goal ring sits.
     */
    fun base(kind: Int, plaza: Float, seed: Int = 3): MeshData {
        val m = MeshData()
        val seg = 72
        val rows = ArrayList<Float>() // y of each row
        val prof = ArrayList<Float>() // base radius of each row
        val jag = ArrayList<Float>() // how rocky each row is
        fun row(y: Float, r: Float, j: Float) { rows.add(y); prof.add(r); jag.add(j) }
        when (kind) {
            BaseKind.CASTLE -> {
                val keep = plaza + 0.15f
                row(0f, keep, 0f)
                row(-0.5f, keep, 0f)
                row(-0.55f, keep - 0.18f, 0f)
                var y = -0.6f
                while (y > -8.4f) { row(y, keep - 0.18f, 0f); y -= 0.9f }
                row(-8.5f, keep - 0.18f, 0f)
                row(-8.6f, keep + 0.35f, 0f) // ledge where the keep meets the rock
                row(-9.0f, keep + 0.35f, 0f)
                y = -9.4f
                while (y > -60f) {
                    val d = -y - 9f
                    row(y, keep + 0.9f + Math.pow(d.toDouble(), 0.82).toFloat() * 0.5f, (d / 2.5f).coerceAtMost(1f))
                    y -= if (y > -20f) 0.9f else 2.2f
                }
            }
            BaseKind.ASTEROID -> {
                row(0f, plaza + 0.2f, 0f)
                row(-0.35f, plaza + 0.4f, 0.15f)
                var y = -0.8f
                while (y > -16f) {
                    val t = -y / 16f
                    row(y, (plaza + 0.6f + 3.2f * Math.pow(kotlin.math.sin(PI * t * 0.75), 0.7).toFloat()) * (1f - t * t * 0.85f), 1f)
                    y -= 0.7f
                }
                row(-16.3f, 0.3f, 0.3f)
            }
            else -> {
                row(0f, plaza + 0.2f, 0f)
                row(-0.3f, plaza + 0.35f, 0.05f)
                var y = -0.7f
                while (y > -64f) {
                    val d = -y
                    row(y, plaza + 0.6f + Math.pow(d.toDouble(), 0.85).toFloat() * 0.42f, (d / 3f).coerceAtMost(1f))
                    y -= if (y > -14f) 0.6f else 2.2f
                }
            }
        }
        val nRows = rows.size
        val px = Array(nRows) { FloatArray(seg + 1) }
        val py = Array(nRows) { FloatArray(seg + 1) }
        val pz = Array(nRows) { FloatArray(seg + 1) }
        for (j in 0 until nRows) for (i in 0..seg) {
            val a = (2 * PI * i / seg).toFloat()
            val n = rockNoise(a, rows[j], seed)
            val r = prof[j] * (1f + jag[j] * (n - 0.45f) * 0.5f)
            val yy = rows[j] + jag[j] * (ring(a, rows[j] * 0.3f, 11, seed + 9) - 0.5f) * 1.1f
            px[j][i] = r * sin(a); py[j][i] = yy; pz[j][i] = r * cos(a)
        }
        val base0 = m.count
        for (j in 0 until nRows) for (i in 0..seg) {
            val jn = minOf(j + 1, nRows - 1); val jp = maxOf(j - 1, 0)
            val inx = if (i == seg) 1 else i + 1; val ipx = if (i == 0) seg - 1 else i - 1
            val dx = px[jn][i] - px[jp][i]; val dy = py[jn][i] - py[jp][i]; val dz = pz[jn][i] - pz[jp][i]
            val ex = px[j][inx] - px[j][ipx]; val ey = py[j][inx] - py[j][ipx]; val ez = pz[j][inx] - pz[j][ipx]
            var nx = dy * ez - dz * ey; var ny = dz * ex - dx * ez; var nz = dx * ey - dy * ex
            val l = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(1e-6f)
            nx /= l; ny /= l; nz /= l
            m.vert(px[j][i], py[j][i], pz[j][i], nx, ny, nz, i.toFloat() / seg, rows[j])
        }
        val stride = seg + 1
        for (j in 0 until nRows - 1) for (i in 0 until seg) {
            val a = base0 + j * stride + i
            m.quad(a, a + 1, a + stride + 1, a + stride)
        }
        // Flat top (the plaza)
        val c = m.vert(0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f)
        val ring0 = m.count
        for (i in 0..seg) {
            val a = (2 * PI * i / seg).toFloat()
            m.vert(px[0][i], 0f, pz[0][i], 0f, 1f, 0f, i.toFloat() / seg, 0f)
        }
        for (i in 0 until seg) m.tri(c, ring0 + i, ring0 + i + 1)
        if (kind == BaseKind.CASTLE) {
            // Battlements around the top of the keep.
            val n = 16
            val r = prof[0] - 0.22f
            for (k in 0 until n) {
                val a = (2 * PI * (k + 0.5) / n).toFloat()
                box(m, r * sin(a), 0.35f, r * cos(a), 0.62f, 0.7f, 0.42f, a)
            }
        }
        return m
    }

    /** Box centred at (x, y, z), size w × h × d, turned by [yaw] around Y. */
    fun box(m: MeshData, x: Float, y: Float, z: Float, w: Float, h: Float, d: Float, yaw: Float) {
        val c = cos(yaw); val s = sin(yaw)
        // local axes: right = (c, 0, -s), up = (0,1,0), out = (s, 0, c)
        fun p(lx: Float, ly: Float, lz: Float) = floatArrayOf(x + lx * c + lz * s, y + ly, z - lx * s + lz * c)
        val hw = w / 2; val hh = h / 2; val hd = d / 2
        val faces = arrayOf(
            floatArrayOf(0f, 0f, 1f), floatArrayOf(0f, 0f, -1f), floatArrayOf(1f, 0f, 0f),
            floatArrayOf(-1f, 0f, 0f), floatArrayOf(0f, 1f, 0f), floatArrayOf(0f, -1f, 0f),
        )
        for (f in faces) {
            // two in-plane axes for this face
            val (ux, uy, uz) = if (f[1] != 0f) Triple(1f, 0f, 0f) else if (f[0] != 0f) Triple(0f, 0f, 1f) else Triple(1f, 0f, 0f)
            val vx = f[1] * uz - f[2] * uy; val vy = f[2] * ux - f[0] * uz; val vz = f[0] * uy - f[1] * ux
            val corners = arrayOf(-1f to -1f, 1f to -1f, 1f to 1f, -1f to 1f)
            val idx = IntArray(4)
            for ((k, cc) in corners.withIndex()) {
                val lx = (f[0] + ux * cc.first + vx * cc.second) * hw
                val ly = (f[1] + uy * cc.first + vy * cc.second) * hh
                val lz = (f[2] + uz * cc.first + vz * cc.second) * hd
                val q = p(lx, ly, lz)
                val nx = f[0] * c + f[2] * s; val nz = -f[0] * s + f[2] * c
                idx[k] = m.vert(q[0], q[1], q[2], nx, f[1], nz, 0f, y + ly)
            }
            m.quad(idx[0], idx[1], idx[2], idx[3])
        }
    }

    /** Unit box (-0.5..0.5) with flat normals per face. */
    fun unitBox(): MeshData {
        val m = MeshData()
        val faces = arrayOf(
            floatArrayOf(0f, 0f, 1f), floatArrayOf(0f, 0f, -1f), floatArrayOf(1f, 0f, 0f),
            floatArrayOf(-1f, 0f, 0f), floatArrayOf(0f, 1f, 0f), floatArrayOf(0f, -1f, 0f),
        )
        for (f in faces) {
            val u = if (f[1] != 0f) floatArrayOf(1f, 0f, 0f) else if (f[0] != 0f) floatArrayOf(0f, 0f, 1f) else floatArrayOf(1f, 0f, 0f)
            // v = f × u
            val v = floatArrayOf(f[1] * u[2] - f[2] * u[1], f[2] * u[0] - f[0] * u[2], f[0] * u[1] - f[1] * u[0])
            val idx = IntArray(4)
            val corners = arrayOf(-1f to -1f, 1f to -1f, 1f to 1f, -1f to 1f)
            for ((k, c) in corners.withIndex()) {
                val x = (f[0] + u[0] * c.first + v[0] * c.second) * 0.5f
                val y = (f[1] + u[1] * c.first + v[1] * c.second) * 0.5f
                val z = (f[2] + u[2] * c.first + v[2] * c.second) * 0.5f
                idx[k] = m.vert(x, y, z, f[0], f[1], f[2], c.first * 0.5f, c.second * 0.5f)
            }
            m.quad(idx[0], idx[1], idx[2], idx[3])
        }
        return m
    }

    /** Flat square in XZ, uv in -1..1 (for paint splats and glows). */
    fun disc(size: Float): MeshData {
        val m = MeshData()
        val s = size
        val a = m.vert(-s, 0f, -s, 0f, 1f, 0f, -1f, -1f)
        val b = m.vert(s, 0f, -s, 0f, 1f, 0f, 1f, -1f)
        val c = m.vert(s, 0f, s, 0f, 1f, 0f, 1f, 1f)
        val d = m.vert(-s, 0f, s, 0f, 1f, 0f, -1f, 1f)
        m.quad(a, d, c, b)
        return m
    }

    /** Full-screen triangle. */
    fun screen(): MeshData {
        val m = MeshData()
        m.vert(-1f, -1f, 0f, 0f, 0f, 1f, 0f, 0f)
        m.vert(3f, -1f, 0f, 0f, 0f, 1f, 2f, 0f)
        m.vert(-1f, 3f, 0f, 0f, 0f, 1f, 0f, 2f)
        m.tri(0, 1, 2)
        return m
    }
}

/** Compiles and links a shader program; caches uniform locations. */
class Program(vs: String, fs: String) {
    val id: Int = GLES30.glCreateProgram()
    private val locs = HashMap<String, Int>()

    init {
        val v = compile(GLES30.GL_VERTEX_SHADER, vs)
        val f = compile(GLES30.GL_FRAGMENT_SHADER, fs)
        GLES30.glAttachShader(id, v)
        GLES30.glAttachShader(id, f)
        GLES30.glLinkProgram(id)
        val ok = IntArray(1)
        GLES30.glGetProgramiv(id, GLES30.GL_LINK_STATUS, ok, 0)
        if (ok[0] == 0) throw RuntimeException("link: " + GLES30.glGetProgramInfoLog(id))
    }

    private fun compile(type: Int, src: String): Int {
        val s = GLES30.glCreateShader(type)
        GLES30.glShaderSource(s, src)
        GLES30.glCompileShader(s)
        val ok = IntArray(1)
        GLES30.glGetShaderiv(s, GLES30.GL_COMPILE_STATUS, ok, 0)
        if (ok[0] == 0) throw RuntimeException("shader: " + GLES30.glGetShaderInfoLog(s))
        return s
    }

    fun use() = GLES30.glUseProgram(id)
    fun loc(name: String): Int = locs.getOrPut(name) { GLES30.glGetUniformLocation(id, name) }
    fun f(name: String, x: Float) = GLES30.glUniform1f(loc(name), x)
    fun i(name: String, x: Int) = GLES30.glUniform1i(loc(name), x)
    fun v2(name: String, x: Float, y: Float) = GLES30.glUniform2f(loc(name), x, y)
    fun v3(name: String, c: FloatArray) = GLES30.glUniform3f(loc(name), c[0], c[1], c[2])
    fun v3(name: String, x: Float, y: Float, z: Float) = GLES30.glUniform3f(loc(name), x, y, z)
    fun m4(name: String, m: FloatArray) = GLES30.glUniformMatrix4fv(loc(name), 1, false, m, 0)
}
