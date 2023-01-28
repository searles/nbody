package searles

import javafx.application.Application.launch
import searles.nbody2d.NBody2D
import searles.nbody3d.NBody3D

fun main(args: Array<String>) {
    launch(NBody2D::class.java, *args)
}