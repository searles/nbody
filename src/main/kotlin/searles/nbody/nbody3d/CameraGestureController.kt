package searles.nbody.nbody3d

import javafx.scene.Camera
import javafx.scene.Group
import javafx.scene.Scene
import kotlin.math.pow

class CameraGestureControl(private val camera: Camera, private val scene: Scene, private val root: Group) {
    private var anchorX: Double = 0.0
    private var anchorY: Double = 0.0
    private var anchorAngleX = 0.0
    private var anchorAngleY = 0.0

    fun setUp() {
        var isDragged = false

        scene.setOnMousePressed {
            anchorX = it.sceneX
            anchorY = it.sceneY
            isDragged = true
        }

        scene.setOnMouseDragged {
            if(isDragged) {
                move(it.sceneX - anchorX, it.sceneY - anchorY)
                anchorX = it.sceneX
                anchorY = it.sceneY
            }
        }

        scene.setOnMouseReleased { isDragged = false }

        scene.setOnScroll {
            scale(it.deltaY)
        }
    }

    fun move(dx: Double, dy: Double) {
        root.translateX += dx
        root.translateY += dy
    }

    fun moveZ(dz: Double) {
        root.translateZ += dz
    }

    fun scale(delta: Double) {
        // factor 100 = double
        // factor -100 = half
        // factor 0 = 1

        val factor = 1.01.pow(delta)

        println(factor)
        camera.scaleX *= factor
        camera.scaleY *= factor
        camera.scaleZ *= factor
    }
}