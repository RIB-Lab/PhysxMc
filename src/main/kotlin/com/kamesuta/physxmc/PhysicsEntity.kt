package com.kamesuta.physxmc

import org.bukkit.Particle
import org.bukkit.block.Block
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.util.EulerAngle
import org.joml.Quaterniond
import org.joml.Quaterniondc
import org.joml.Vector3d
import org.joml.Vector3dc

sealed class PhysicsEntity(
    val physicsGroup: Int,
    val physicsMask: Int = 23,
) {
    open var translation: Vector3dc = Vector3d()
    val oldTranslation = Vector3d()
    open var rotation: Quaterniondc = Quaterniond()
    val oldRotation = Quaterniond()
    open val isDead = false

    open class MobEntity(
        val entity: Entity,
    ) : PhysicsEntity(physicsGroup = 2) {
        override var translation: Vector3dc
            get() = entity.location.toVector().toJoml()
            set(value) {
                entity.teleport(entity.location.set(value.x(), value.y(), value.z()))
            }

        override var rotation: Quaterniondc
            get() = Quaterniond().rotationXYZ(entity.location.pitch.toDouble(), entity.location.yaw.toDouble(), 0.0)
            set(value) {}

        override val isDead: Boolean
            get() = entity.isDead
    }

    class ArmorStandEntity(
        val armorStand: ArmorStand,
    ) : MobEntity(armorStand) {
        override var translation: Vector3dc
            get() = armorStand.location.toVector().toJoml()
            set(value) {
                armorStand.teleport(armorStand.location.set(value.x(), value.y(), value.z()))
            }

        override var rotation: Quaterniondc
            get() = Quaterniond().rotationXYZ(
                armorStand.headPose.x,
                armorStand.headPose.y - armorStand.location.yaw,
                armorStand.headPose.z
            )
            set(value) {
                val euler = value.quaternion2Euler(Vector3d(), RotSeq.zyx)
                armorStand.headPose = EulerAngle(euler.x, -euler.y, -euler.z)

                val location = armorStand.location
                val direction = value.transform(Vector3d(1.0, 0.0, 0.0)).toBukkit()
                armorStand.world.spawnParticle(Particle.FLAME, location, 1, 0.0, 0.0, 0.0, 0.0)
                armorStand.world.spawnParticle(Particle.FLAME, location.clone().add(direction), 1, 0.0, 0.0, 0.0, 0.0)
            }
    }

    class BlockEntity(
        val block: Block,
    ) : PhysicsEntity(physicsGroup = 1) {
        override var translation: Vector3dc
            get() = block.location.toVector().toJoml()
            set(value) {
                block.location.set(value.x(), value.y(), value.z())
            }
    }
}
