package searles

import javafx.application.Application.launch
import searles.nbody.DirectImageWriter
import searles.nbody.nbody2d.NBody2D
import searles.nbody.nbody3d.NBody3D

fun main(args: Array<String>) {
    launch(DirectImageWriter::class.java, *args)
}