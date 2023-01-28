package searles.nbody3d

import javafx.animation.AnimationTimer
import javafx.application.Application
import javafx.application.Platform
import javafx.event.EventHandler
import javafx.scene.*
import javafx.scene.paint.Color
import javafx.scene.paint.PhongMaterial
import javafx.scene.shape.Box
import javafx.scene.shape.Sphere
import javafx.scene.transform.Rotate
import javafx.scene.transform.Scale
import javafx.scene.transform.Translate
import javafx.stage.Stage
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.*

class NBody3D: Application() {
    private var isCalculationRunning: AtomicBoolean = AtomicBoolean(false)
    val universe = Universe.createCollidingDiscs()
    val particles = universe.bodies.map {
        Sphere(0.01).apply {
            translateX = it.gx
            translateY = it.gy
            translateZ = it.gz
        }
    }
    val positions = universe.bodies.map {
        arrayOf(it.x, it.y, it.z)
    }

    val screenWidth = 800.0
    val screenHeight = 800.0

    override fun start(primaryStage: Stage) {
        //Configuring Group, Scene and Stage
        val root = Group()
        //root.children.add(buildAxes())
        val scene = Scene(root, 800.0, 800.0, Color.BLACK)

        setUpCamera(scene)

        val centerX = screenWidth / 2.0
        val centerY = screenHeight / 2.0
        val translation = Translate(centerX, centerY)

        val scale = Scale(10.0, 10.0, 10.0)
        root.transforms.addAll(translation, scale)

        root.children.addAll(particles)

        primaryStage.title = "Box Example"
        primaryStage.scene = scene
        primaryStage.show()

        val timer = object : AnimationTimer() {
            override fun handle(now: Long) {
                @Suppress("DeferredResultUnused")
                GlobalScope.async {
                    // code to run in the background
                    if (isCalculationRunning.compareAndSet(false, true)) {
                        universe.step()
                        Platform.runLater {
                            drawUniverse()
                            isCalculationRunning.set(false)
                        }
                    }
                }
            }
        }

        timer.start()
    }
    private fun drawUniverse() {
        universe.bodies.forEachIndexed { i, b ->
            particles[i].translateX = b.x// - positions[i][0]
            particles[i].translateY = b.y// - positions[i][1]
            particles[i].translateZ = b.z// - positions[i][2]

            positions[i][0] = b.x
            positions[i][1] = b.y
            positions[i][2] = b.z
        }

//        var minMass = universe.bodies.first().mass
//        var maxMass = universe.bodies.first().mass
//        var meanLogForce = 0.0
//        var varianceLogForce = 0.0
//        var n = 0
//
//        universe.forEachBody {
//            n++
//
//            minMass = min(minMass, it.mass)
//            maxMass = max(maxMass, it.mass)
//
//            val logForce = ln(it.totalForce)
//
//            varianceLogForce = (n - 1.0) / n * (varianceLogForce + meanLogForce.pow(2)) + logForce.pow(2) / n
//            meanLogForce = (n - 1.0) / n * meanLogForce + logForce / n
//            varianceLogForce -= meanLogForce.pow(2)
//        }
//
//        universe.forEachBody {
//            // size varies from 0.2 to 4 using 3rd root of mass.
//            val size = getSizeForMass(it.mass, minMass, maxMass)
//            graphicsContext.fill = getColorForStats(ln(it.totalForce), meanLogForce, sqrt(varianceLogForce))
//            val vx = (it.x - cx + len / 2.0) / len * width
//            val vy = (it.y - cy + len / 2.0) / len * height
//            graphicsContext.fillOval(vx, vy, size, size)
//        }
    }

    private fun buildAxes(): Group {
        val xAxis = Box(1200.0, 1.0, 1.0)
        val yAxis = Box(1.0, 1200.0, 1.0)
        val zAxis = Box(1.0, 1.0, 1200.0)
        xAxis.material = PhongMaterial(Color.RED)
        yAxis.material = PhongMaterial(Color.GREEN)
        zAxis.material = PhongMaterial(Color.BLUE)
        val axisGroup = Group()
        axisGroup.children.addAll(xAxis, yAxis, zAxis)
        return axisGroup
    }

    private fun setUpCamera(scene: Scene) {
        val camera = PerspectiveCamera()
        scene.camera = camera

        // Add camera controls
        var x0 = 0.0
        var y0 = 0.0
        var isDragged = false

        val rotateX = Rotate(0.0, Rotate.X_AXIS)
        val rotateY = Rotate(0.0, Rotate.Y_AXIS)
        val rotateZ = Rotate(0.0, Rotate.Z_AXIS)

        camera.transforms.apply {
            add(rotateX)
            add(rotateY)
            add(rotateZ)
        }

        scene.onMousePressed = EventHandler {
            x0 = it.sceneX
            y0 = it.sceneY
            isDragged = true
        }

        scene.onMouseDragged = EventHandler {
            if(!isDragged) return@EventHandler

            val dx = x0 - it.sceneX
            val dy = y0 - it.sceneY

            if(it.isShiftDown) {
                camera.translateX += dx
                camera.translateY += dy
            } else {
                rotateY.angle -= dx / 2.0
                rotateX.angle += dy / 10.0
            }

            x0 = it.sceneX
            y0 = it.sceneY
        }

        scene.onMouseReleased = EventHandler {
            isDragged = false
        }

        scene.onScroll = EventHandler {
            camera.translateZ += it.deltaY
        }
    }

    private fun createParticle(): Sphere {
        val sphere = Sphere(5.0)

        // Set a random position for the particle
        sphere.translateX = Math.random() * 800 - 400
        sphere.translateY = Math.random() * 600 - 300
        sphere.translateZ = Math.random() * 800 - 400
        // Set a random color for the particle
        sphere.material = PhongMaterial().apply {
            diffuseColor = Color.color(Math.random(), Math.random(), Math.random())
        }
        return sphere
    }
}