package com.kamesuta.physxmc

import org.joml.Quaterniond
import org.joml.Quaterniondc
import org.joml.Vector3d
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2

fun Quaterniond.getEulerAnglesYXZSingularity(eulerAngles: Vector3d): Vector3d {
    val sqw = w * w
    val sqx = x * x
    val sqy = y * y
    val sqz = z * z
    val unit = sqx + sqy + sqz + sqw // if normalised is one, otherwise is correction factor
    val test = x * y + z * w
    if (test > 0.499 * unit) { // singularity at North Pole
        eulerAngles.y = 2 * atan2(x, w)
        eulerAngles.x = Math.PI / 2
        eulerAngles.z = 0.0
    } else if (test < -0.499 * unit) { // singularity at South Pole
        eulerAngles.y = -2 * atan2(x, w)
        eulerAngles.x = -Math.PI / 2
        eulerAngles.z = 0.0
    } else {
        eulerAngles.y = atan2(2 * y * w - 2 * x * z, sqx - sqy - sqz + sqw)
        eulerAngles.x = org.joml.Math.safeAsin(2 * test / unit)
        eulerAngles.z = atan2(2 * x * w - 2 * y * z, -sqx + sqy - sqz + sqw)
    }
    return eulerAngles
}

fun Quaterniond.getEulerAnglesXYZSingularity(eulerAngles: Vector3d): Vector3d {
    val sqw = w * w
    val sqx = x * x
    val sqy = y * y
    val sqz = z * z
    val unit = sqx + sqy + sqz + sqw // if normalised is one, otherwise is correction factor
    val test = x * z + y * w
    if (test > 0.499 * unit) { // singularity at North Pole
        eulerAngles.x = 2 * atan2(y, w)
        eulerAngles.y = Math.PI / 2
        eulerAngles.z = 0.0
    } else if (test < -0.499 * unit) { // singularity at South Pole
        eulerAngles.x = -2 * atan2(y, w)
        eulerAngles.y = -Math.PI / 2
        eulerAngles.z = 0.0
    } else {
        getEulerAnglesXYZ(eulerAngles)
    }
    return eulerAngles
}

enum class RotSeq { zyx, zyz, zxy, zxz, yxz, yxy, yzx, yzy, xyz, xyx, xzy, xzx }

fun twoAxisRot(r11: Double, r12: Double, r21: Double, r31: Double, r32: Double, res: Vector3d) {
    res.x = atan2(r11, r12)
    res.y = acos(r21)
    res.z = atan2(r31, r32)
}

fun threeAxisRot(r11: Double, r12: Double, r21: Double, r31: Double, r32: Double, res: Vector3d) {
    res.x = atan2(r31, r32)
    res.y = asin(r21)
    res.z = atan2(r11, r12)
}

fun Quaterniondc.quaternion2Euler(res: Vector3d, rotSeq: RotSeq): Vector3d {
    val x = x()
    val y = y()
    val z = z()
    val w = w()

    when (rotSeq) {
        RotSeq.zyx ->
            threeAxisRot(
                2 * (x * y + w * z),
                w * w + x * x - y * y - z * z,
                -2 * (x * z - w * y),
                2 * (y * z + w * x),
                w * w - x * x - y * y + z * z,
                res
            )

        RotSeq.zyz ->
            twoAxisRot(
                2 * (y * z - w * x),
                2 * (x * z + w * y),
                w * w - x * x - y * y + z * z,
                2 * (y * z + w * x),
                -2 * (x * z - w * y),
                res
            )

        RotSeq.zxy ->
            threeAxisRot(
                -2 * (x * y - w * z),
                w * w - x * x + y * y - z * z,
                2 * (y * z + w * x),
                -2 * (x * z - w * y),
                w * w - x * x - y * y + z * z,
                res
            )

        RotSeq.zxz ->
            twoAxisRot(
                2 * (x * z + w * y),
                -2 * (y * z - w * x),
                w * w - x * x - y * y + z * z,
                2 * (x * z - w * y),
                2 * (y * z + w * x),
                res
            )

        RotSeq.yxz ->
            threeAxisRot(
                2 * (x * z + w * y),
                w * w - x * x - y * y + z * z,
                -2 * (y * z - w * x),
                2 * (x * y + w * z),
                w * w - x * x + y * y - z * z,
                res
            )

        RotSeq.yxy ->
            twoAxisRot(
                2 * (x * y - w * z),
                2 * (y * z + w * x),
                w * w - x * x + y * y - z * z,
                2 * (x * y + w * z),
                -2 * (y * z - w * x),
                res
            )

        RotSeq.yzx ->
            threeAxisRot(
                -2 * (x * z - w * y),
                w * w + x * x - y * y - z * z,
                2 * (x * y + w * z),
                -2 * (y * z - w * x),
                w * w - x * x + y * y - z * z,
                res
            )

        RotSeq.yzy ->
            twoAxisRot(
                2 * (y * z + w * x),
                -2 * (x * y - w * z),
                w * w - x * x + y * y - z * z,
                2 * (y * z - w * x),
                2 * (x * y + w * z),
                res
            )

        RotSeq.xyz ->
            threeAxisRot(
                -2 * (y * z - w * x),
                w * w - x * x - y * y + z * z,
                2 * (x * z + w * y),
                -2 * (x * y - w * z),
                w * w + x * x - y * y - z * z,
                res
            )

        RotSeq.xyx ->
            twoAxisRot(
                2 * (x * y + w * z),
                -2 * (x * z - w * y),
                w * w + x * x - y * y - z * z,
                2 * (x * y - w * z),
                2 * (x * z + w * y),
                res
            )

        RotSeq.xzy ->
            threeAxisRot(
                2 * (y * z + w * x),
                w * w - x * x + y * y - z * z,
                -2 * (x * y - w * z),
                2 * (x * z + w * y),
                w * w + x * x - y * y - z * z,
                res
            )

        RotSeq.xzx ->
            twoAxisRot(
                2 * (x * z - w * y),
                2 * (x * y + w * z),
                w * w + x * x - y * y - z * z,
                2 * (x * z + w * y),
                -2 * (x * y - w * z),
                res
            )
    }

    return res
}