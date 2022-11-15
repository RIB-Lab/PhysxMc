package com.kamesuta.physxmc

import org.joml.Math
import org.joml.Quaterniondc
import org.joml.Vector3d

fun Quaterniondc.getEulerAnglesZYXRightHanded(eulerAngles: Vector3d): Vector3d {
    val x = x()
    val y = y()
    val z = z()
    val w = w()
    eulerAngles.x = Math.atan2(2.0 * (y * z + w * x), w * w - x * x - y * y + z * z)
    eulerAngles.y = Math.safeAsin(-2.0 * (x * z - w * y))
    eulerAngles.z = Math.atan2(2.0 * (x * y + w * z), w * w + x * x - y * y - z * z)
    return eulerAngles
}
