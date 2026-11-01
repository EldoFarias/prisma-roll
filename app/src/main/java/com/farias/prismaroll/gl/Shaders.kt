package com.farias.prismaroll.gl

/** GLSL ES 3.0 sources. */
object Shaders {

    // ------------------------------------------------------------------ lit meshes

    const val LIT_VS = """#version 300 es
layout(location=0) in vec3 aPos;
layout(location=1) in vec3 aNormal;
layout(location=2) in vec2 aUv;
layout(location=3) in vec4 aInst;      // instanced slices: angle, y, scale xz, scale y
layout(location=4) in vec3 aInstColor;
uniform mat4 uVP;
uniform mat4 uModel;
uniform mediump int uInst;
uniform float uExtra;
out vec3 vWorld;
out vec3 vNormal;
out vec3 vLocal;
out vec2 vUv;
out vec3 vInstColor;
out float vExtra;
void main() {
    vExtra = uExtra;
    vec4 w;
    if (uInst == 1) {
        float c = cos(aInst.x); float s = sin(aInst.x);
        vec3 p = aPos * vec3(aInst.z, aInst.w, aInst.z);
        w = vec4(c * p.x + s * p.z, p.y + aInst.y, -s * p.x + c * p.z, 1.0);
        vNormal = vec3(c * aNormal.x + s * aNormal.z, aNormal.y, -s * aNormal.x + c * aNormal.z);
    } else {
        w = uModel * vec4(aPos, 1.0);
        vNormal = mat3(uModel) * aNormal;
    }
    vWorld = w.xyz;
    vLocal = aPos;
    vUv = aUv;
    vInstColor = aInstColor;
    gl_Position = uVP * w;
}
"""


    /**
     * Instanced boxes for the track and obstacles: a unit box (-0.5..0.5) stretched and sheared so one
     * instance is a whole slanted / sloped piece of track.
     */
    const val TRACK_VS = """#version 300 es
layout(location=0) in vec3 aPos;
layout(location=1) in vec3 aNormal;
layout(location=2) in vec2 aUv;
layout(location=3) in vec4 aA;   // centre xyz, width
layout(location=4) in vec4 aB;   // thickness, length, slant (dx/ds), slope (dy/ds)
layout(location=5) in vec4 aC;   // colour rgb, kind
uniform mat4 uVP;
out vec3 vWorld;
out vec3 vNormal;
out vec3 vLocal;
out vec2 vUv;
out vec3 vInstColor;
out float vExtra;
void main() {
    float len = aB.y;
    vec3 X = vec3(aA.w, 0.0, 0.0);
    vec3 Y = vec3(0.0, aB.x, 0.0);
    vec3 Z = vec3(aB.z * len, aB.w * len, -len);
    vec3 w = aA.xyz + X * aPos.x + Y * aPos.y + Z * aPos.z;
    mat3 M = mat3(X, Y, Z);
    vNormal = transpose(inverse(M)) * aNormal;
    vWorld = w;
    vLocal = aPos;
    // Distance (units) to the side edges and to the front/back edges of the face.
    vUv = vec2((0.5 - abs(aPos.x)) * aA.w, (0.5 - abs(aPos.z)) * len);
    vInstColor = aC.rgb;
    vExtra = aC.a;
    gl_Position = uVP * vec4(w, 1.0);
}
"""

    /** Soft round shadow under the ball. */
    const val SHADOW_FS = """#version 300 es
precision mediump float;
in vec2 vUv;
uniform float uAlpha;
out vec4 frag;
void main() {
    float r = length(vUv);
    float a = clamp(1.0 - r, 0.0, 1.0);
    frag = vec4(0.0, 0.0, 0.0, a * a * uAlpha);
}
"""

    /**
     * uKind: 0 ring slice, 1 pillar, 2 ball, 3 additive halo, 4 danger slice, 5 goal ring.
     * uPattern: pillar style (PillarStyle) or ball style (BallStyle).
     */
    const val LIT_FS = """#version 300 es
precision highp float;
float ss(float a, float b, float x) { float t = clamp((x - a) / (b - a), 0.0, 1.0); return t * t * (3.0 - 2.0 * t); }
in vec3 vWorld;
in vec3 vNormal;
in vec3 vLocal;
in vec2 vUv;
in vec3 vInstColor;
#ifdef V_TRACK
in float vExtra;
#endif
uniform mediump int uInst;
uniform vec3 uCam;
uniform vec3 uLight;
uniform vec3 uColor;
uniform vec3 uColor2;
uniform vec3 uEmissive;
uniform vec3 uSky;
uniform vec3 uGround;
uniform vec3 uRim;
uniform vec3 uFog;
uniform vec2 uFogRange;
uniform float uAlpha;
uniform float uTime;
uniform float uGloss;
uniform float uMetal;
uniform float uSpacing;
uniform int uKind;
uniform int uPattern;
uniform float uBaseTop;
out vec4 frag;

vec3 hsv(float h, float s, float v) {
    vec3 k = clamp(abs(mod(h * 6.0 + vec3(0.0, 4.0, 2.0), 6.0) - 3.0) - 1.0, 0.0, 1.0);
    return v * mix(vec3(1.0), k, s);
}
float hash(vec2 p) { return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453); }
float noise(vec2 p) {
    vec2 i = floor(p); vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1, 0)), u.x), mix(hash(i + vec2(0, 1)), hash(i + vec2(1, 1)), u.x), u.y);
}
float hash3(vec3 p) { return fract(sin(dot(p, vec3(127.1, 311.7, 74.7))) * 43758.5453); }
float noise3(vec3 p) {
    vec3 i = floor(p); vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = mix(hash3(i), hash3(i + vec3(1, 0, 0)), f.x);
    float b = mix(hash3(i + vec3(0, 1, 0)), hash3(i + vec3(1, 1, 0)), f.x);
    float c = mix(hash3(i + vec3(0, 0, 1)), hash3(i + vec3(1, 0, 1)), f.x);
    float d = mix(hash3(i + vec3(0, 1, 1)), hash3(i + vec3(1, 1, 1)), f.x);
    return mix(mix(a, b, f.y), mix(c, d, f.y), f.z);
}
float fbm3(vec3 p) {
    float s = 0.0; float a = 0.5;
    for (int i = 0; i < 4; i++) { s += a * noise3(p); p *= 2.07; a *= 0.5; }
    return s;
}
// Truncated icosahedron (football): pentagon centres = icosahedron vertices, hexagons = dodecahedron.
const float PHI = 1.618034;
float soccerSeam(vec3 n, out float pent) {
    float b1 = -2.0; float b2 = -2.0; bool p1 = false;
    for (int k = 0; k < 32; k++) {
        vec3 d;
        if (k < 12) {
            int q = k / 4; float sa = (k % 2 == 0) ? 1.0 : -1.0; float sb = ((k / 2) % 2 == 0) ? 1.0 : -1.0;
            d = (q == 0) ? vec3(0.0, sa, sb * PHI) : (q == 1) ? vec3(sa, sb * PHI, 0.0) : vec3(sb * PHI, 0.0, sa);
        } else {
            int m = k - 12;
            if (m < 8) {
                d = vec3((m % 2 == 0) ? 1.0 : -1.0, ((m / 2) % 2 == 0) ? 1.0 : -1.0, ((m / 4) % 2 == 0) ? 1.0 : -1.0);
            } else {
                int r = m - 8; int q = r / 4; float sa = (r % 2 == 0) ? 1.0 : -1.0; float sb = ((r / 2) % 2 == 0) ? 1.0 : -1.0;
                d = (q == 0) ? vec3(0.0, sa / PHI, sb * PHI) : (q == 1) ? vec3(sa / PHI, sb * PHI, 0.0) : vec3(sb * PHI, 0.0, sa / PHI);
            }
        }
        float v = dot(n, normalize(d));
        if (v > b1) { b2 = b1; b1 = v; p1 = k < 12; } else if (v > b2) { b2 = v; }
    }
    pent = p1 ? 1.0 : 0.0;
    return b1 - b2;
}
// Rock surface for the base; returns colour, may bend the normal.
vec3 rock(vec3 rockCol, vec3 topCol, float a, float yb, inout vec3 N) {
    float n = noise(vec2(a * 6.0, yb * 0.9)) * 0.6 + noise(vec2(a * 19.0, yb * 3.3)) * 0.4;
    vec3 c = rockCol * (0.65 + 0.6 * n);
    c *= 0.9 + 0.12 * sin(yb * 5.0 + n * 5.0);
    N = normalize(N + (vec3(noise(vec2(a * 23.0, yb * 4.0)), 0.0, noise(vec2(a * 29.0 + 3.0, yb * 4.0))) - 0.5) * 0.5);
    float cover = ss(-7.0, -0.5, yb + n * 3.0) * ss(0.05, 0.55, N.y + n * 0.25);
    return mix(c, topCol * (0.85 + 0.3 * n), cover);
}

void main() {
    vec3 N = normalize(vNormal);
    vec3 V = normalize(uCam - vWorld);
    vec3 base = (uInst == 1) ? vInstColor : uColor;
    vec3 emis = uEmissive;
    float gloss = uGloss;
    float metal = uMetal;

#ifdef V_RING
    if (uKind == 3) {
        // Additive halo: brighter at grazing angles.
        float f = pow(1.0 - abs(dot(N, V)), 1.5);
        frag = vec4(uColor * (0.35 + f) * uAlpha, 1.0);
        return;
    }
#endif

#ifdef V_TRACK
    {
        if (uKind == 3) {
            // Additive glow: brighter at grazing angles.
            float f = pow(1.0 - abs(dot(N, V)), 1.5);
            frag = vec4(base * (0.35 + f) * uAlpha, 1.0);
            return;
        }
        int k = int(vExtra + 0.5);
        bool topF = N.y > 0.6;
        bool botF = N.y < -0.6;
        float sPos = -vWorld.z;
        if (k <= 2 || k == 13) {
            // Track: glossy tiles on top with glowing side edges; darker glassy sides.
            if (topF) {
                vec2 g = vec2(vWorld.x * 0.9, sPos * 0.9);
                vec2 f = abs(fract(g) - 0.5);
                float grout = max(ss(0.46, 0.5, f.x), ss(0.46, 0.5, f.y));
                float h = hash(floor(g));
                base = mix(base * (0.9 + 0.18 * h), base * 0.62, grout * 0.7);
                if (k == 13) base = mix(base, base * 1.25, ss(0.35, 0.0, length(vLocal.xz)));
                float edge = ss(0.22, 0.0, vUv.x);
                emis += uRim * edge * 1.1 + uRim * ss(0.12, 0.0, vUv.y) * 0.5;
                // A slow light sweep running along the track.
                emis += base * 0.18 * pow(0.5 + 0.5 * sin(sPos * 0.25 - uTime * 3.0), 8.0);
                gloss = 80.0;
            } else if (botF) {
                base *= 0.25;
                emis += uRim * 0.04;
            } else {
                base *= mix(0.32, 0.75, vLocal.y + 0.5);
                emis += uRim * ss(0.14, 0.0, 0.5 - vLocal.y) * 0.75;
                emis += uRim * ss(0.06, 0.0, abs(vLocal.y + 0.15)) * 0.15;
                gloss = 120.0;
            }
        } else if (k == 10) {
            // Speed pad: chevrons racing forward.
            if (topF) {
                float c = fract(sPos * 0.9 - abs(vLocal.x) * 1.4 - uTime * 3.2);
                float m = ss(0.32, 0.22, abs(c - 0.5));
                base = mix(base * 0.35, base, m);
                emis += base * (0.25 + 1.1 * m);
            } else base *= 0.5;
            gloss = 100.0;
        } else if (k == 11) {
            // Jump pad: pulsing rings.
            if (topF) {
                float r = length(vec2(vLocal.x * 2.2, vLocal.z * 1.3));
                float pz = fract(r * 2.5 - uTime * 2.2);
                float m = ss(0.25, 0.1, abs(pz - 0.5));
                emis += base * (0.3 + 1.0 * m);
                base = mix(base * 0.5, base, m);
            } else base *= 0.5;
            gloss = 100.0;
        } else if (k == 12) {
            // Finish line: checkerboard.
            float cb = mod(floor(vWorld.x * 2.0) + floor(sPos * 2.0) + floor(vWorld.y * 2.0), 2.0);
            base = mix(vec3(0.06), vec3(0.96), cb);
            emis += vec3(1.0, 0.85, 0.35) * ss(0.2, 0.0, vUv.x) * 0.8;
            gloss = 90.0;
        } else if (k == 20 || k == 22) {
            // Obstacles: glossy danger colour with hazard stripes and a pulsing glow.
            float st = step(0.5, fract((vWorld.x + vWorld.y - vWorld.z) * 1.4));
            base = mix(base, base * 0.3, st * 0.55);
            float pulse = 0.75 + 0.25 * sin(uTime * 6.0);
            emis += base * (0.28 + 0.12 * pulse);
            vec3 a = abs(vLocal);
            float e = ss(0.42, 0.5, max(min(a.x, a.y), max(min(a.y, a.z), min(a.x, a.z))));
            emis += vec3(1.0, 0.9, 0.85) * e * 0.6;
            if (k == 22) emis += base * ss(0.08, 0.0, abs(fract(vLocal.y * 4.0) - 0.5) - 0.38) * 0.8;
            gloss = 110.0;
        } else if (k == 21) {
            // Metal frames and posts.
            metal = 0.85;
            gloss = 120.0;
            base *= 0.85 + 0.15 * sin(vLocal.y * 30.0);
            emis += uRim * ss(0.46, 0.5, max(abs(vLocal.x), abs(vLocal.z))) * 0.25;
        } else if (k == 23) {
            // Support columns going down into the clouds.
            base *= 0.45;
            emis += uRim * ss(0.08, 0.0, abs(vLocal.x)) * 0.35 * ss(-0.5, 0.5, vLocal.y);
        }
    }
#endif
#ifdef V_PILLAR
    {
        // Pillar
        float a = atan(vLocal.x, vLocal.z);
        float y = vWorld.y;
        if (uPattern == 0) {
            float band = abs(fract(y / uSpacing + 0.5) - 0.5) * uSpacing;
            float line = ss(0.09, 0.0, abs(band - uSpacing * 0.32));
            emis += uColor2 * line * (0.8 + 0.2 * sin(uTime * 3.0 + y));
            base = mix(base, base * 1.6, 0.5 + 0.5 * sin(a * 3.0));
        } else if (uPattern == 1) {
            float r = sin(a * 26.0);
            N = normalize(N + vec3(cos(a), 0.0, -sin(a)) * r * 0.35);
            base = mix(base, uColor2, 0.25 + 0.25 * r);
        } else if (uPattern == 2) {
            float s = fract(a / 6.2831 * 3.0 + y * 0.18 - uTime * 0.05);
            float m = ss(0.45, 0.5, s) - ss(0.95, 1.0, s);
            base = mix(base, uColor2, m);
        } else if (uPattern == 3) {
            float q = floor((a + 3.1416) / 6.2831 * 10.0) + 0.5;
            float qa = q / 10.0 * 6.2831 - 3.1416;
            N = normalize(vec3(sin(qa), N.y + 0.25 * sin(q * 7.0 + floor(y * 0.6)), cos(qa)));
            float edge = abs(fract((a + 3.1416) / 6.2831 * 10.0) - 0.5);
            emis += uColor2 * ss(0.47, 0.5, edge) * 0.9;
            gloss = 140.0;
            metal = 0.4;
        } else if (uPattern == 4) {
            float row = floor(y * 2.2);
            float col = (a + 3.1416) / 6.2831 * 18.0 + mod(row, 2.0) * 0.5;
            float mortar = max(ss(0.42, 0.5, abs(fract(y * 2.2) - 0.5)), ss(0.44, 0.5, abs(fract(col) - 0.5)));
            base = mix(base * (0.85 + 0.3 * hash(vec2(floor(col), row))), uColor2 * 0.35, mortar);
            N = normalize(N + vec3(0.0, (fract(y * 2.2) - 0.5) * 0.4, 0.0));
        } else {
            vec2 g = vec2((a + 3.1416) / 6.2831 * 24.0, y * 2.0);
            vec2 cell = floor(g);
            float h = hash(cell);
            vec2 f = fract(g);
            float trace = (h > 0.5) ? ss(0.08, 0.0, abs(f.x - 0.5)) : ss(0.08, 0.0, abs(f.y - 0.5));
            float pulse = 0.5 + 0.5 * sin(uTime * 4.0 - y * 1.5 + h * 6.0);
            emis += uColor2 * trace * (0.35 + 0.65 * pulse) * step(0.25, h);
        }
    }
#endif
#ifdef V_RING
    {
        float r = length(vLocal.xz);
        base = mix(base * 0.82, base * 1.12, ss(1.0, 2.7, r));
        if (uKind == 4) {
            float st = sin((vUv.x * 7.0 + r * 3.0) - uTime * 6.0);
            float pulse = 0.65 + 0.35 * sin(uTime * 5.0);
            emis += uColor * (0.55 + 0.25 * st) * pulse;
            gloss = 90.0;
        }
        if (uKind == 7) {
            // Portal: a violet whirlpool.
            float sw = sin(vUv.x * 9.0 + r * 5.0 - uTime * 7.0) * 0.5 + 0.5;
            float core = ss(2.4, 1.5, r) * 0.6 + 0.4;
            base = vec3(0.12, 0.04, 0.22);
            emis += mix(vec3(0.45, 0.15, 1.0), vec3(0.2, 0.9, 1.0), sw) * (0.5 + 0.7 * sw) * core;
            gloss = 120.0;
        }
        if (uKind == 8) {
            // Glass: icy, very shiny, bright edges.
            float edge = max(ss(0.12, 0.0, vUv.x), ss(0.88, 1.0, vUv.x));
            emis += vec3(0.5, 0.75, 1.0) * (0.15 + 0.6 * edge);
            gloss = 180.0;
            metal = 0.3;
        }
        if (uKind == 5) {
            float sh = pow(0.5 + 0.5 * sin(vUv.x * 20.0 - uTime * 3.0 + r * 4.0), 6.0);
            emis += uColor * (0.25 + 0.6 * sh);
            metal = 1.0;
            gloss = 120.0;
        }
    }
#endif
#ifdef V_BASE
    {
        // The base: mountain, castle keep, temple or tech tower.
        float a = atan(vLocal.x, vLocal.z);
        float yb = vWorld.y - uBaseTop;
        float r = length(vLocal.xz);
        gloss = 18.0;
        if (N.y > 0.9 && yb > -0.05 && r < 3.6) {
            // Paved plaza with a glowing inlay circle.
            float ringN = floor(r * 1.4);
            float segN = floor((a + 3.1416) / 6.2831 * (6.0 + ringN * 7.0));
            float fr = fract(r * 1.4); float fs = fract((a + 3.1416) / 6.2831 * (6.0 + ringN * 7.0));
            float grout = max(ss(0.42, 0.5, abs(fr - 0.5)), ss(0.44, 0.5, abs(fs - 0.5)));
            vec3 tile = (uPattern == 2) ? vec3(0.12, 0.14, 0.18) : uColor * 1.15;
            base = mix(tile * (0.85 + 0.3 * hash(vec2(ringN, segN))), tile * 0.4, grout);
            emis += uRim * ss(0.07, 0.0, abs(r - 3.0)) * (0.7 + 0.3 * sin(uTime * 3.0));
            gloss = 60.0;
        } else if (uPattern == 0 || yb < -8.9) {
            base = rock(uColor, uColor2, a, yb, N);
            if (uPattern == 2 && yb < -8.9) base = rock(vec3(0.16, 0.17, 0.2), vec3(0.2, 0.22, 0.26), a, yb, N);
        } else if (uPattern == 1 || uPattern == 3) {
            // Stone (castle) or sandstone (temple) blocks with lit windows.
            float row = floor(yb * 2.2);
            float col = (a + 3.1416) / 6.2831 * 34.0 + mod(row, 2.0) * 0.5;
            float mortar = max(ss(0.40, 0.5, abs(fract(yb * 2.2) - 0.5)), ss(0.44, 0.5, abs(fract(col) - 0.5)));
            float h = hash(vec2(floor(col), row));
            vec3 stone = uColor * (0.78 + 0.35 * h);
            base = mix(stone, stone * 0.45, mortar);
            N = normalize(N + vec3(0.0, (fract(yb * 2.2) - 0.5) * 0.35, 0.0));
            vec2 wg = vec2((a + 3.1416) / 6.2831 * 9.0, (yb + 0.6) / 2.6);
            vec2 wc = floor(wg); vec2 wf = fract(wg);
            float win = step(abs(wf.x - 0.5), 0.13) * step(abs(wf.y - 0.45), 0.24) * step(0.35, hash(wc + 7.0)) * step(yb, -1.2) * step(-8.2, yb);
            float arch = step(length((wf - vec2(0.5, 0.69)) * vec2(1.0, 1.9)), 0.13) * step(0.35, hash(wc + 7.0)) * step(yb, -1.2) * step(-8.2, yb);
            float lit = max(win, arch);
            base = mix(base, vec3(0.05), lit);
            emis += uColor2 * lit * (0.9 + 0.2 * sin(uTime * 2.0 + wc.x));
            if (vLocal.y > -0.02 && N.y < 0.9) base = stone; // battlements
        } else {
            // Tech: dark metal panels with running neon lines.
            vec2 g = vec2((a + 3.1416) / 6.2831 * 24.0, yb * 1.6);
            vec2 f = fract(g);
            float seam = max(ss(0.45, 0.5, abs(f.x - 0.5)), ss(0.45, 0.5, abs(f.y - 0.5)));
            base = mix(uColor * (0.8 + 0.4 * hash(floor(g))), uColor * 0.3, seam);
            float neon = ss(0.06, 0.0, abs(f.y - 0.5)) * step(0.55, hash(vec2(floor(g.y), 3.0)));
            float run = fract(g.x * 0.08 - uTime * 0.4 + hash(vec2(floor(g.y), 9.0)));
            emis += uColor2 * neon * (0.3 + 0.9 * ss(0.8, 1.0, run));
            emis += uColor2 * seam * 0.08;
            metal = 0.6; gloss = 80.0;
        }
    }
#endif
#ifdef V_BALL
    {
        // Ball styles, from the object-space normal.
        vec3 n0 = normalize(vLocal);
        float lon = atan(n0.x, n0.z);
        if (uPattern == 2) {
            base = mix(uColor, uColor2, ss(-0.1, 0.1, sin(n0.y * 14.0)));
        } else if (uPattern == 3) {
            float seg = floor((lon + 3.1416) / 6.2831 * 6.0);
            base = (abs(n0.y) > 0.88) ? vec3(1.0) : ((mod(seg, 2.0) < 0.5) ? uColor : hsv(seg / 6.0, 0.75, 1.0));
        } else if (uPattern == 4) {
            base = hsv(fract(lon / 6.2831 + n0.y * 0.3 + uTime * 0.25), 0.8, 1.0);
            emis += base * 0.2;
        } else if (uPattern == 5) {
            emis += mix(uColor, uColor2, 0.5 + 0.5 * sin(uTime * 4.0)) * (0.55 + 0.35 * sin(uTime * 6.0));
        } else if (uPattern == 6) {
            vec2 g = vec2(lon * 4.0, n0.y * 8.0);
            vec2 cell = floor(g);
            N = normalize(N + (vec3(hash(cell), hash(cell + 3.1), hash(cell + 7.7)) - 0.5) * 0.9);
            emis += uColor2 * pow(hash(cell + floor(uTime * 6.0)), 12.0) * 2.0;
        } else if (uPattern == 7) {
            float b = noise(vec2(n0.y * 6.0, lon * 0.6)) * 0.6 + 0.4 * sin(n0.y * 18.0);
            base = mix(uColor, uColor2, ss(0.1, 0.7, b));
        } else if (uPattern == 8) {
            float spot = ss(0.80, 0.78, dot(n0, normalize(vec3(0.0, 0.25, 1.0))));
            base = mix(uColor, uColor2, 1.0 - spot);
            float eight = ss(0.93, 0.95, dot(n0, normalize(vec3(0.0, 0.25, 1.0))));
            base = mix(base, uColor, eight * 0.9);
        } else if (uPattern == 9) {
            float n = noise(vec2(lon * 2.0, n0.y * 4.0) + uTime * 0.6) * 0.7 + noise(vec2(lon * 5.0, n0.y * 9.0) - uTime) * 0.3;
            float crack = ss(0.55, 0.62, n);
            base = mix(uColor2, uColor, crack);
            emis += uColor * crack * 1.2;
        } else if (uPattern == 10) {
            float pent;
            float e = soccerSeam(n0, pent);
            base = mix(uColor, uColor2, pent);
            base = mix(base * 0.35, base, ss(0.004, 0.02, e));
        } else if (uPattern == 11) {
            base = uColor * (0.82 + 0.25 * noise3(n0 * 48.0));
            float l = min(min(abs(n0.x), abs(n0.y)), abs(abs(n0.z) - 0.72));
            base = mix(base, uColor2, ss(0.04, 0.025, l));
        } else if (uPattern == 12) {
            base = uColor * (0.8 + 0.35 * noise3(n0 * 70.0));
            float seam = abs(n0.y - 0.5 * cos(2.0 * lon) * sqrt(max(0.0, 1.0 - n0.y * n0.y)));
            base = mix(base, uColor2, ss(0.05, 0.03, seam));
        } else if (uPattern == 13) {
            float st = sin(lon * 9.0 + 2.5 * noise(vec2(lon * 3.0, n0.y * 4.0)) + n0.y * 1.5);
            base = mix(uColor, uColor2, ss(-0.15, 0.35, st) * (0.75 + 0.25 * noise(vec2(lon * 20.0, n0.y * 30.0))));
        } else if (uPattern == 14) {
            base = uColor * (0.62 + 0.5 * fbm3(n0 * 3.0));
            vec3 g = n0 * 4.5; vec3 cell = floor(g); vec3 f = fract(g) - 0.5;
            vec3 c = (vec3(hash3(cell), hash3(cell + 1.7), hash3(cell + 4.1)) - 0.5) * 0.5;
            float d = length(f - c); float rr = 0.08 + 0.14 * hash3(cell + 9.0);
            float inside = ss(rr, rr - 0.03, d); float rim = ss(rr + 0.05, rr, d) - inside;
            base *= 1.0 - inside * 0.3 + rim * 0.22;
        } else if (uPattern == 15) {
            float n = fbm3(n0 * 2.3 + 1.7);
            float land = ss(0.5, 0.54, n);
            vec3 ground = mix(uColor2, vec3(0.55, 0.42, 0.25), ss(0.58, 0.7, n));
            base = mix(uColor * (0.8 + 0.3 * n), ground, land);
            base = mix(base, vec3(0.95), ss(0.82, 0.88, abs(n0.y)));
            float cl = fbm3(n0 * 4.0 + vec3(uTime * 0.03, 0.0, 0.0));
            base = mix(base, vec3(1.0), ss(0.55, 0.72, cl) * 0.85);
            gloss = mix(90.0, 10.0, land);
        } else if (uPattern == 16) {
            vec2 q = n0.xy;
            float eyes = max(ss(0.95, 0.96, dot(n0, normalize(vec3(0.3, 0.32, 0.9)))), ss(0.95, 0.96, dot(n0, normalize(vec3(-0.3, 0.32, 0.9)))));
            float sm = ss(0.06, 0.035, abs(length(q - vec2(0.0, 0.18)) - 0.5)) * step(q.y, -0.08) * step(0.0, n0.z);
            base = mix(uColor, uColor2, max(eyes, sm));
            base = mix(base, vec3(1.0, 0.55, 0.5), ss(0.97, 0.985, dot(n0, normalize(vec3(0.52, -0.05, 0.85)))) * 0.5);
            base = mix(base, vec3(1.0, 0.55, 0.5), ss(0.97, 0.985, dot(n0, normalize(vec3(-0.52, -0.05, 0.85)))) * 0.5);
        } else if (uPattern == 17) {
            vec3 id = floor(n0 * 2.6 + 0.5);
            float h = hash3(id);
            base = hsv(fract(0.72 + h * 0.18 + uTime * 0.03), 0.55, 1.0);
            base = mix(base, uColor, 0.35);
            emis += vec3(1.0) * pow(hash3(id + floor(uTime * 5.0)), 24.0) * 2.0;
            emis += base * 0.12;
        } else if (uPattern == 18) {
            float d = dot(n0, vec3(0.0, 0.0, 1.0));
            float iris = ss(0.82, 0.84, d);
            float ang = atan(n0.y, n0.x);
            vec3 irisCol = uColor2 * (0.7 + 0.4 * sin(ang * 30.0 + noise(vec2(ang * 5.0, d * 40.0)) * 3.0));
            irisCol = mix(irisCol, uColor2 * 1.4, ss(0.9, 0.96, d));
            base = mix(uColor, irisCol, iris);
            base = mix(base, vec3(0.02), ss(0.955, 0.965, d));
            float vein = ss(0.04, 0.0, abs(noise(vec2(ang * 6.0, d * 9.0)) - 0.5)) * ss(0.6, 0.0, d);
            base = mix(base, vec3(0.9, 0.2, 0.25), vein * 0.5);
            emis += uColor2 * iris * 0.15;
            emis += vec3(1.0) * ss(0.99, 0.995, dot(n0, normalize(vec3(0.12, 0.14, 1.0))));
        } else if (uPattern == 19) {
            float neb = fbm3(n0 * 2.8 + vec3(0.0, uTime * 0.04, 0.0));
            base = uColor;
            emis += mix(uColor2, vec3(0.25, 0.45, 1.0), noise3(n0 * 2.0 + 5.0)) * ss(0.4, 0.75, neb) * 0.9;
            emis += vec3(1.0) * pow(hash3(floor(n0 * 38.0)), 40.0) * (1.5 + sin(uTime * 4.0 + hash3(floor(n0 * 38.0)) * 30.0));
        }
    }
#endif

    vec3 L = normalize(uLight);
    vec3 H = normalize(L + V);
    float diff = max(dot(N, L), 0.0);
    vec3 amb = mix(uGround, uSky, N.y * 0.5 + 0.5);
    float spec = pow(max(dot(N, H), 0.0), gloss) * (0.45 + 0.55 * metal + gloss / 300.0);
    float fres = pow(1.0 - max(dot(N, V), 0.0), 3.0);
    vec3 R = reflect(-V, N);
    vec3 env = mix(uGround * 0.6, uSky * 1.6 + vec3(0.25), ss(-0.4, 0.8, R.y));
    vec3 col = base * (amb * 0.65 + diff * 0.8);
    col = mix(col, env * base * 1.25, metal * 0.55);
    // Soft secondary light from below (bounce light from the colourful floor).
    col += base * uRim * max(-N.y, 0.0) * 0.18;
    col += vec3(spec) * mix(vec3(1.0), base + 0.3, metal);
    col += uRim * fres * 0.55;
    col += emis;

    float dist = length(uCam - vWorld);
    float fog = ss(uFogRange.x, uFogRange.y, dist);
    col = mix(col, uFog, fog);
    frag = vec4(col, uAlpha);
}
"""

    /**
     * The lit shader specialised for one kind of object. One big shader for everything made every pixel pay
     * for the most complex path (GPU registers); each variant only contains its own code.
     */
    fun lit(variant: String): String = LIT_FS.replaceFirst("#version 300 es\n", "#version 300 es\n#define $variant 1\n")

    // ------------------------------------------------------------------ paint splat

    const val SPLAT_FS = """#version 300 es
precision mediump float;
float ss(float a, float b, float x) { float t = clamp((x - a) / (b - a), 0.0, 1.0); return t * t * (3.0 - 2.0 * t); }
in vec3 vWorld;
in vec3 vNormal;
in vec3 vLocal;
in vec2 vUv;
uniform vec3 uColor;
uniform float uAlpha;
uniform float uSeed;
out vec4 frag;
void main() {
    vec2 p = vUv;
    float r = length(p);
    float a = atan(p.y, p.x);
    float edge = 0.62 + 0.12 * sin(a * 5.0 + uSeed) + 0.07 * sin(a * 11.0 + uSeed * 2.3);
    float body = ss(edge, edge - 0.06, r);
    // A few droplets around the splat.
    float drops = 0.0;
    for (int k = 0; k < 5; k++) {
        float ka = uSeed * 1.7 + float(k) * 1.31;
        vec2 c = vec2(cos(ka), sin(ka)) * (0.78 + 0.12 * sin(ka * 3.0));
        drops = max(drops, ss(0.09, 0.06, length(p - c)));
    }
    float m = max(body, drops);
    vec3 col = uColor * (0.85 + 0.25 * (1.0 - r));
    frag = vec4(col, m * uAlpha);
}
"""

    // ------------------------------------------------------------------ background

    const val BG_VS = """#version 300 es
layout(location=0) in vec3 aPos;
layout(location=2) in vec2 aUv;
out vec2 vUv;
void main() { vUv = aUv; gl_Position = vec4(aPos.xy, 0.9999, 1.0); }
"""

    /**
     * Sky and scenery. [uProg] is how far down the tower the camera is (0 top, 1 bottom): the land
     * rises and the cloud sea passes by as the ball descends, so the tower feels high above the world.
     */
    const val BG_FS = """#version 300 es
precision highp float;
float ss(float a, float b, float x) { float t = clamp((x - a) / (b - a), 0.0, 1.0); return t * t * (3.0 - 2.0 * t); }
in vec2 vUv;
uniform vec3 uTop;
uniform vec3 uBottom;
uniform vec3 uA1;
uniform vec3 uA2;
uniform vec3 uA3;
uniform vec3 uLand1;
uniform vec3 uLand2;
uniform vec3 uSun;
uniform vec3 uCloud;
uniform float uTime;
uniform float uProg;
uniform float uAspect;
uniform float uStars;
uniform int uScene;
out vec4 frag;
float hash1(float x) { return fract(sin(x * 127.1) * 43758.5453); }
float hash(vec2 p) { return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453); }
float vnoise(float x) { float i = floor(x); float f = fract(x); f = f * f * (3.0 - 2.0 * f); return mix(hash1(i), hash1(i + 1.0), f); }
float fbm1(float x) { float s = 0.0; float a = 0.5; for (int i = 0; i < 4; i++) { s += a * vnoise(x); x = x * 2.03 + 1.7; a *= 0.5; } return s; }
float noise2(vec2 p) {
    vec2 i = floor(p); vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1, 0)), u.x), mix(hash(i + vec2(0, 1)), hash(i + vec2(1, 1)), u.x), u.y);
}
float fbm2(vec2 p) { float s = 0.0; float a = 0.5; for (int i = 0; i < 4; i++) { s += a * noise2(p); p = p * 2.1 + 3.1; a *= 0.5; } return s; }
float ridge(float x) { return 1.0 - abs(vnoise(x) * 2.0 - 1.0); }

// Height of the skyline (above its horizon) for layer L at x.
float shapeH(float x, float L) {
    float s = L * 17.3;
    if (uScene == 0) {
        float h = 0.0; float a = 0.15; float f = 2.0;
        for (int i = 0; i < 4; i++) { h += a * ridge(x * f + s); f *= 2.1; a *= 0.45; }
        return h + 0.02;
    } else if (uScene == 1) {
        float c = floor(x * 13.0 + s);
        float tall = step(0.85, hash1(c + 3.0));
        return 0.04 + 0.13 * hash1(c) * (0.6 + 0.4 * hash1(c + 9.0)) + tall * 0.1;
    } else if (uScene == 2) {
        return 0.07 + 0.045 * sin(x * 3.1 + s) + 0.035 * sin(x * 7.3 + s * 2.0) + 0.03 * fbm1(x * 24.0 + s);
    } else if (uScene == 3) {
        float h = 0.05 + 0.1 * fbm1(x * 4.0 + s);
        float cx = (0.3 + L * 0.35) * uAspect;
        h = max(h, min(0.30, 0.34 - abs(x - cx) * 0.75));
        return h;
    } else if (uScene == 5) {
        float h = 0.05 + 0.035 * sin(x * 2.3 + s) + 0.02 * sin(x * 5.1 + s * 1.7);
        float u = x * 0.9 + s * 0.13;
        float pc = floor(u); float px = fract(u);
        if (hash1(pc) > 0.4) h = max(h, 0.06 + 0.13 * hash1(pc + 3.0) - abs(px - 0.5) * 0.55);
        return h;
    } else if (uScene == 6) {
        return 0.004 + max(0.0, fbm1(x * 3.0 + s) - 0.56) * 0.4;
    } else {
        float h = 0.05 + 0.04 * sin(x * 2.7 + s) + 0.02 * fbm1(x * 9.0 + s);
        float u = x * 4.0 + s;
        float cell = floor(u); float fx = fract(u);
        if (hash1(cell) > 0.45) {
            float tw = 0.09 + 0.08 * hash1(cell + 1.0);
            if (abs(fx - 0.5) < 0.1) h = max(h, tw + 0.07 - abs(fx - 0.5) * 0.7);
            else if (abs(fx - 0.5) < 0.3) h = max(h, tw * 0.6 + step(0.5, fract(fx * 22.0)) * 0.012);
        }
        return h;
    }
}

void main() {
    vec2 uv = vUv;
    float x = uv.x * uAspect;
    float p = uProg;
    vec3 sky = mix(uBottom, uTop, ss(0.0, 1.0, uv.y));
    vec3 col = sky;
    float t = uTime * 0.05;
    // Soft colour glows in the sky
    vec2 q = (uv - vec2(0.2 + 0.1 * sin(t), 0.9)) * vec2(uAspect, 1.0);
    col += uA1 * ss(0.8, 0.0, length(q)) * 0.35;
    q = (uv - vec2(0.85 + 0.08 * cos(t * 1.3), 0.6)) * vec2(uAspect, 1.0);
    col += uA2 * ss(0.6, 0.0, length(q)) * 0.22;
    if (uStars > 0.5) {
        vec2 g = (uv + vec2(0.0, p * 0.15)) * vec2(uAspect, 1.0) * 60.0;
        vec2 cell = floor(g);
        float h = hash(cell);
        float d = length(fract(g) - vec2(hash(cell + 1.3), hash(cell + 2.1)));
        float tw = 0.5 + 0.5 * sin(uTime * (1.0 + h * 3.0) + h * 20.0);
        col += vec3(ss(0.08, 0.0, d) * step(0.93, h) * tw) * ss(0.2, 0.6, uv.y);
    }
    // Aurora curtains (peaks with stars)
    if (uScene == 0 && uStars > 0.5) {
        float w = sin(x * 3.0 + uTime * 0.2 + 2.0 * fbm1(x * 2.0 + uTime * 0.05));
        float band = ss(0.25, 0.0, abs(uv.y - 0.75 - 0.06 * w));
        float rays = 0.6 + 0.4 * fbm1(x * 30.0 + uTime * 0.3);
        col += mix(uA1, uA2, 0.5 + 0.5 * w) * band * rays * 0.55;
    }
    // Sun (or moon) with a wide glow
    vec2 sp = vec2(0.76, 0.8 - 0.1 * p);
    float sd = length((uv - sp) * vec2(uAspect, 1.0));
    col += uSun * (ss(0.048, 0.042, sd) * 0.95 + pow(max(0.0, 1.0 - sd / 0.55), 3.0) * 0.4 + ss(0.12, 0.0, sd) * 0.2);

    if (uScene == 4) {
        // Space: a ringed planet rising, a small moon, nebula.
        vec2 pc = vec2(0.28 * uAspect, mix(-0.32, 0.18, p));
        vec2 d = vec2(x, uv.y) - pc;
        float r = length(d);
        float R = 0.34;
        float ringE = abs(length(d * vec2(1.0, 3.4)) - 0.55);
        float ringM = ss(0.07, 0.04, ringE) * (0.6 + 0.4 * sin(length(d * vec2(1.0, 3.4)) * 120.0));
        bool front = d.y < 0.0;
        if (!front) col = mix(col, uA3 * 0.9 + 0.1, ringM * 0.7);
        if (r < R) {
            float band = sin(d.y * 40.0 + fbm1(d.x * 6.0) * 4.0);
            vec3 pl = mix(uLand1, uLand2, 0.5 + 0.5 * band);
            float sh = ss(-R, R, dot(d, normalize(vec2(-0.6, 0.8))));
            col = pl * (0.25 + 0.9 * sh);
            col += uA2 * pow(r / R, 6.0) * 0.5;
        }
        if (front) col = mix(col, uA3 * 0.9 + 0.1, ringM * 0.7);
        vec2 mc = vec2(0.82 * uAspect, mix(0.35, 0.62, p));
        float mr = length(vec2(x, uv.y) - mc);
        if (mr < 0.05) col = mix(uSun * 0.8, uLand2, ss(-0.05, 0.05, x - mc.x + uv.y - mc.y));
        float neb = fbm2(vec2(x * 1.8, uv.y * 2.4 + p) + uTime * 0.01);
        col += mix(uA1, uA3, neb) * ss(0.45, 0.8, neb) * 0.35;
    } else {
        // Far layer
        float hz1 = mix(-0.03, 0.34, p);
        float h1 = hz1 + shapeH(x * 0.8, 1.0);
        vec3 far = mix(uLand1, sky, 0.45);
        if (uv.y < h1) {
            float slope = 0.0;
            float lit = (uScene == 1 || uScene == 7) ? 0.0 : clamp(-slope * 10.0, -1.0, 1.0) * 0.6;
            col = mix(far * 0.85, far, ss(h1 - 0.3, h1, uv.y)) * (1.0 + 0.18 * lit);
            if (uScene == 0) {
                float line = hz1 + 0.14 + 0.06 * fbm1(x * 22.0) - 0.03 * lit;
                float cap = ss(line, line + 0.01, uv.y);
                col = mix(col, mix(vec3(1.0), uSun, 0.2) * (0.85 + 0.25 * lit), cap * 0.9);
            }
            if (uScene == 1) {
                vec2 wg = vec2(x * 0.8 * 13.0 * 5.0, uv.y * 120.0);
                float lit = step(0.72, hash(floor(wg))) * step(0.3, fract(wg.x)) * step(0.35, fract(wg.y));
                col += uA2 * lit * 0.5;
            }
            if (uScene == 6) {
                float sp2 = pow(noise2(vec2(x * 30.0, uv.y * 260.0 + uTime * 2.0)), 10.0);
                col = mix(uLand1, sky, 0.35) + uSun * sp2 * 1.5 * ss(hz1 - 0.3, hz1, uv.y);
                if (uv.y > hz1 + 0.006) col = far * 0.9;
            }
            if (uScene == 3) col += vec3(1.0, 0.3, 0.05) * ss(0.25, 0.0, h1 - uv.y) * 0.08;
        }
        // Haze where the land meets the sky
        col = mix(col, mix(sky, uCloud, 0.4), ss(0.07, 0.0, abs(uv.y - hz1)) * 0.5);
        if (uScene == 3) {
            float plume = exp(-abs(x - 0.65 * uAspect / 0.8) * 9.0) * ss(0.4, 0.0, uv.y - (hz1 + 0.3)) * step(hz1 + 0.27, uv.y);
            col += vec3(1.0, 0.35, 0.08) * plume * (0.5 + 0.2 * sin(uTime * 3.0));
        }
        // Sea of clouds that rises past as the camera descends
        float cy = mix(0.08, 1.4, p);
        float dy = uv.y - cy;
        vec2 cp = vec2(x * 1.4 + uTime * 0.012, dy * 3.0);
        float cd = fbm2(cp * vec2(1.0, 1.8));
        // Billowy top edge, flatter bottom.
        float edgeTop = 0.07 + 0.12 * (noise2(vec2(x * 2.2 + uTime * 0.01, 1.0)) * 0.65 + noise2(vec2(x * 5.1, 3.0)) * 0.35);
        float edgeBot = -0.1 - 0.05 * noise2(vec2(x * 1.7, 5.0));
        float inside = ss(edgeTop, edgeTop - 0.06, dy) * ss(edgeBot, edgeBot + 0.08, dy);
        float cm = inside * ss(0.3, 0.55, cd + 0.15);
        vec3 cc = mix(uCloud * 0.7, mix(uCloud, uSun, 0.3) * 1.08, ss(edgeBot, edgeTop, dy) * (0.55 + 0.45 * cd));
        col = mix(col, cc, cm * 0.95);
        // Near layer
        float hz2 = mix(-0.32, 0.18, p);
        float h2 = hz2 + shapeH(x * 1.05 + 3.7, 2.0) * 1.25;
        if (uv.y < h2) {
            vec3 nearC = mix(uLand2, sky, 0.12);
            float slope2 = 0.0;
            float lit2 = (uScene == 1 || uScene == 7) ? 0.0 : clamp(-slope2 * 10.0, -1.0, 1.0) * 0.6;
            col = mix(nearC * 0.75, nearC, ss(h2 - 0.25, h2, uv.y)) * (1.0 + 0.2 * lit2);
            if (uScene == 0) {
                float line2 = hz2 + 0.18 + 0.07 * fbm1(x * 18.0 + 5.0) - 0.03 * lit2;
                float cap2 = ss(line2, line2 + 0.01, uv.y);
                col = mix(col, mix(vec3(0.92), uSun, 0.2) * (0.8 + 0.25 * lit2), cap2 * 0.85);
            }
            if (uScene == 1) {
                vec2 wg = vec2((x * 1.05 + 3.7) * 13.0 * 6.0, uv.y * 150.0);
                float lit = step(0.66, hash(floor(wg) + 5.0)) * step(0.3, fract(wg.x)) * step(0.35, fract(wg.y));
                col += mix(uA1, uA2, hash(floor(wg))) * lit * 0.8;
            }
            if (uScene == 7 || uScene == 2) {
                // little warm lights on the hills / castle windows
                vec2 wg = vec2((x * 1.05) * 150.0, uv.y * 260.0);
                col += uSun * step(0.992, hash(floor(wg))) * 0.9 * step(uv.y, h2 - 0.01);
            }
            if (uScene == 6) {
                float sp2 = pow(noise2(vec2(x * 40.0, uv.y * 300.0 + uTime * 3.0)), 10.0);
                if (uv.y < hz2 + 0.004) col = mix(uLand2, sky, 0.2) + uSun * sp2;
            }
            if (uScene == 3) col += vec3(1.0, 0.3, 0.05) * ss(0.12, 0.0, h2 - uv.y) * 0.15 * (0.6 + 0.4 * fbm1(x * 30.0));
        }
        col = mix(col, mix(sky, uCloud, 0.3), ss(0.06, 0.0, abs(uv.y - hz2)) * 0.35);
    }
    vec2 v = uv - 0.5;
    col *= 1.0 - dot(v, v) * 0.8;
    frag = vec4(col, 1.0);
}
"""

    /** Light sky (no scenery): gradient, soft colour glows, stars. Used when detailed scenery is off. */
    const val SKY_FS = """#version 300 es
precision mediump float;
float ss(float a, float b, float x) { float t = clamp((x - a) / (b - a), 0.0, 1.0); return t * t * (3.0 - 2.0 * t); }
in vec2 vUv;
uniform vec3 uTop;
uniform vec3 uBottom;
uniform vec3 uA1;
uniform vec3 uA2;
uniform vec3 uA3;
uniform float uTime;
uniform float uAspect;
uniform float uStars;
out vec4 frag;
float hash(vec2 p) { return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453); }
void main() {
    vec2 uv = vUv;
    vec3 col = mix(uBottom, uTop, uv.y);
    float t = uTime * 0.07;
    col += uA1 * ss(0.75, 0.0, length((uv - vec2(0.25 + 0.15 * sin(t), 0.85)) * vec2(uAspect, 1.0))) * 0.4;
    col += uA2 * ss(0.6, 0.0, length((uv - vec2(0.85 + 0.1 * cos(t * 1.3), 0.45)) * vec2(uAspect, 1.0))) * 0.3;
    col += uA3 * ss(0.7, 0.0, length((uv - vec2(0.3, 0.05)) * vec2(uAspect, 1.0))) * 0.35;
    if (uStars > 0.5) {
        vec2 g = uv * vec2(uAspect, 1.0) * 60.0;
        vec2 cell = floor(g);
        float h = hash(cell);
        float d = length(fract(g) - vec2(hash(cell + 1.3), hash(cell + 2.1)));
        col += vec3(ss(0.08, 0.0, d) * step(0.93, h) * (0.5 + 0.5 * sin(uTime * (1.0 + h * 3.0) + h * 20.0)));
    }
    vec2 q = uv - 0.5;
    col *= 1.0 - dot(q, q) * 0.9;
    frag = vec4(col, 1.0);
}
"""

    /** Stretches the small sky texture over the screen. */
    const val BLIT_FS = """#version 300 es
precision mediump float;
in vec2 vUv;
uniform sampler2D uTex;
out vec4 frag;
void main() { frag = vec4(texture(uTex, vUv).rgb, 1.0); }
"""

    /** Billboard clouds drifting around the tower (alpha blended). */
    const val CLOUD_FS = """#version 300 es
precision mediump float;
float ss(float a, float b, float x) { float t = clamp((x - a) / (b - a), 0.0, 1.0); return t * t * (3.0 - 2.0 * t); }
in vec3 vWorld;
in vec3 vNormal;
in vec3 vLocal;
in vec2 vUv;
uniform vec3 uColor;
uniform vec3 uColor2;
uniform float uAlpha;
uniform float uSeed;
out vec4 frag;
void main() {
    vec2 p = vUv * vec2(1.0, 1.7);
    float d = 0.0;
    for (int k = 0; k < 6; k++) {
        float a = uSeed * 2.3 + float(k) * 1.9;
        vec2 c = vec2(sin(a) * 0.55, cos(a * 1.3) * 0.25 + 0.1);
        float r = 0.32 + 0.16 * fract(sin(a * 12.7) * 43.1);
        d = max(d, ss(r, r * 0.35, length(p - c)));
    }
    d *= ss(-0.55, -0.15, p.y);
    vec3 col = mix(uColor2, uColor, ss(-0.5, 0.6, p.y));
    frag = vec4(col, d * uAlpha);
}
"""

    // ------------------------------------------------------------------ glowing particles

    const val PT_VS = """#version 300 es
layout(location=0) in vec3 aPos;
layout(location=1) in vec4 aColor;
layout(location=2) in float aSize;
uniform mat4 uVP;
uniform float uScale;
out vec4 vColor;
void main() {
    vec4 p = uVP * vec4(aPos, 1.0);
    gl_Position = p;
    gl_PointSize = clamp(aSize * uScale / max(p.w, 0.1), 1.0, 256.0);
    vColor = aColor;
}
"""

    const val PT_FS = """#version 300 es
precision mediump float;
float ss(float a, float b, float x) { float t = clamp((x - a) / (b - a), 0.0, 1.0); return t * t * (3.0 - 2.0 * t); }
in vec4 vColor;
out vec4 frag;
void main() {
    vec2 p = gl_PointCoord * 2.0 - 1.0;
    float d = dot(p, p);
    float core = ss(1.0, 0.0, d);
    float a = core * core;
    frag = vec4(vColor.rgb * (a + ss(0.15, 0.0, d) * 0.6) * vColor.a, 1.0);
}
"""
}
