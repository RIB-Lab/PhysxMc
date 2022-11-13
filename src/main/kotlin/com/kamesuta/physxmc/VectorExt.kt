package com.kamesuta.physxmc

import org.bukkit.util.Vector
import org.joml.Vector3d

fun Vector.toJoml() = Vector3d(x, y, z)

fun Vector3d.toBukkit() = Vector(x, y, z)
