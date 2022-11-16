package com.kamesuta.physxmc

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.ProtocolManager
import com.comphenix.protocol.events.ListenerPriority
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketEvent
import com.kamesuta.physxmc.physics.PhysicsWorld
import com.kamesuta.physxmc.physx.Physx
import com.kamesuta.physxmc.physx.PhysxLoader
import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.HumanEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.plugin.java.JavaPlugin

class PhysxMc : JavaPlugin(), Listener {
    /** 物理演算ワールド */
    lateinit var physicsWorld: PhysicsWorld

    /** ProtocolManagerインスタンス */
    lateinit var protocolManager: ProtocolManager

    override fun onEnable() {
        // プラグインインスタンスをstaticフィールドに保存
        instance = this
        // ProtocolLibを初期化
        protocolManager = ProtocolLibrary.getProtocolManager()

        // Plugin startup logic
        PhysxLoader.loadPhysxOnAppClassloader()
        Physx.init()

        // TODO: ワールド取得
        physicsWorld = PhysicsWorld(server.worlds[0])

        server.pluginManager.registerEvents(this, this)
        server.scheduler.runTaskTimer(this, this::tick, 0, 0)

        // ブロック変更時
        protocolManager.addPacketListener(object : PacketAdapter(
            this,
            ListenerPriority.NORMAL,
            PacketType.Play.Server.BLOCK_CHANGE,
        ) {
            /** 送信 (サーバー→クライアント) */
            override fun onPacketSending(event: PacketEvent) {
                val packet = event.packet
                val pos = packet.blockPositionModifier.read(0)
                physicsWorld.blockUpdate(event.player.world.getBlockAt(pos.x, pos.y, pos.z))
            }
        })
        protocolManager.addPacketListener(object : PacketAdapter(
            this,
            ListenerPriority.NORMAL,
            PacketType.Play.Server.MULTI_BLOCK_CHANGE,
        ) {
            /** 送信 (サーバー→クライアント) */
            override fun onPacketSending(event: PacketEvent) {
                val packet = event.packet
                val chunk = packet.sectionPositions.read(0)
                val changePositions = packet.shortArrays.read(0)
                for (changePosition in changePositions) {
                    val pos = changePosition.toInt()
                    val x = (chunk.x shl 4) + (pos shr 8 and 0xF)
                    val y = (chunk.y shl 4) + (pos shr 0 and 0xF)
                    val z = (chunk.z shl 4) + (pos shr 4 and 0xF)
                    physicsWorld.blockUpdate(event.player.world.getBlockAt(x, y, z))
                }
            }
        })
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
        val size = if (player.inventory.itemInMainHand.type == Material.SPONGE) 7.5f else 1f
        val boxEntity = physicsWorld.addBoxEntity(spawnLocation, player.inventory.itemInMainHand, size = size)

        if (player.inventory.itemInOffHand.type == Material.STICK) {
            val force = player.eyeLocation.direction.clone().multiply(100)
            physicsWorld.addForce(boxEntity, force)
        }
    }

    @EventHandler
    fun onExplosion(event: EntityExplodeEvent) {
        physicsWorld.applyExplosion(PhysicsWorld.Explosion(event.location.toVector().toJoml(), 6.9f))
    }

    companion object {
        /** プラグインインスタンス */
        lateinit var instance: PhysxMc
            private set
    }
}