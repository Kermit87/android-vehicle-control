package com.example.robotikcontrol

import Enums.Direction
import Enums.MoveMode
import Enums.Orientation
import android.view.MotionEvent
import org.json.JSONObject


const val ORIENTATION = "ORIENTATION"
const val DIRECTION = "DIRECTION"
const val SPEED = "SPEED"

fun createVehicleMotionJson(orientation: Orientation, direction: Direction, speed: Int): JSONObject{
    val vehicleMotion = JSONObject()
    vehicleMotion.put(ORIENTATION, orientation.toString())
    vehicleMotion.put(DIRECTION, direction.toString())
    vehicleMotion.put(SPEED, speed)
    return vehicleMotion
}

fun calculateMoveMode(orientation: Orientation, speed: Int): MoveMode {
    if (orientation == Orientation.BACKWARD && speed == 200){
        return MoveMode.BackwardSpeed2
    }
    if (orientation == Orientation.BACKWARD && speed == 100){
        return MoveMode.BackwardSpeed1
    }
    if (orientation == Orientation.FORWARD && speed == 200){
        return MoveMode.ForwardSpeed2
    }
    if (orientation == Orientation.FORWARD && speed == 100){
        return MoveMode.ForwardSpeed1
    }
    return MoveMode.Stop
}

fun createVehicleMotionString(orientation: Orientation, direction: Direction, speed: Int): String {
    var or = ""
    var dir = ""

    or = when(orientation){
        Orientation.FORWARD -> "F"
        Orientation.BACKWARD -> "B"
        Orientation.RELEASE -> "R"
    }

    dir = when(direction){
        Direction.STRAIGHT -> "S"
        Direction.LEFT -> "L"
        Direction.RIGHT -> "R"
        Direction.UNKNOWN -> "S"
    }
   return "$or$dir$speed"
}

// calculate from the touch position the defined speed
fun MainActivity.getPredefinedSpeed(motionEvent: MotionEvent?): Int {
    val y = motionEvent?.y?.toInt() ?: return 0

    if (y in 40..280){
        return 200
    }
    if (y in 281..530){
        return 100
    }
    if (y in 531..780){
        return 100
    }
    if (y in 781..1000){
        return 200
    }
    return 0
}
// calculate from the touch position the vehicle orientation
fun MainActivity.getPredefinedOrientation(motionEvent: MotionEvent?): Orientation{
    val y = motionEvent?.y?.toInt() ?: return Orientation.RELEASE

    if (y > 530){
        return Orientation.BACKWARD
    }
    if (y < 530){
        return Orientation.FORWARD
    }
    return Orientation.RELEASE
}