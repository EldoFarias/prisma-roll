import com.farias.prismaroll.game.*

/** Builds levels on the JVM: time to build, how many attempts, length, bot result. */
fun main(args: Array<String>) {
    val list = if (args.isNotEmpty()) args.map { it.toInt() } else listOf(1, 2, 3, 5, 8, 10, 12, 15, 18, 20, 22, 26, 30, 35, 40, 50, 60, 80, 100, 120)
    for (n in list) {
        val t0 = System.nanoTime()
        val lv = RollLevels.level(n)
        val ms = (System.nanoTime() - t0) / 1e6
        val kinds = lv.obs.groupingBy { it.kind }.eachCount()
        val segKinds = lv.segs.groupingBy { it.kind }.eachCount()
        val times = RollLevels.timeTable(lv)
        val dur = RollSim.timeAt(times, lv.finish)
        println("L$n${if (lv.boss) "B" else ""} build=${"%.0f".format(ms)}ms len=${"%.0f".format(lv.finish)} speed=${"%.1f".format(lv.speed)} time=${"%.0f".format(dur)}s segs=${lv.segs.size} obs=$kinds segKinds=$segKinds mech=${lv.mechanics} number=${lv.number} beats=${RollBot.beats(lv)}")
    }
    val t0 = System.nanoTime()
    val e = RollLevels.endless(42L)
    println("endless build=${(System.nanoTime() - t0) / 1_000_000}ms len=${e.finish} obs=${e.obs.size} beats=${RollBot.beats(e)}")
}
