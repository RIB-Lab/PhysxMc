package com.kamesuta.physxmc

import org.bukkit.World
import org.joml.Vector3f
import physx.PxTopLevelFunctions
import physx.common.PxVec3
import physx.physics.*

class DynamicsWorld(level: World, fixedTimeStep: Float) {
    private val sceneDesc: PxSceneDesc
    private val scene: PxScene
    val fixedTimeStep: Float
    var isDestroyed: Boolean
        private set
    var time = 0.0
        private set
    private var skipFirst = false
    private val level: World
    private var gravity: Vector3f? = null
    //private var contactCallback: ContactSimulationCallback? = null

    init {
        this.level = level
        isDestroyed = false
        this.fixedTimeStep = fixedTimeStep
        val numThreads = ConfigClient.cpuThreads
        sceneDesc = PxSceneDesc(Physx.instance.tolerances)
        val gravity: Vector3f = DEFAULT_GRAVITY
        var gravityVec: PxVec3? = null
        try {
            gravityVec = PxVec3(gravity.x, gravity.y, gravity.z)
            sceneDesc.gravity = gravityVec
            this.gravity = Vector3f(gravity.x, gravity.y, gravity.z)
        } finally {
            gravityVec?.destroy()
        }
        sceneDesc.cpuDispatcher = PxTopLevelFunctions.DefaultCpuDispatcherCreate(numThreads)
        sceneDesc.filterShader = PxTopLevelFunctions.DefaultFilterShader()
        sceneDesc.solverType = PxSolverTypeEnum.ePGS
        sceneDesc.kineKineFilteringMode = PxPairFilteringModeEnum.eKILL
        sceneDesc.staticKineFilteringMode = PxPairFilteringModeEnum.eKILL
        sceneDesc.sceneQueryUpdateMode = PxSceneQueryUpdateModeEnum.eBUILD_DISABLED_COMMIT_DISABLED
        //sceneDesc.simulationEventCallback = ContactSimulationCallback(level).also { contactCallback = it }
        sceneDesc.flags.set(PxSceneFlagEnum.eEXCLUDE_KINEMATICS_FROM_ACTIVE_ACTORS)
        scene = Physx.instance.physics.createScene(sceneDesc)
    }

    fun getGravity(): Vector3f? {
        return gravity
    }

    fun update(physicsUpdate: (diff: Double) -> Unit, diff: Double): Int {
        var updateCount = 0
        if (!isDestroyed) {
            time += diff
            while (time >= fixedTimeStep.toDouble()) {
                time -= fixedTimeStep.toDouble()
                if (!skipFirst) {
                    skipFirst = true
                } else {
                    scene.fetchResults(true)
                    physicsUpdate(fixedTimeStep.toDouble())
                }
                scene.simulate(fixedTimeStep)
                //contactCallback.soundCount = 0
                //contactCallback.tick(fixedTimeStep)
                ++updateCount
            }
        }
        return updateCount
    }

    fun willUpdate(diff: Double): Boolean {
        return !isDestroyed && time + diff >= fixedTimeStep.toDouble()
    }

    fun destroy() {
        if (!isDestroyed) {
            scene.fetchResults(true)
            scene.release()
            //if (contactCallback != null) {
            //    contactCallback.destroy()
            //}
        }
        isDestroyed = true
    }

    fun addActor(actor: PxActor?) {
        scene.addActor(actor)
    }

    fun addAggregate(aggregate: PxAggregate?) {
        scene.addAggregate(aggregate)
    }

    fun removeActor(actor: PxActor?) {
        scene.removeActor(actor)
    }

    fun removeAggregate(aggregate: PxAggregate?) {
        scene.removeAggregate(aggregate)
    }

    fun addArticulation(articulation: PxArticulationBase?) {
        scene.addArticulation(articulation)
    }

    fun removeArticulation(articulation: PxArticulationBase?) {
        scene.removeArticulation(articulation)
    }

    companion object {
        val DEFAULT_GRAVITY: Vector3f = Vector3f(0.0f, -9.81f, 0.0f)
        val DEFAULT_BUOYANCY: Vector3f = Vector3f(0.0f, 2.0f, 0.0f)
    }
}