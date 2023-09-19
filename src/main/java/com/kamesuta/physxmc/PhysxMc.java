package com.kamesuta.physxmc;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.kamesuta.physxmc.command.PhysxCommands;
import com.kamesuta.physxmc.core.Physx;
import com.kamesuta.physxmc.core.PhysxTerrain;
import com.kamesuta.physxmc.utils.BoundingBoxUtil;
import com.kamesuta.physxmc.utils.ConversionUtility;
import com.kamesuta.physxmc.utils.PhysxLoader;
import com.kamesuta.physxmc.widget.EventHandler;
import com.kamesuta.physxmc.widget.GrabTool;
import com.kamesuta.physxmc.widget.PlayerTriggerHolder;
import com.kamesuta.physxmc.wrapper.DisplayedBoxHolder;
import com.kamesuta.physxmc.wrapper.IntegratedPhysxWorld;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public final class PhysxMc extends JavaPlugin {
    private static PhysxMc INSTANCE;

    @Getter
    private Physx physx;
    @Getter
    private IntegratedPhysxWorld physxWorld;
    @Getter
    private DisplayedBoxHolder displayedBoxHolder;
    @Getter
    private PlayerTriggerHolder playerTriggerHolder;

    @Getter
    private GrabTool grabTool;
    @Getter
    private ProtocolManager protocolManager;

    public PhysxMc(){
        INSTANCE = this;
    }
    
    @Override
    public void onLoad() {
        CommandAPI.onLoad(new CommandAPIBukkitConfig(this));
        PhysxCommands.registerCommands();
    }

    public static PhysxMc instance() {
        return INSTANCE;
    }
    
    @Override
    public void onEnable() {
        CommandAPI.onEnable();
        
        try {
            PhysxLoader.loadPhysxOnAppClassloader();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        physx = new Physx();
        physxWorld = new IntegratedPhysxWorld();
        physxWorld.setUpScene();
        displayedBoxHolder = new DisplayedBoxHolder();
        playerTriggerHolder = new PlayerTriggerHolder();
        grabTool = new GrabTool();

        new BukkitRunnable() {
            @Override
            public void run() {
                physxWorld.tick();
                displayedBoxHolder.update();
                playerTriggerHolder.update();
                grabTool.update();
            }
        }.runTaskTimer(this, 1, 1);

        getServer().getPluginManager().registerEvents(new EventHandler(), this);

        initProtocolLib();
        try {
            BoundingBoxUtil.init();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }

        forceInit(PhysxTerrain.class);
    }

    private void initProtocolLib() {
        protocolManager = ProtocolLibrary.getProtocolManager();
        protocolManager.addPacketListener(new PacketAdapter(this, ListenerPriority.NORMAL, PacketType.Play.Server.BLOCK_CHANGE) {
            @Override
            public void onPacketSending(PacketEvent event) {
                PacketContainer packet = event.getPacket();
                BlockPosition pos = packet.getBlockPositionModifier().read(0);
                physxWorld.registerChunksToReloadNextSecond(event.getPlayer().getWorld().getBlockAt(pos.getX(), pos.getY(), pos.getZ()).getChunk());
            }
        });
        protocolManager.addPacketListener(new PacketAdapter(this, ListenerPriority.NORMAL, PacketType.Play.Server.MULTI_BLOCK_CHANGE) {
            @Override
            public void onPacketSending(PacketEvent event) {
                PacketContainer packet = event.getPacket();
                List<Location> locations = new ArrayList<>();

                var sectionPos = packet.getSectionPositions().read(0);
                var shortLocations = packet.getShortArrays().read(0);

                for (short shortLocation : shortLocations) {
                    var loc = ConversionUtility.convertShortLocation(event.getPlayer().getWorld(), sectionPos, shortLocation);
                    locations.add(loc);
                }

                for (Location location : locations) {
                    physxWorld.registerChunksToReloadNextSecond(location.getChunk());
                }
            }
        });
    }

    @Override
    public void onDisable() {
        CommandAPI.onDisable();
        
        if (displayedBoxHolder != null) {
            displayedBoxHolder.destroyAll();
            playerTriggerHolder.destroyAll();
        }

        if (physx != null) {
            physxWorld.destroyScene();
            physx.terminate();
        }
    }

    /**
     * BukkitのOnDisableでエラーが出ないようにクラスを強制的にロードする
     */
    public static <T> Class<T> forceInit(Class<T> klass) {
        try {
            Class.forName(klass.getName(), true, klass.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new AssertionError(e);  // Can't happen
        }
        return klass;
    }
}
