package searles.nbody

import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min

object Commons {
    fun getColorForStats(x: Double, mean: Double, sigma: Double): Int {
        val zValue = 2.33 // 99%
        val min = mean - sigma * zValue
        var v = (x - min) / (2 * zValue * sigma)

        if(v < 0.0) v = 0.0 else if(v > 1.0) v = 1.0

        return getColor(v)
    }

    fun getColor(v: Double): Int {
        // 0 = (0.5, 0.25, 0.0), 0.33 = (1.0, 0.75, ?), 0.66 = (0.5, 0.75, 1.0), 1 = (1.0, 1.0, 1.0)
        val r = 0.5 - 0.5 * cos(v * Math.PI * 3.0)
        val g = 0.3 + 0.7 * v
        val b = if(v < 2.0 / 3.0)
            0.5 - 0.5 * cos(v * Math.PI * 3.0 / 2.0)
        else
            1.0

        val ir = max(0, min(255, (r * 256.0).toInt()))
        val ig = max(0, min(255, (g * 256.0).toInt()))
        val ib = max(0, min(255, (b * 256.0).toInt()))

        return (0xff shl 24) or (ir shl 16) or (ig shl 8) or ib
    }

    fun getSizeForMass(mass: Double, minMass: Double, maxMass: Double): Double {
        val minSize = 0.5
        val maxSize = 3.0

        val d = maxMass - minMass
        if(d == 0.0) return 1.0

        val min = Math.cbrt(minMass)
        val max = Math.cbrt(maxMass)
        val len = Math.cbrt(mass)

        return (maxSize - minSize) * (len - min) / (max - min) + minSize
    }
}