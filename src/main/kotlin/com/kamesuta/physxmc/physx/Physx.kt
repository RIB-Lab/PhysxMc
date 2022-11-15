package com.kamesuta.physxmc.physx

import com.kamesuta.physxmc.PhysxMc
import physx.PxTopLevelFunctions
import physx.common.JavaErrorCallback
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
    val foundation: PxFoundation
    val tolerances: PxTolerancesScale
    val physics: PxPhysics
    val cookingParams: PxCookingParams
    val cooking: PxCooking
    val defaultMaterial: PxMaterial

    /** 初期化 */
    init {
        val version = PxTopLevelFunctions.getPHYSICS_VERSION()
        allocator = PxDefaultAllocator()
        errorCb = object : JavaErrorCallback() {
            override fun reportError(code: Int, message: String?, file: String?, line: Int) {
                PhysxMc.instance.logger.severe("PhysX Error: code=$code, message=$message, file=$file, line=$line")
            }
        }
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
