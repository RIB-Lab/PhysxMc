package com.kamesuta.physxmc

import com.kamesuta.physxmc.PhysicsEntity.ArmorStandEntity.Companion.blockCenterHeight
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector
import org.joml.Quaterniond
import org.joml.Vector3d
import org.joml.Vector3i
import physx.common.PxVec3
import physx.physics.PxRigidDynamic

class PhysicsWorld(val level: World) {
    val dynamicsWorld = DynamicsWorld(level, 0.025F)
    private var renderPercent = 0.0
    private val bodies = mutableSetOf<BoxRigidBody>()
    private var lastEntityUpdates = mutableSetOf<BoxRigidBody>()
    private val chunkBodies = mutableMapOf<Vector3i, List<BoxRigidBody>>()
    private var blocksChanged = false
    private val loadedChunks = mutableSetOf<Vector3i>()
    private val chunkUpdates = mutableSetOf<Vector3i>()
    private val entities = mutableListOf<Entity>()

    init {
        val floor = BoxRigidBody(
            PhysicsEntity.BlockEntity(level.getBlockAt(0, 3, 0)),
            100f, 1f, 100f, 0f, 0f, 0f, false,
        )
        addRigidBody(floor)
    }

    fun addBoxEntity(location: Location, itemStack: ItemStack): ArmorStand {
        val armorStand =
            location.world.spawn(location.clone().add(0.0, -blockCenterHeight, 0.0), ArmorStand::class.java)
        //armorStand.isInvulnerable = true
        //armorStand.isMarker = true
        armorStand.isVisible = false
        armorStand.setGravity(false)
        addRigidBody(
            BoxRigidBody(
                PhysicsEntity.ArmorStandEntity(armorStand),
                1f, 1f, 1f,
                0f, 0f, 0f,
                true
            )
        )
        entities.add(armorStand)

        armorStand.setItem(EquipmentSlot.HEAD, itemStack)

        return armorStand
    }

    fun addRigidBody(rigidBody: BoxRigidBody) {
        dynamicsWorld.addActor(rigidBody.actor)
        bodies.add(rigidBody)
    }

    fun findEntity(entity: Entity): BoxRigidBody? {
        return bodies.find {
            it.entity is PhysicsEntity.MobEntity && it.entity.entity == entity
        }
    }

    fun addForce(entity: Entity, force: Vector) {
        val rigidBody = findEntity(entity)
            ?: return
        val rigidDynamic = rigidBody.actor as? PxRigidDynamic
            ?: return
        // 力を加える
        val pxForce = PxVec3(force.x.toFloat(), force.y.toFloat(), force.z.toFloat())
        rigidDynamic.addForce(pxForce)
        pxForce.destroy()
    }

    fun update(diff: Double) {
        if (dynamicsWorld.willUpdate(diff)) {
            lastEntityUpdates.subtract(bodies).forEach {
                dynamicsWorld.removeActor(it.actor)
                it.destroy()
            }
            lastEntityUpdates.clear()
            lastEntityUpdates.addAll(bodies)
        }
        val updateCount = dynamicsWorld.update({ _: Double -> }/*this::physicsUpdate*/, diff)
        this.renderPercent = dynamicsWorld.time / dynamicsWorld.fixedTimeStep.toDouble()
        if (updateCount > 0) {
            val updateDiff: Float = updateCount.toFloat() * dynamicsWorld.fixedTimeStep
            bodies.removeIf {
                if (it.isDestroyed) return@removeIf true
                if (!it.isKinematicOrFrozen) {
                    updateTransformations(it, updateDiff.toDouble())
                    if (it.entity.isDead) {
                        dynamicsWorld.removeActor(it.actor)
                        it.destroy()
                        return@removeIf true
                    }
                }
                return@removeIf false
            }
        }
        this.chunkUpdates.clear()
    }

    fun destroy() {
        for (body in bodies) {
            dynamicsWorld.removeActor(body.actor)
            body.destroy()
        }
        bodies.clear()
        entities.forEach { it.remove() }

        dynamicsWorld.destroy()
    }

    private fun updateTransformations(boxRigidBody: BoxRigidBody, diff: Double) {
        boxRigidBody.actor.globalPose.p.apply {
            boxRigidBody.entity.translation = Vector3d(
                x.toDouble(),
                y.toDouble(),
                z.toDouble(),
            )
        }
        boxRigidBody.actor.globalPose.q.apply {
            boxRigidBody.entity.rotation = Quaterniond(
                x.toDouble(),
                y.toDouble(),
                z.toDouble(),
                w.toDouble(),
            )
        }
    }

    /*
    fun physicsUpdate(diff: Double) {
        for (body in bodies) {
            if (!body.isKinematicOrFrozen && !body.isDestroyed) {
                body.updatePhysics(this, diff, this.blocksChanged)
            }
        }
        this.blocksChanged = false
        this.checkLoadedChunks()
        this.loadedChunkEntitiesChanged = false
    }

    private fun updateChunk(cx: Int, cy: Int, cz: Int) {
        val chunkPos = Vector3i(cx, cy, cz)
        if (!chunkUpdates.contains(chunkPos)) {
            chunkUpdates.add(chunkPos)
            if (this.loadedChunks.contains(chunkPos)) {
                this.unloadChunk(chunkPos)
                loadChunk(chunkPos)
            }
        }
    }

    private fun unloadChunk(chunkPos: Vector3i) {
        val bodies = chunkBodies.remove(chunkPos)
            ?: return
        for (body in bodies) {
            dynamicsWorld.removeActor(body.actor)
            body.destroy()
        }
    }
    */
}