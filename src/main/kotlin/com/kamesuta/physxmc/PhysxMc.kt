package com.kamesuta.physxmc

import org.bukkit.Location
import org.bukkit.entity.ArmorStand
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin

class PhysxMc : JavaPlugin(), Listener {
    lateinit var physicsWorld: PhysicsWorld

    override fun onEnable() {
        // Plugin startup logic
        PhysxLoader.loadPhysxOnAppClassloader()
        Physx.init()

        // TODO: ワールド取得
        physicsWorld = PhysicsWorld(server.worlds[0])

        server.pluginManager.registerEvents(this, this)
        server.scheduler.runTaskTimer(this, this::tick, 0, 0)

        val armorStand = physicsWorld.level.spawn(Location(physicsWorld.level, 0.0, 10.0, 0.0), ArmorStand::class.java)
        armorStand.isInvulnerable = true
        armorStand.isSmall = true
        armorStand.isMarker = true
        armorStand.setGravity(false)
        armorStand.isGlowing = true
        physicsWorld.addRigidBody(BoxRigidBody(PhysicsEntity.MobEntity(armorStand), 1f, 1f, 1f, 0f, 0f, 0f, true))
    }

    override fun onDisable() {
        // Plugin shutdown logic
        physicsWorld.destroy()
        Physx.release()
    }

    private fun tick() {
        physicsWorld.update(0.025)
    }
}