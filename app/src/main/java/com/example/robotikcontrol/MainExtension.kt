package com.example.robotikcontrol

import Enums.Direction
import Enums.MoveMode
import Enums.Orientation
import android.util.Log
import android.view.MotionEvent


const val ORIENTATION = "ORIENTATION"
const val DIRECTION = "DIRECTION"
const val SPEED = "SPEED"

private var lastHeightSize: Int = 1100
private var borders = intArrayOf(40, 280, 281, 530, 531, 780, 781, 1000) // speed panel view borders
private var orientationBorder: Int = 530

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
fun MainActivity.getPredefinedSpeed(viewSize: Int, motionEvent: MotionEvent?): Int {
    val y = motionEvent?.y?.toInt() ?: return 0
    if (lastHeightSize != viewSize){
        calculateSpeedViewBorders(viewSize)
        lastHeightSize = viewSize
    }

    if (y in borders[0]..borders[1]){ // fast forward 40..280
        return 200
    }
    if (y in borders[2]..borders[3]){ // forward 281..530
        return 100
    }
    if (y in borders[4]..borders[5]){ // backward 531..780
        return 100
    }
    if (y in borders[6]..borders[7]){ // fast backward // 781..1000
        return 200
    }
    return 0
}
// if the speed control panel has different sizes depending on the
// screen the borders has to calculate new with the correct ratio
private fun calculateSpeedViewBorders(viewSize: Int){
    borders[0] =  (viewSize * 0.036).toInt()
    borders[1] = (viewSize * 0.255).toInt()
    borders[2] = borders[1] + 1
    borders[3] = (viewSize * 0.481).toInt()
    borders[4] = borders[3] + 1
    borders[5] = (viewSize * 0.710).toInt()
    borders[6] = borders[5] + 1
    borders[7] = (viewSize * 0.909).toInt()

    orientationBorder = (viewSize * 0.482).toInt()
    Log.w("MainExtensionsNewBorders", "New Borders: ${borders[0]} : ${borders[1]} : " +
            "${borders[2]} : ${borders[3]} : ${borders[4]} : ${ borders[5]} : ${borders[6]} : ${borders[7]}")
}
// calculate from the touch position the vehicle orientation
fun MainActivity.getPredefinedOrientation(motionEvent: MotionEvent?): Orientation{
    val y = motionEvent?.y?.toInt() ?: return Orientation.RELEASE

    if (y > orientationBorder){
        return Orientation.BACKWARD
    }
    if (y < orientationBorder){
        return Orientation.FORWARD
    }
    return Orientation.RELEASE
}

/*fun CoroutineScope.launchPeriodicAsync(
    repeatMillis: Long,
    action: () -> Unit
) = this.async {
    if (repeatMillis > 0) {
        while (true) {
            action()
            delay(repeatMillis)
        }
    } else {
        action()
    }
}*/