package searles

import javafx.application.Application.launch
import searles.nbody.nbody2d.NBody2D

fun main(args: Array<String>) {
    launch(NBody2D::class.java, *args)
}