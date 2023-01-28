package searles.nbody3d

import javafx.scene.Camera
import javafx.scene.Scene

class CameraGestureControl(private val camera: Camera, private val scene: Scene) {
    private var anchorX: Double = 0.0
    private var anchorY: Double = 0.0
    private var anchorAngleX = 0.0
    private var anchorAngleY = 0.0

    init {
        scene.setOnMousePressed {
            anchorX = it.sceneX
            anchorY = it.sceneY
            anchorAngleX = camera.rotate
            // TODO anchorAngleY = camera.tilt
        }

        scene.setOnMouseDragged {
            camera.rotate = anchorAngleX - (anchorY - it.sceneY)
            // TODO camera.tilt = anchorAngleY + (anchorX - it.sceneX)
        }

        scene.setOnScroll {
            camera.translateZ += it.deltaY
        }
    }
}