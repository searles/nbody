package searles.nbody

import org.apache.commons.math3.linear.Array2DRowRealMatrix
import org.apache.commons.math3.linear.RealMatrix
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

object Commons {
    fun getColorForStats(x: Double, mean: Double, sigma: Double): Int {
        val zValue = 2.33 // 99%
        val min = mean - sigma * zValue
        var v = (x - min) / (2 * zValue * sigma)

        if(v < 0.0) v = 0.0 else if(v > 1.0) v = 1.0

        return getColor(v)
    }

    fun getColor(v: Double): Int {
        var r = 0.0
        var g = 0.0
        var b = 0.0

        when {
            v < 0.25 -> { b = 1.0; r = v * 4.0 }
            v < 0.5 -> { b = 2.0 - 4.0 * v; r = 1.0 }
            v < 0.75 -> { g = 4.0 * v - 2.0; r = 1.0 }
            else -> {r = 1.0; g = 1.0; b = 4.0 * v - 3.0}
        }

        val ir = max(0, min(255, (r * 256.0).toInt()))
        val ig = max(0, min(255, (g * 256.0).toInt()))
        val ib = max(0, min(255, (b * 256.0).toInt()))

        return (0xff shl 24) or (ir shl 16) or (ig shl 8) or ib
    }

    fun getSizeForMass(mass: Double, minMass: Double, maxMass: Double): Double {
        val minSize = 0.25
        val maxSize = 2.0

        val d = maxMass - minMass
        if(d == 0.0) return 1.0

        val min = Math.cbrt(minMass)
        val max = Math.cbrt(maxMass)
        val len = Math.cbrt(mass)

        return (maxSize - minSize) * (len - min) / (max - min) + minSize
    }

    fun translate(dx: Double, dy: Double): RealMatrix {
        return Array2DRowRealMatrix(
            arrayOf(
                doubleArrayOf(1.0, 0.0, dx),
                doubleArrayOf(0.0, 1.0, dy),
                doubleArrayOf(0.0, 0.0, 1.0)
            )
        )
    }

    fun translate(dx: Double, dy: Double, dz: Double): RealMatrix {
        return Array2DRowRealMatrix(
            arrayOf(
                doubleArrayOf(1.0, 0.0, 0.0, dx),
                doubleArrayOf(0.0, 1.0, 0.0, dy),
                doubleArrayOf(0.0, 0.0, 1.0, dz),
                doubleArrayOf(0.0, 0.0, 0.0, 1.0),
            )
        )
    }

    fun scale2D(factor: Double): RealMatrix {
        return Array2DRowRealMatrix(
            arrayOf(
                doubleArrayOf(factor, 0.0, 0.0),
                doubleArrayOf(0.0, factor, 0.0),
                doubleArrayOf(0.0, 0.0, 1.0)
            )
        )
    }

    fun scale3D(factor: Double): RealMatrix {
        return Array2DRowRealMatrix(
            arrayOf(
                doubleArrayOf(factor, 0.0, 0.0, 0.0),
                doubleArrayOf(0.0, factor, 0.0, 0.0),
                doubleArrayOf(0.0, 0.0, factor, 0.0),
                doubleArrayOf(0.0, 0.0, 0.0, 1.0)
            )
        )
    }

    fun rotate(dx: Double, dy: Double, dz: Double): RealMatrix {
        val thetaX = dy / 100.0
        val thetaY = dx / 100.0

        val cx = cos(thetaX)
        val sx = sin(thetaX)
        val cy = cos(thetaY)
        val sy = sin(thetaY)

        return Array2DRowRealMatrix(
            arrayOf(
                doubleArrayOf(cy, 0.0, sy, 0.0),
                doubleArrayOf(sx * sy, cx, -sx * cy, 0.0),
                doubleArrayOf(-sy * cx, sx, cx * cy, 0.0),
                doubleArrayOf(0.0, 0.0, 0.0, 1.0)
            )
        )
    }
}