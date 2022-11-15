package com.kamesuta.physxmc

import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.HumanEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
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
    }

    override fun onDisable() {
        // Plugin shutdown logic
        physicsWorld.destroy()
        Physx.release()
    }

    var lastNanoTime = System.nanoTime()

    private fun tick() {
        val nanoTime = System.nanoTime()
        physicsWorld.update((nanoTime - lastNanoTime) / 1_000_000_000.0)
        lastNanoTime = nanoTime
    }

    @EventHandler
    fun onDamage(event: EntityDamageByEntityEvent) {
        val player = event.damager as? HumanEntity
            ?: return
        if (player.inventory.itemInOffHand.type != Material.STICK && player.inventory.itemInOffHand.type != Material.BLAZE_ROD) return
        // キャンセル
        event.isCancelled = true

        val armorStand = event.entity as? ArmorStand
            ?: return

        val force = player.eyeLocation.direction.clone().multiply(100)
        physicsWorld.addForce(armorStand, force)
    }

    @EventHandler
    fun onPlayerRightClick(event: PlayerInteractEvent) {
        if (event.hand != EquipmentSlot.HAND) return
        val player = event.player
        if (player.inventory.itemInOffHand.type != Material.STICK && player.inventory.itemInOffHand.type != Material.BLAZE_ROD) return
        // キャンセル
        event.isCancelled = true

        if (event.action != Action.RIGHT_CLICK_BLOCK && event.action != Action.RIGHT_CLICK_AIR) return
        if (player.inventory.itemInMainHand.type.isAir || !player.inventory.itemInMainHand.type.isBlock) return

        val spawnLocation = player.eyeLocation.clone().add(player.eyeLocation.direction)
        val boxEntity = physicsWorld.addBoxEntity(spawnLocation, player.inventory.itemInMainHand)

        if (player.inventory.itemInOffHand.type == Material.STICK) {
            val force = player.eyeLocation.direction.clone().multiply(100)
            physicsWorld.addForce(boxEntity, force)
        }
    }

    @EventHandler
    fun onBlockChanged(event: BlockPlaceEvent) {
        physicsWorld.blockUpdate(event.block)
    }

    @EventHandler
    fun onBlockChanged(event: BlockBreakEvent) {
        physicsWorld.blockUpdate(event.block)
    }
}