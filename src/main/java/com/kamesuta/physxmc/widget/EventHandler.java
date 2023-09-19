package com.kamesuta.physxmc.widget;

import com.kamesuta.physxmc.PhysxMc;
import com.kamesuta.physxmc.PhysxSetting;
import com.kamesuta.physxmc.wrapper.DisplayedPhysxBox;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * Bukkitのイベントを受信するクラス
 */
public class EventHandler implements Listener {

    @org.bukkit.event.EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND || !PhysxSetting.isDebugMode())
            return;

        if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            if (event.getAction() == Action.RIGHT_CLICK_AIR) {
                DisplayedPhysxBox box = PhysxMc.instance().getDisplayedBoxHolder().debugCreate(event.getPlayer());
                if (box != null) {
                    box.throwBox(event.getPlayer().getEyeLocation());
                    return;
                }
            }
            if(event.getAction() == Action.RIGHT_CLICK_BLOCK){
                if(event.getItem() == null && event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.DIAMOND_BLOCK){
                    event.setCancelled(true);
                    List<Vector> offsets = scanOffsets(event.getInteractionPoint(), Material.DIAMOND_BLOCK);
                    PhysxMc.instance().getDisplayedBoxHolder().getOffsetMap().put(event.getPlayer(), offsets);
                    event.getPlayer().sendMessage("連結する形状を記憶しました");
                }
            }
        }
        if ((event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK)) {
            if (PhysxMc.instance().getGrabTool().isGrabbing(event.getPlayer())) {
                event.setCancelled(true);
                PhysxMc.instance().getGrabTool().release(event.getPlayer());
            } else {
                if (event.getItem() != null && event.getItem().getType() == Material.STICK) {
                    DisplayedPhysxBox box = PhysxMc.instance().getDisplayedBoxHolder().raycast(event.getPlayer().getEyeLocation(), 4);
                    if (box != null) {
                        box.throwBox(event.getPlayer().getEyeLocation());
                    }
                    event.setCancelled(true);
                    return;
                }

                if (event.getItem() != null && event.getItem().getType() == Material.FLINT_AND_STEEL) {
                    DisplayedPhysxBox box = PhysxMc.instance().getDisplayedBoxHolder().raycast(event.getPlayer().getEyeLocation(), 4);
                    if (box != null) {
                        PhysxMc.instance().getDisplayedBoxHolder().destroySpecific(box);
                    }
                    event.setCancelled(true);
                    return;
                }

                if (PhysxMc.instance().getGrabTool().tryGrab(event.getPlayer()))
                    event.setCancelled(true);
            }
        }
    }

    @org.bukkit.event.EventHandler
    public void onExplosion(EntityExplodeEvent event) {
        PhysxMc.instance().getDisplayedBoxHolder().executeExplosion(event.getLocation(), 6.9f);
    }

    @org.bukkit.event.EventHandler
    public void onPlayerLogout(PlayerQuitEvent event) {
        PhysxMc.instance().getGrabTool().release(event.getPlayer());
    }
    
    public static List<Vector> scanOffsets(Location location, Material material){
        List<Vector> offsets = new ArrayList<>();
        for (int i = -5; i <= 5; i++) {
            for (int j = -5; j <= 5; j++) {
                for (int k = -5; k <= 5; k++) {
                    Block block = location.getBlock().getRelative(i,j,k);
                    if(block.getType() == material)
                        offsets.add(new Vector(i,j,k));
                }
            }
        }
        if(offsets.size() == 0)
            offsets.add(new Vector());
        
        return offsets;
    }
}
