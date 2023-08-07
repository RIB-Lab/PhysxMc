package com.kamesuta.physxmc;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.HashMap;
import java.util.Map;

import static com.kamesuta.physxmc.PhysxMc.displayedBoxHolder;

public class EventHandler implements Listener {
    
    @org.bukkit.event.EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if(event.getHand() == EquipmentSlot.OFF_HAND)
            return;
        
        if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) && PhysxSetting.isDebugMode()) {
            if(event.getAction() == Action.RIGHT_CLICK_AIR){
                DisplayedPhysxBox box = displayedBoxHolder.debugCreate(event.getPlayer());
                if (box != null){
                    box.throwBox(event.getPlayer().getEyeLocation());
                    return;
                }
            }
        }
        if ((event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) && PhysxSetting.isDebugMode()) {
            if(PhysxMc.grabTool.isGrabbing(event.getPlayer())){
                event.setCancelled(true);
                PhysxMc.grabTool.release(event.getPlayer());
            }
            else{
                if(event.getItem() != null && event.getItem().getType() == Material.STICK){
                    DisplayedPhysxBox box = displayedBoxHolder.raycast(event.getPlayer().getEyeLocation(), 4);
                    if (box != null){
                        box.throwBox(event.getPlayer().getEyeLocation());
                    }
                    event.setCancelled(true);
                    return;
                }
                
                if(PhysxMc.grabTool.tryGrab(event.getPlayer()))
                    event.setCancelled(true);
            }
        }
    }

    @org.bukkit.event.EventHandler
    public void onExplosion(EntityExplodeEvent event) {
        displayedBoxHolder.executeExplosion(event.getLocation(), 6.9f);
    }

    @org.bukkit.event.EventHandler
    public void onPlayerLogout(PlayerQuitEvent event){
        PhysxMc.grabTool.release(event.getPlayer());
    }
}
