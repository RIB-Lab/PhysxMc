package com.kamesuta.physxmc.physics

import com.kamesuta.physxmc.physx.Physx
import org.joml.RoundingMode
import org.joml.Vector3d
import org.joml.Vector3i
import physx.common.PxQuat
import physx.common.PxTransform
import physx.common.PxVec3
import physx.extensions.PxRigidBodyExt
import physx.geomutils.PxBoxGeometry
import physx.physics.*
import kotlin.math.abs

class BoxRigidBody(
    val entity: PhysicsEntity,
    val width: Float,
    val height: Float,
    val depth: Float,
    val offsetx: Float,
    val offsety: Float,
    val offsetz: Float,
    val dynamic: Boolean,
    val physics: PhysicsWorld,
) {
    val shape: PxShape
    val actor: PxRigidActor
    var isDestroyed = false
        private set
    private val tmpPos = Vector3i()
    private var lastChunk: Vector3i? = null

    val isKinematicOrFrozen
        get() = actor.actorFlags.isSet(PxActorFlagEnum.eDISABLE_SIMULATION)
                || actor !is PxRigidDynamic || actor.rigidBodyFlags.isSet(PxRigidBodyFlagEnum.eKINEMATIC)

    init {
        val scale = Vector3d(1.0, 1.0, 1.0)
        val translation = entity.translation
        val rotation = entity.rotation
        val mem = mutableListOf<() -> Unit>()

        try {
            val shapeFlags = PxShapeFlags(PxShapeFlagEnum.eSIMULATION_SHAPE.toByte()).also { mem.add { it.destroy() } }
            val tmpVec = PxVec3(
                translation.x().toFloat(),
                translation.y().toFloat(),
                translation.z().toFloat()
            ).also { mem.add { it.destroy() } }
            val tmpQuat = PxQuat(
                rotation.x().toFloat(),
                rotation.y().toFloat(),
                rotation.z().toFloat(),
                rotation.w().toFloat()
            ).also { mem.add { it.destroy() } }
            val tmpPose = PxTransform(tmpVec, tmpQuat).also { mem.add { it.destroy() } }
            val tmpFilterData = PxFilterData(
                physicsGroup,
                physicsMask,
                if (dynamic) REPORT_CONTACT_FLAGS else 0,
                0
            ).also { mem.add { it.destroy() } }
            val boxGeometry = PxBoxGeometry(
                width * 0.5f * abs(scale.x).toFloat(),
                height * 0.5f * abs(scale.y).toFloat(),
                depth * 0.5f * abs(scale.z).toFloat(),
            ).also { mem.add { it.destroy() } }
            val boxShape: PxShape =
                Physx.instance.physics.createShape(boxGeometry, Physx.instance.defaultMaterial, true, shapeFlags)
            if (offsetx != 0.0f || offsety != 0.0f || offsetz != 0.0f) {
                boxShape.localPose = PxTransform(
                    PxVec3(offsetx, offsety, offsetz),
                    PxQuat(0.0f, 0.0f, 0.0f, 1.0f)
                ).also { mem.add { it.destroy() } }
            }
            val box = if (dynamic) {
                Physx.instance.physics.createRigidDynamic(tmpPose)
            } else {
                Physx.instance.physics.createRigidStatic(tmpPose)
            }
            boxShape.simulationFilterData = tmpFilterData
            box.attachShape(boxShape)
            if (box is PxRigidDynamic) {
                PxRigidBodyExt.updateMassAndInertia(box, 0.1f)
                box.contactReportThreshold = 0.25f
                box.maxDepenetrationVelocity = 2.5f
            }

            shape = boxShape
            actor = box
        } catch (e: Throwable) {
            runCatching {
                mem.asReversed().forEach { it() }
            }.onFailure {
                e.addSuppressed(it)
            }
            throw e
        }
        mem.asReversed().forEach { it() }
    }

    fun destroy() {
        if (!isDestroyed) {
            shape.release()
            actor.release()
        }
        isDestroyed = true
    }

    private fun loadChunkPhysics(x: Float, y: Float, z: Float) {
        val offset: Vector3d = this.physics.offset
        this.tmpPos.set(Vector3i(x + offset.x, y + offset.y, z + offset.z, RoundingMode.FLOOR))
        if ((this.tmpPos != Vector3i(entity.translation, RoundingMode.FLOOR)) && !this.isKinematicOrFrozen) {
            val cx: Int = this.tmpPos.x shr PhysicsWorld.CHUNK_SIZE_NUM_BITS
            val cy: Int = this.tmpPos.y shr PhysicsWorld.CHUNK_SIZE_NUM_BITS
            val cz: Int = this.tmpPos.z shr PhysicsWorld.CHUNK_SIZE_NUM_BITS

            var lastChunk1 = lastChunk
            if (lastChunk1 == null || !lastChunk1.equals(cx, cy, cz)) {
                if (lastChunk1 == null) {
                    lastChunk1 = Vector3i(cx, cy, cz).also { lastChunk = it }
                } else {
                    physics.removeLoadedChunkEntity(lastChunk1)
                    lastChunk1.set(cx, cy, cz)
                }
                physics.addLoadedChunkEntity(lastChunk1)
            }
        }
    }

    fun updatePhysics(diff: Double, blocksChanged: Boolean) {
        loadChunkPhysics(actor.globalPose.p.x, actor.globalPose.p.y, actor.globalPose.p.z)
    }

    companion object {
        val REPORT_CONTACT_FLAGS = PxPairFlagEnum.eNOTIFY_THRESHOLD_FORCE_FOUND or PxPairFlagEnum.eNOTIFY_CONTACT_POINTS
        val physicsGroup = 2
        val physicsMask = 23
    }
}