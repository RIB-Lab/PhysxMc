package com.kamesuta.physxmc.physics

import com.kamesuta.physxmc.physics.PhysicsEntity.ArmorStandEntity.Companion.blockCenterHeight
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector
import org.joml.Math
import org.joml.Quaterniond
import org.joml.Vector3d
import org.joml.Vector3i
import physx.common.PxVec3
import physx.physics.PxForceModeEnum
import physx.physics.PxRigidBody
import physx.physics.PxRigidDynamic

class PhysicsWorld(val level: World) {
    val dynamicsWorld = DynamicsWorld(level, 0.025F)
    private var renderPercent = 0.0
    private val bodies = mutableSetOf<BoxRigidBody>()
    private var lastEntityUpdates = mutableSetOf<BoxRigidBody>()
    private val chunkBodies = mutableMapOf<Vector3i, MutableList<BoxRigidBody>>()
    private var blocksChanged = false
    private val explosions = mutableListOf<Explosion>()
    private val loadedChunks = mutableSetOf<Vector3i>()
    private val chunkUpdates = mutableSetOf<Vector3i>()
    private val entities = mutableListOf<Entity>()
    val offset = Vector3d()
    private val loadedChunkEntities = mutableMapOf<Vector3i, Int>()
    private var loadedChunkEntitiesChanged = false

    fun addBoxEntity(location: Location, itemStack: ItemStack): ArmorStand {
        val armorStand =
            location.world.spawn(
                location.clone().apply { y -= blockCenterHeight; yaw = 0f; pitch = 0f },
                ArmorStand::class.java
            )
        //armorStand.isInvulnerable = true
        //armorStand.isMarker = true
        armorStand.isVisible = false
        armorStand.setGravity(false)
        addRigidBody(
            BoxRigidBody(
                PhysicsEntity.ArmorStandEntity(armorStand).apply {
                    rotation = Quaterniond().rotationZYX(
                        -Math.toRadians(location.pitch).toDouble(), Math.toRadians(location.yaw).toDouble(), 0.0
                    )
                },
                1f, 1f, 1f,
                0f, 0f, 0f,
                true, this,
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
        val updateCount = dynamicsWorld.update(::physicsUpdate, diff)
        renderPercent = dynamicsWorld.time / dynamicsWorld.fixedTimeStep.toDouble()
        if (updateCount > 0) {
            val updateDiff: Float = updateCount.toFloat() * dynamicsWorld.fixedTimeStep
            bodies.removeIf {
                if (it.isDestroyed) return@removeIf true
                if (!it.isKinematicOrFrozen) {
                    updateTransformations(it, updateDiff.toDouble())
                    if (it.entity.isDead) {
                        return@removeIf true
                    }
                }
                return@removeIf false
            }

            explosions.removeIf {
                if (it.tickDelay == 0) {
                    executeExplosion(it)
                    return@removeIf true
                }
                it.tickDelay--
                return@removeIf false
            }
        }
        chunkUpdates.clear()
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

    private fun physicsUpdate(diff: Double) {
        for (body in bodies) {
            if (!body.isKinematicOrFrozen && !body.isDestroyed) {
                body.updatePhysics(diff, blocksChanged)
            }
        }

        blocksChanged = false
        checkChunksToUnload()
        checkLoadedChunks()
        loadedChunkEntitiesChanged = false
    }

    private fun checkLoadedChunks() {
        if (loadedChunkEntitiesChanged) {
            for ((chunk, amount) in loadedChunkEntities) {
                if (amount != 0 && !loadedChunks.contains(chunk)) {
                    val wasLoaded: Boolean = loadChunk(chunk)
                    if (wasLoaded) {
                        loadedChunks.add(chunk)
                    }
                }
            }
        }
    }

    private fun checkChunksToUnload() {
        if (loadedChunkEntitiesChanged) {
            loadedChunks.removeIf {
                if (loadedChunkEntities.getOrDefault(it, 0) <= 0) {
                    unloadChunk(it)
                    true
                } else {
                    false
                }
            }
        }
    }

    fun blockUpdate(pos: Block) {
        blocksChanged = true
        val cx: Int = pos.x shr CHUNK_SIZE_NUM_BITS
        val cy: Int = pos.y shr CHUNK_SIZE_NUM_BITS
        val cz: Int = pos.z shr CHUNK_SIZE_NUM_BITS
        val ax: Int = pos.x and 3
        val ay: Int = pos.y and 3
        val az: Int = pos.z and 3
        updateChunk(cx, cy, cz)
        if (ax == 0) {
            updateChunk(cx - 1, cy, cz)
        }
        if (ay == 0) {
            updateChunk(cx, cy - 1, cz)
        }
        if (az == 0) {
            updateChunk(cx, cy, cz - 1)
        }
        if (ax == 3) {
            updateChunk(cx + 1, cy, cz)
        }
        if (ay == 3) {
            updateChunk(cx, cy + 1, cz)
        }
        if (az == 3) {
            updateChunk(cx, cy, cz + 1)
        }
    }

    private fun updateChunk(cx: Int, cy: Int, cz: Int) {
        val chunkPos = Vector3i(cx, cy, cz)
        if (!chunkUpdates.contains(chunkPos)) {
            chunkUpdates.add(chunkPos)
            if (loadedChunks.contains(chunkPos)) {
                unloadChunk(chunkPos)
                loadChunk(chunkPos)
            }
        }
    }

    fun addLoadedChunkEntity(chunk: Vector3i) {
        for (x in -1..1) {
            for (y in -1..1) {
                for (z in -1..1) {
                    addLoadedChunkEntityOffset(Vector3i(chunk.x + x, chunk.y + y, chunk.z + z))
                }
            }
        }
    }

    private fun addLoadedChunkEntityOffset(loaded: Vector3i) {
        val amount: Int = loadedChunkEntities.getOrDefault(loaded, 0)
        loadedChunkEntities[loaded] = amount + 1
        loadedChunkEntitiesChanged = true
    }

    fun removeLoadedChunkEntity(chunk: Vector3i) {
        for (x in -1..1) {
            for (y in -1..1) {
                for (z in -1..1) {
                    removeLoadedChunkEntityOffset(Vector3i(chunk.x + x, chunk.y + y, chunk.z + z))
                }
            }
        }
    }

    private fun removeLoadedChunkEntityOffset(chunk: Vector3i) {
        val amount: Int = loadedChunkEntities.getOrDefault(chunk, 0)
        loadedChunkEntities[chunk] = amount - 1
        loadedChunkEntitiesChanged = true
    }

    private fun unloadChunk(chunkPos: Vector3i) {
        val bodies = chunkBodies.remove(chunkPos)
            ?: return
        for (body in bodies) {
            dynamicsWorld.removeActor(body.actor)
            body.destroy()
        }
    }

    private fun loadChunk(chunkPos: Vector3i): Boolean {
        if (!(chunkPos.y >= level.minHeight && chunkPos.y < level.maxHeight shr CHUNK_SIZE_NUM_BITS)) return true

        val chunkX = chunkPos.x shr CHUNK_SIZE_RELATIVE_NUM_BITS
        val chunkZ = chunkPos.z shr CHUNK_SIZE_RELATIVE_NUM_BITS
        if (!level.isChunkGenerated(chunkX, chunkZ)) return false

        val bodies = chunkBodies.getOrPut(chunkPos) { mutableListOf() }
        for (x in 0..3) {
            for (y in 0..3) {
                for (z in 0..3) {
                    val pos = level.getBlockAt(chunkPos.x * 4 + x, chunkPos.y * 4 + y, chunkPos.z * 4 + z)
                    val voxelShape = pos.boundingBox
                    if (voxelShape.volume > 0.0 && areNeighboursEmpty(level, pos)) {
                        val entity = PhysicsEntity.BlockEntity()
                        val width = voxelShape.widthX.toFloat()
                        val height = voxelShape.height.toFloat()
                        val depth = voxelShape.widthZ.toFloat()
                        entity.translation = pos.location.let {
                            Vector3d(
                                it.x + width / 2 - offset.x,
                                it.y + height / 2 - offset.y,
                                it.z + depth / 2 - offset.z
                            )
                        }
                        val body = BoxRigidBody(
                            entity,
                            width, height, depth,
                            0.0f, 0.0f, 0.0f,
                            false, this,
                        )
                        dynamicsWorld.addActor(body.actor)
                        bodies.add(body)
                    }
                }
            }
        }
        return true
    }

    private fun areNeighboursEmpty(level: World, pos: Block): Boolean {
        return pos.y >= level.maxHeight || pos.y <= level.minHeight
                || pos.y < level.maxHeight - 1 && pos.getRelative(BlockFace.UP).isTranslucent
                || pos.y > level.minHeight && pos.getRelative(BlockFace.DOWN).isTranslucent
                || pos.getRelative(BlockFace.NORTH).isTranslucent
                || pos.getRelative(BlockFace.EAST).isTranslucent
                || pos.getRelative(BlockFace.SOUTH).isTranslucent
                || pos.getRelative(BlockFace.WEST).isTranslucent
    }

    private val Block.isTranslucent: Boolean
        get() {
            val boundingBox = boundingBox
            return !(boundingBox.minX == 0.0 && boundingBox.minY == 0.0 && boundingBox.minZ == 0.0
                    && boundingBox.maxX == 1.0 && boundingBox.maxY == 1.0 && boundingBox.maxZ == 1.0)
        }

    fun applyExplosion(explosion: Explosion) {
        explosions.add(explosion)
    }

    private fun executeExplosion(explosion: Explosion) {
        val tmp = Vector3d()
        val explosionStrengthSquared = explosion.strength * 2.0 * explosion.strength * 2.0

        for (body in this.bodies) {
            tmp.set(body.entity.translation).add(offset)
            val distanceSquared = explosion.position.distanceSquared(tmp)
            if (distanceSquared <= explosionStrengthSquared) {
                val distance = Math.sqrt(distanceSquared)
                val direction = tmp.sub(explosion.position).normalize()
                direction.y += 2.0
                direction.normalize()
                val realStrength = (1.0 - (distance / (explosion.strength * 2.0)).coerceIn(0.0..1.0)) * 15.0
                if (body.actor is PxRigidBody) {
                    val pxVec = PxVec3(
                        (direction.x * realStrength).toFloat(),
                        (direction.y * realStrength).toFloat(),
                        (direction.z * realStrength).toFloat()
                    )
                    body.actor.addForce(pxVec, PxForceModeEnum.eVELOCITY_CHANGE)
                    pxVec.destroy()
                }
            }
        }
    }

    data class Explosion(
        val position: Vector3d,
        val strength: Float = 0f,
        var tickDelay: Int = 2,
    )

    companion object {
        val CHUNK_SIZE_NUM_BITS = Integer.bitCount(3)
        val CHUNK_SIZE_RELATIVE_NUM_BITS = Integer.bitCount(3)
    }
}