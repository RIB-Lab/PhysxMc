package com.kamesuta.physxmc

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.ArmorStand
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.util.EulerAngle
import org.joml.Quaterniond
import org.joml.Vector3d
import physx.common.PxVec3
import physx.physics.PxRigidDynamic

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
        //armorStand.isInvulnerable = true
        //armorStand.isMarker = true
        armorStand.setItem(EquipmentSlot.HEAD, ItemStack(Material.TNT))
        armorStand.isSmall = true
        armorStand.setGravity(false)
        armorStand.isGlowing = true
        physicsWorld.addRigidBody(
            BoxRigidBody(
                PhysicsEntity.ArmorStandEntity(armorStand),
                1f,
                1f,
                1f,
                0f,
                0f,
                0f,
                true
            )
        )
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

        val armorStand = armorStand ?: return
        val location = armorStand.location.clone().apply { y += armorStand.eyeHeight * 2 }
        val directionX = rotate.transform(Vector3d(1.0, 0.0, 0.0)).toBukkit()
        val directionY = rotate.transform(Vector3d(0.0, 1.0, 0.0)).toBukkit()
        val directionZ = rotate.transform(Vector3d(0.0, 0.0, 1.0)).toBukkit()
        armorStand.world.spawnParticle(Particle.FLAME, location, 1, 0.0, 0.0, 0.0, 0.0)
        armorStand.world.spawnParticle(
            Particle.SOUL_FIRE_FLAME,
            location.clone().add(directionX),
            1,
            0.0,
            0.0,
            0.0,
            0.0
        )
        armorStand.world.spawnParticle(
            Particle.SOUL_FIRE_FLAME,
            location.clone().add(directionX.clone().multiply(2)),
            1,
            0.0,
            0.0,
            0.0,
            0.0
        )
        armorStand.world.spawnParticle(
            Particle.SOUL_FIRE_FLAME,
            location.clone().add(directionY),
            1,
            0.0,
            0.0,
            0.0,
            0.0
        )
        armorStand.world.spawnParticle(
            Particle.SOUL_FIRE_FLAME,
            location.clone().add(directionY.clone().multiply(2)),
            1,
            0.0,
            0.0,
            0.0,
            0.0
        )
        armorStand.world.spawnParticle(Particle.FLAME, location.clone().add(directionZ), 1, 0.0, 0.0, 0.0, 0.0)
        armorStand.world.spawnParticle(
            Particle.FLAME,
            location.clone().add(directionZ.clone().multiply(2)),
            1,
            0.0,
            0.0,
            0.0,
            0.0
        )
    }

    @EventHandler
    fun onDamage(event: EntityDamageByEntityEvent) {
        val damager = event.damager.location
        val armorStand = event.entity as? ArmorStand
            ?: return
        val rigidBody = physicsWorld.findEntity(armorStand)
            ?: return
        val rigidDynamic = rigidBody.actor as? PxRigidDynamic
            ?: return
        // キャンセル
        event.isCancelled = true
        // 力を加える
        val force = damager.direction.clone().multiply(100)
        val pxForce = PxVec3(force.x.toFloat(), force.y.toFloat(), force.z.toFloat())
        rigidDynamic.addForce(pxForce)
        pxForce.destroy()
    }

    val rotate = Quaterniond()
    var armorStand: ArmorStand? = null

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
        if (command.name != "rotationtest") return false
        if (args == null) return false

        if (armorStand == null) {
            val armor = physicsWorld.level.spawn(Location(physicsWorld.level, 0.0, 10.0, 0.0), ArmorStand::class.java)
            //armorStand.isInvulnerable = true
            //armorStand.isMarker = true
            armor.setItem(EquipmentSlot.HEAD, ItemStack(Material.TNT))
            armor.isSmall = true
            armor.setGravity(false)
            armor.isGlowing = true
            armor.addScoreboardTag("rotationtest")
            armorStand = armor
        }
        val armorStand = armorStand
            ?: return false

        if (args.isEmpty()) return false

        when (args[0]) {
            "0" -> {
                rotate.identity()
                armorStand.teleport(armorStand.location.apply { yaw = 0f; pitch = 0f })
            }
            "1" -> {}

            "x+" -> rotate.rotateX(Math.toRadians(10.0))
            "x-" -> rotate.rotateX(Math.toRadians(-10.0))
            "y+" -> rotate.rotateY(Math.toRadians(10.0))
            "y-" -> rotate.rotateY(Math.toRadians(-10.0))
            "z+" -> rotate.rotateZ(Math.toRadians(10.0))
            "z-" -> rotate.rotateZ(Math.toRadians(-10.0))
            "yaw+" -> armorStand.teleport(armorStand.location.apply { yaw += 10f })
            "yaw-" -> armorStand.teleport(armorStand.location.apply { yaw -= 10f })
            "pitch+" -> armorStand.teleport(armorStand.location.apply { pitch += 10f })
            "pitch-" -> armorStand.teleport(armorStand.location.apply { pitch -= 10f })
            else -> return false
        }

        val seq = args.getOrNull(1)?.let { p -> RotSeq.values().find { it.name == p } } ?: return false

        val euler = rotate.quaternion2Euler(Vector3d(), seq)
        //val euler = rotate.getEulerAnglesZYX(Vector3d())
        armorStand.headPose = EulerAngle(euler.x, -euler.y, -euler.z)

        return true
    }
}