package com.farias.prismaroll.gl

import androidx.annotation.StringRes
import com.farias.prismaroll.R

/** Colour as 3 floats (0..1) from 0xRRGGBB. */
fun rgb(c: Long): FloatArray = floatArrayOf(((c shr 16) and 0xFF) / 255f, ((c shr 8) and 0xFF) / 255f, (c and 0xFF) / 255f)

/** Pillar surface styles, implemented in the shader. */
object PillarStyle {
    const val NEON_BANDS = 0
    const val RIBS = 1
    const val SPIRAL = 2
    const val CRYSTAL = 3
    const val BRICKS = 4
    const val CIRCUIT = 5
}

/** Scenery painted behind the tower (background shader). */
object Scene {
    const val PEAKS = 0
    const val CITY = 1
    const val HILLS = 2
    const val VOLCANO = 3
    const val SPACE = 4
    const val DUNES = 5
    const val SEA = 6
    const val CASTLE = 7
}

/** What the tower stands on at the bottom. */
object BaseKind {
    const val MOUNTAIN = 0
    const val CASTLE = 1
    const val ASTEROID = 2
}

/** Surface of the base (shader). */
object BaseStyle {
    const val ROCK = 0
    const val STONE = 1
    const val TECH = 2
    const val SAND = 3
}

/** Little things floating in the air around the tower. */
object Ambient {
    const val NONE = 0
    const val SNOW = 1
    const val PETALS = 2
    const val EMBERS = 3
    const val FIREFLIES = 4
    const val SPARKLES = 5
}

/** The world around a tower: scenery, the base it stands on, clouds and air particles. */
class Env(
    val scene: Int,
    val baseKind: Int,
    val baseStyle: Int,
    rock: Long,
    top: Long,
    land1: Long,
    land2: Long,
    sun: Long,
    cloud: Long,
    val ambient: Int = Ambient.NONE,
    ambientColor: Long = 0xFFFFFF,
) {
    val rock = rgb(rock)
    val top = rgb(top)
    val land1 = rgb(land1)
    val land2 = rgb(land2)
    val sun = rgb(sun)
    val cloud = rgb(cloud)
    val ambientColor = rgb(ambientColor)
}

/** A tower look: background, pillar, rings, danger slices and the goal. */
class TowerLook(
    val id: String,
    @StringRes val nameRes: Int,
    val price: Int,
    bgTop: Long,
    bgBottom: Long,
    aurora: List<Long>,
    pillar: Long,
    pillar2: Long,
    val pillarStyle: Int,
    ringA: Long,
    ringB: Long,
    danger: Long,
    rim: Long,
    val stars: Boolean = false,
    val metalRings: Float = 0f,
    val env: Env = Env(Scene.PEAKS, BaseKind.MOUNTAIN, BaseStyle.ROCK, 0x4B5563, 0xE5E7EB, 0x3B3363, 0x1E1840, 0xFDE68A, 0xE9E3FF),
) {
    val bgTop = rgb(bgTop)
    val bgBottom = rgb(bgBottom)
    val aurora = aurora.map { rgb(it) }
    val pillar = rgb(pillar)
    val pillar2 = rgb(pillar2)
    val ringA = rgb(ringA)
    val ringB = rgb(ringB)
    val danger = rgb(danger)
    val rim = rgb(rim)
    /** For UI previews (ARGB ints). */
    val uiColors: List<Long> = listOf(bgTop, bgBottom, pillar, ringA, ringB, danger)
}

/** Ball surface styles, implemented in the shader. */
object BallStyle {
    const val PEARL = 0
    const val METAL = 1
    const val STRIPES = 2
    const val BEACH = 3
    const val RAINBOW = 4
    const val NEON = 5
    const val DISCO = 6
    const val PLANET = 7
    const val EIGHT = 8
    const val LAVA = 9
    const val SOCCER = 10
    const val BASKET = 11
    const val TENNIS = 12
    const val MELON = 13
    const val MOON = 14
    const val EARTH = 15
    const val SMILEY = 16
    const val GEM = 17
    const val EYE = 18
    const val GALAXY = 19
}

class BallLook(
    val id: String,
    @StringRes val nameRes: Int,
    val price: Int,
    c1: Long,
    c2: Long,
    val style: Int,
    trail: Long,
    val gloss: Float = 60f,
    val metal: Float = 0f,
) {
    val c1 = rgb(c1)
    val c2 = rgb(c2)
    val trail = rgb(trail)
    val uiColors: List<Long> = listOf(c1, c2, trail)
}

enum class Rarity { COMMON, RARE, EPIC, LEGENDARY }

fun rarityOf(price: Int): Rarity = when {
    price < 600 -> Rarity.COMMON
    price < 1500 -> Rarity.RARE
    price < 3000 -> Rarity.EPIC
    else -> Rarity.LEGENDARY
}

object Looks {
    val towers = listOf(
        TowerLook(
            "neon", R.string.tower_neon, 0,
            0x0A0620, 0x1A0B3D, listOf(0x7C3AED, 0x06B6D4, 0xDB2777),
            0x2B1B5E, 0x22D3EE, PillarStyle.NEON_BANDS,
            0x22D3EE, 0xA855F7, 0xFF2D55, 0xE879F9,
            env = Env(Scene.CITY, BaseKind.CASTLE, BaseStyle.TECH, 0x1E1B3A, 0x22D3EE, 0x2A1F5C, 0x140C30, 0xF0ABFC, 0x6D4AB8, Ambient.SPARKLES, 0xE879F9),
        ),
        TowerLook(
            "ocean", R.string.tower_ocean, 400,
            0x0B3A5C, 0x05223A, listOf(0x0EA5E9, 0x14B8A6, 0x6366F1),
            0x0B4F6C, 0x7DD3FC, PillarStyle.RIBS,
            0x2DD4BF, 0x38BDF8, 0xFF5A5F, 0x99F6E4,
            env = Env(Scene.SEA, BaseKind.MOUNTAIN, BaseStyle.ROCK, 0x5B5247, 0x4ADE80, 0x1E6E8C, 0x0A3D5A, 0xFFF3C4, 0xE0F7FF),
        ),
        TowerLook(
            "candy", R.string.tower_candy, 700,
            0x7C2D6B, 0x3B1238, listOf(0xF472B6, 0xC4B5FD, 0xFDBA74),
            0xFFF1F7, 0xFF7EB6, PillarStyle.SPIRAL,
            0xFF9EC4, 0x9BE8D8, 0x8B5CF6, 0xFFFFFF,
            env = Env(Scene.HILLS, BaseKind.MOUNTAIN, BaseStyle.ROCK, 0xC2577F, 0xFFF5FA, 0xE38BB8, 0x9B3D74, 0xFFFFFF, 0xFFE4F1, Ambient.SPARKLES, 0xFFC4E1),
        ),
        TowerLook(
            "lava", R.string.tower_lava, 1000,
            0x2E0A04, 0x120303, listOf(0xDC2626, 0xEA580C, 0xBE185D),
            0x1C1412, 0xFF6A1A, PillarStyle.CRYSTAL,
            0x3A2A26, 0x55362C, 0xFF7A00, 0xFF8A3D,
            env = Env(Scene.VOLCANO, BaseKind.MOUNTAIN, BaseStyle.ROCK, 0x2A1E1A, 0xFF5A1A, 0x3A1610, 0x1A0806, 0xFF7A3D, 0x5A2A22, Ambient.EMBERS, 0xFF7A1A),
        ),
        TowerLook(
            "galaxy", R.string.tower_galaxy, 1400,
            0x0D0B30, 0x02010A, listOf(0x4F46E5, 0x9333EA, 0x0891B2),
            0x1E1B4B, 0xC4B5FD, PillarStyle.CRYSTAL,
            0x818CF8, 0xF0ABFC, 0xFF3B6B, 0xA5B4FC, stars = true,
            env = Env(Scene.SPACE, BaseKind.ASTEROID, BaseStyle.ROCK, 0x3F3A52, 0xA78BFA, 0x6D28D9, 0x1E1B4B, 0xFDE68A, 0x7C3AED, Ambient.SPARKLES, 0xC4B5FD),
        ),
        TowerLook(
            "gold", R.string.tower_gold, 2000,
            0x2A1D08, 0x0B0904, listOf(0xCA8A04, 0xB45309, 0x92400E),
            0x2A2110, 0xFACC15, PillarStyle.RIBS,
            0xF5C542, 0xE8A317, 0x111111, 0xFFE8A3, stars = true, metalRings = 0.8f,
            env = Env(Scene.DUNES, BaseKind.CASTLE, BaseStyle.SAND, 0xB98A4B, 0xFACC15, 0x7A5520, 0x3D2A0E, 0xFFE8A3, 0xFFE1A8, Ambient.SPARKLES, 0xFDE68A),
        ),
        TowerLook(
            "ice", R.string.tower_ice, 2600,
            0x3B6E96, 0x123250, listOf(0x7DD3FC, 0xE0F2FE, 0x818CF8),
            0xCFE9FF, 0x7DD3FC, PillarStyle.CRYSTAL,
            0xE0F2FE, 0x93C5FD, 0x3B82F6, 0xFFFFFF, metalRings = 0.3f,
            env = Env(Scene.PEAKS, BaseKind.MOUNTAIN, BaseStyle.ROCK, 0x56677D, 0xF8FBFF, 0x7FA3C4, 0x3D5A7A, 0xFFFFFF, 0xF1F7FF, Ambient.SNOW, 0xFFFFFF),
        ),
        TowerLook(
            "jungle", R.string.tower_jungle, 3200,
            0x0F4A2E, 0x04140C, listOf(0x16A34A, 0x84CC16, 0x0D9488),
            0x3F2A17, 0x86EFAC, PillarStyle.BRICKS,
            0x4ADE80, 0xA3E635, 0xF97316, 0xBBF7D0,
            env = Env(Scene.HILLS, BaseKind.MOUNTAIN, BaseStyle.ROCK, 0x4A3B2A, 0x3FA34D, 0x1F6B3A, 0x0B3A20, 0xFFF3B0, 0xCFF5DC, Ambient.FIREFLIES, 0xD9F99D),
        ),
        TowerLook(
            "sakura", R.string.tower_sakura, 4000,
            0x8A4A72, 0x3A1A30, listOf(0xF9A8D4, 0xFBCFE8, 0xC084FC),
            0x3B2230, 0xFBCFE8, PillarStyle.SPIRAL,
            0xFFFFFF, 0xF9A8D4, 0xBE123C, 0xFFE4F1, stars = true,
            env = Env(Scene.CASTLE, BaseKind.CASTLE, BaseStyle.STONE, 0x6B5563, 0xF9A8D4, 0xB06A93, 0x5A2A4A, 0xFFE4F1, 0xFFE4F1, Ambient.PETALS, 0xFFB7D5),
        ),
        TowerLook(
            "cyber", R.string.tower_cyber, 5000,
            0x06171A, 0x020607, listOf(0x22C55E, 0x06B6D4, 0x84CC16),
            0x061316, 0x39FF88, PillarStyle.CIRCUIT,
            0x0F2A2E, 0x123A33, 0xFF2BD6, 0x39FF88, metalRings = 0.5f,
            env = Env(Scene.CITY, BaseKind.CASTLE, BaseStyle.TECH, 0x0B1F22, 0x39FF88, 0x0C3A3A, 0x041A1C, 0x39FF88, 0x1C4A44, Ambient.SPARKLES, 0x39FF88),
        ),
        TowerLook(
            "castle", R.string.tower_castle, 3800,
            0x5B6FA8, 0x23294A, listOf(0x6366F1, 0xF59E0B, 0x0EA5E9),
            0x6B6F7A, 0xD1D5DB, PillarStyle.BRICKS,
            0x60A5FA, 0xFBBF24, 0xDC2626, 0xFDE68A,
            env = Env(Scene.CASTLE, BaseKind.CASTLE, BaseStyle.STONE, 0x7C7F88, 0x4D9A4A, 0x5161A0, 0x2B3466, 0xFFE8A3, 0xF4F1FF),
        ),
        TowerLook(
            "desert", R.string.tower_desert, 2300,
            0xF59E5B, 0x6B2E1A, listOf(0xF97316, 0xFBBF24, 0xDB2777),
            0x8A5A33, 0xFCD34D, PillarStyle.BRICKS,
            0xFDBA74, 0xF59E0B, 0x7C2D12, 0xFFE8C2,
            env = Env(Scene.DUNES, BaseKind.MOUNTAIN, BaseStyle.SAND, 0xB0703C, 0xF2C27A, 0xC9733B, 0x8A3F1E, 0xFFF1C4, 0xFFE3C2),
        ),
        TowerLook(
            "aurora", R.string.tower_aurora, 4500,
            0x0B2A3A, 0x030B14, listOf(0x22D3EE, 0x34D399, 0xA855F7),
            0x16324A, 0x5EEAD4, PillarStyle.CRYSTAL,
            0x5EEAD4, 0xA78BFA, 0xF43F5E, 0xA7F3D0, stars = true, metalRings = 0.4f,
            env = Env(Scene.PEAKS, BaseKind.MOUNTAIN, BaseStyle.ROCK, 0x3A4A5E, 0xEAF6FF, 0x1E3A50, 0x0C1E2E, 0xD1FAE5, 0x9DB8D0, Ambient.SNOW, 0xD1FAE5),
        ),
    ).sortedBy { it.price }

    val balls = listOf(
        BallLook("pearl", R.string.ball_pearl, 0, 0xF8F7FF, 0xD9D4FF, BallStyle.PEARL, 0xE879F9),
        BallLook("ruby", R.string.ball_ruby, 250, 0xE11D48, 0xFF8FAB, BallStyle.PEARL, 0xFF4D6D, gloss = 90f),
        BallLook("sapphire", R.string.ball_sapphire, 250, 0x2563EB, 0x93C5FD, BallStyle.PEARL, 0x60A5FA, gloss = 90f),
        BallLook("beach", R.string.ball_beach, 400, 0xFFFFFF, 0xFF4D6D, BallStyle.BEACH, 0xFACC15),
        BallLook("candy", R.string.ball_candy, 500, 0xFF7EB6, 0xFFFFFF, BallStyle.STRIPES, 0xFF7EB6),
        BallLook("gold", R.string.ball_gold, 800, 0xFACC15, 0xB45309, BallStyle.METAL, 0xFDE047, gloss = 120f, metal = 1f),
        BallLook("chrome", R.string.ball_chrome, 900, 0xE5E7EB, 0x6B7280, BallStyle.METAL, 0xBAE6FD, gloss = 140f, metal = 1f),
        BallLook("neon", R.string.ball_neon, 1100, 0x39FF88, 0x22D3EE, BallStyle.NEON, 0x39FF88),
        BallLook("rainbow", R.string.ball_rainbow, 1400, 0xFF0000, 0x0000FF, BallStyle.RAINBOW, 0xF0ABFC),
        BallLook("disco", R.string.ball_disco, 1700, 0xD1D5DB, 0xF0ABFC, BallStyle.DISCO, 0xC4B5FD, gloss = 160f, metal = 0.9f),
        BallLook("planet", R.string.ball_planet, 2000, 0xF59E0B, 0x7C2D12, BallStyle.PLANET, 0xFDBA74),
        BallLook("eight", R.string.ball_eight, 2400, 0x0B0B0F, 0xFFFFFF, BallStyle.EIGHT, 0x94A3B8, gloss = 120f),
        BallLook("lava", R.string.ball_lava, 3000, 0xFF4500, 0x1C0A05, BallStyle.LAVA, 0xFF7A00),
        BallLook("tennis", R.string.ball_tennis, 900, 0xD4F13A, 0xFFFFFF, BallStyle.TENNIS, 0xD9F99D, gloss = 12f),
        BallLook("melon", R.string.ball_melon, 1000, 0x14532D, 0x4ADE80, BallStyle.MELON, 0xFF4D6D, gloss = 70f),
        BallLook("soccer", R.string.ball_soccer, 1200, 0xF8FAFC, 0x111827, BallStyle.SOCCER, 0xFFFFFF, gloss = 50f),
        BallLook("basket", R.string.ball_basket, 1300, 0xE8701A, 0x1A1A1A, BallStyle.BASKET, 0xFB923C, gloss = 25f),
        BallLook("smiley", R.string.ball_smiley, 1500, 0xFFD43B, 0x2A1A00, BallStyle.SMILEY, 0xFDE047, gloss = 80f),
        BallLook("moon", R.string.ball_moon, 2200, 0xC9CDD6, 0x6B7280, BallStyle.MOON, 0xE5E7EB, gloss = 8f),
        BallLook("eye", R.string.ball_eye, 2800, 0xF8FAFC, 0x0EA5E9, BallStyle.EYE, 0x67E8F9, gloss = 120f),
        BallLook("earth", R.string.ball_earth, 3500, 0x1D4ED8, 0x16A34A, BallStyle.EARTH, 0x60A5FA, gloss = 60f),
        BallLook("gem", R.string.ball_gem, 4200, 0xA855F7, 0xFFFFFF, BallStyle.GEM, 0xE879F9, gloss = 160f, metal = 0.6f),
        BallLook("galaxy", R.string.ball_galaxy, 5000, 0x0B0620, 0xC026D3, BallStyle.GALAXY, 0xA78BFA, gloss = 90f),
    ).sortedBy { it.price }

    fun tower(id: String) = towers.firstOrNull { it.id == id } ?: towers[0]
    fun ball(id: String) = balls.firstOrNull { it.id == id } ?: balls[0]
}
