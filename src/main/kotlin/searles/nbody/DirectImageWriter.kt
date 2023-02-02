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

        stage.show()

        val format = PixelFormat.getIntArgbInstance()

        val timer: AnimationTimer = object : AnimationTimer() {
            override fun handle(now: Long) {
                animationStep(now, buffer)
                backBufferWriter.setPixels(0, 0, buffer.width, buffer.height, format, buffer.pixels, 0, buffer.width)
            }
        }

        timer.start()
    }

    private fun updateImageBinding(imageView: ImageView) {
        backBuffer = WritableImage(buffer.width, buffer.height)
        backBufferWriter = backBuffer.pixelWriter
        imageView.image = backBuffer
    }

    abstract fun animationStep(now: Long, pixelBuffer: PixelBuffer)
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

    operator fun get(x: Int, y: Int): Int {
        if(x in 0 until width && y in 0 until height) {
            return pixels[x + y * width]
        }

        return 0
    }

    fun drawCircle(cx: Double, cy: Double, rad: Double, color: Int) {
        val x0 = max(0, floor(cx - rad).toInt())
        val x1 = min(width - 1, ceil(cx + rad).toInt())
        val y0 = max(0, floor(cy - rad).toInt())
        val y1 = min(height - 1, ceil(cy + rad).toInt())

        for(y in y0 .. y1) {
            for(x in x0 .. x1) {
                if((x - cx).pow(2) + (y - cy).pow(2) <= rad.pow(2)) {
                    set(x, y, color)
                }
            }
        }
    }
}