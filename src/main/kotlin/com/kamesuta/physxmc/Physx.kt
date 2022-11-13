package com.kamesuta.physxmc

import physx.PxTopLevelFunctions
import physx.common.PxDefaultErrorCallback
import physx.common.PxErrorCallback
import physx.common.PxFoundation
import physx.common.PxTolerancesScale
import physx.cooking.PxConvexMeshCookingTypeEnum
import physx.cooking.PxCooking
import physx.cooking.PxCookingParams
import physx.extensions.PxDefaultAllocator
import physx.physics.PxMaterial
import physx.physics.PxPhysics

class Physx private constructor() {
    val allocator: PxDefaultAllocator
    val errorCb: PxErrorCallback
    val physics: PxPhysics
    val tolerances: PxTolerancesScale
    val cooking: PxCooking
    val defaultMaterial: PxMaterial
    val foundation: PxFoundation
    val cookingParams: PxCookingParams

    /** 初期化 */
    init {
        val version = PxTopLevelFunctions.getPHYSICS_VERSION()
        allocator = PxDefaultAllocator()
        errorCb = PxDefaultErrorCallback()
        foundation = PxTopLevelFunctions.CreateFoundation(version, allocator, errorCb)
        tolerances = PxTolerancesScale()
        physics = PxTopLevelFunctions.CreatePhysics(version, foundation, tolerances)
        cookingParams = PxCookingParams(tolerances)
        cookingParams.convexMeshCookingType = PxConvexMeshCookingTypeEnum.eQUICKHULL
        cookingParams.suppressTriangleMeshRemapTable = true
        cookingParams.buildGPUData = false
        cooking = PxTopLevelFunctions.CreateCooking(version, foundation, cookingParams)
        defaultMaterial = physics.createMaterial(1.0f, 1.0f, 0.0f)
    }

    /** 終了処理 */
    private fun release() {
        defaultMaterial.release()
        cooking.release()
        cookingParams.destroy()
        physics.release()
        tolerances.destroy()
        foundation.release()
        errorCb.destroy()
        allocator.destroy()
    }

    companion object {
        private var physx: Physx? = null

        /** シングルトンインスタンス */
        val instance: Physx get() = physx!!

        /** 初期化 */
        fun init() {
            physx = Physx()
        }

        /** 終了処理 */
        fun release() {
            physx?.release()
            physx = null
        }
    }
}
