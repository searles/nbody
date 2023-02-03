package searles.nbody

import javafx.animation.AnimationTimer
import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.image.ImageView
import javafx.scene.image.PixelFormat
import javafx.scene.image.PixelWriter
import javafx.scene.image.WritableImage
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.stage.Stage
import kotlin.math.*

abstract class DirectImageWriter : Application() {

    private var buffer = PixelBuffer(800, 800)
    private lateinit var backBuffer: WritableImage
    private lateinit var backBufferWriter: PixelWriter

    val width get() = buffer.width
    val height get() = buffer.height

    override fun start(stage: Stage) {
        val root = StackPane()

        val imageView = ImageView().also {
            updateImageBinding(it)
        }

        root.children.add(imageView)

        val scene = Scene(root, buffer.width.toDouble(), buffer.height.toDouble(), Color.WHITE)
        stage.scene = scene

        scene.widthProperty().addListener { _, _, newWidth ->
            buffer.resize(width = newWidth.toInt())
            updateImageBinding(imageView)
        }

        scene.heightProperty().addListener { _, _, newHeight ->
            buffer.resize(height = newHeight.toInt())
            updateImageBinding(imageView)
        }

        connectControls(scene)

        stage.show()

        val timer: AnimationTimer = object : AnimationTimer() {
            override fun handle(now: Long) {
                animationStep(now, buffer)
            }
        }

        timer.start()
    }

    protected fun updateView() {
        backBufferWriter.setPixels(0, 0, buffer.width, buffer.height, PixelFormat.getIntArgbInstance(), buffer.pixels, 0, buffer.width)
    }

    private fun updateImageBinding(imageView: ImageView) {
        backBuffer = WritableImage(buffer.width, buffer.height)
        backBufferWriter = backBuffer.pixelWriter
        imageView.image = backBuffer
    }

    abstract fun animationStep(now: Long, pixelBuffer: PixelBuffer)
    open fun connectControls(scene: Scene) {}
}

class PixelBuffer(var width: Int, var height: Int) {
    var pixels: IntArray = IntArray(width * height)

    fun resize(width: Int = this.width, height: Int = this.height) {
        if(this.width != width || this.height != height) {
            this.width = width
            this.height = height
            this.pixels = IntArray(width * height)
        }
    }

    fun fill(color: Int) {
        pixels.fill(color)
    }

    operator fun set(x: Int, y: Int, color: Int) {
        if(x in 0 until width && y in 0 until height) {
            pixels[x + y * width] = color
        }
    }

    fun set(x: Int, y: Int, color: Int, alpha: Double) {
        pixels[x + y * width] = alphaBlend(color, pixels[x + y * width], alpha)
    }

    operator fun get(x: Int, y: Int): Int {
        if(x in 0 until width && y in 0 until height) {
            return pixels[x + y * width]
        }

        return 0
    }

    fun alphaBlend(color1: Int, color2: Int, alpha: Double): Int {
        // alpha == 1 shows color1.
        val red1 = (color1 shr 16 and 0xff) / 255.0
        val green1 = (color1 shr 8 and 0xff) / 255.0
        val blue1 = (color1 and 0xff) / 255.0

        val red2 = (color2 shr 16 and 0xff) / 255.0
        val green2 = (color2 shr 8 and 0xff) / 255.0
        val blue2 = (color2 and 0xff) / 255.0

        val red = (1 - alpha) * red2 + alpha * red1
        val green = (1 - alpha) * green2 + alpha * green1
        val blue = (1 - alpha) * blue2 + alpha * blue1

        return (255 shl 24) or ((255 * red).toInt() shl 16) or ((255 * green).toInt() shl 8) or (255 * blue).toInt()
    }

    fun drawCircle(cx: Double, cy: Double, rad: Double, color: Int) {
        val x0 = max(0, floor(cx - rad).toInt())
        val x1 = min(width - 1, ceil(cx + rad).toInt())
        val y0 = max(0, floor(cy - rad).toInt())
        val y1 = min(height - 1, ceil(cy + rad).toInt())

        for(y in y0 .. y1) {
            for(x in x0 .. x1) {
                val d2 = (x - cx).pow(2) + (y - cy).pow(2)
                if(d2 <= rad.pow(2)) {
                    set(x, y, color)
                } else if(d2 <= (rad + 1).pow(2)) {
                    set(x, y, color, 1 - sqrt(d2) + rad)
                }
            }
        }
    }
}