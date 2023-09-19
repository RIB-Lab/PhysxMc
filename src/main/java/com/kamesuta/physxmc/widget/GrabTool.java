package com.kamesuta.physxmc.widget;

import com.kamesuta.physxmc.PhysxMc;
import com.kamesuta.physxmc.wrapper.DisplayedPhysxBox;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;

import java.util.HashMap;
import java.util.Map;

/**
 * 物理オブジェクトをMinecraft世界で掴めるようにするクラス
 */
public class GrabTool {

    private final Map<Player, DisplayedPhysxBox> grabbedPlayerMap = new HashMap<>();
    private final Map<Player, Quaternionf> originalRotationMap = new HashMap<>();

    public GrabTool() {

    }

    /**
     * プレイヤーの視線の先のブロックを掴む
     *
     * @param player 　プレイヤー
     * @return 掴んだかどうか
     */
    public boolean tryGrab(Player player) {
        DisplayedPhysxBox box = PhysxMc.instance().getDisplayedBoxHolder().raycast(player.getEyeLocation(), 10);
        if (box == null || grabbedPlayerMap.containsValue(box)) {
            return false;
        }

        grabbedPlayerMap.put(player, box);

        // プレイヤー始点の回転
        Location eyeLocation = player.getEyeLocation().clone();
        Quaternionf playerQuatInvert = new Quaternionf()
                .rotateY((float) -Math.toRadians(eyeLocation.getYaw()))
                .rotateX((float) Math.toRadians(eyeLocation.getPitch()))
                .invert();

        Quaternionf boxDiffQuat = new Quaternionf()
                .mul(playerQuatInvert)
                .mul(box.getQuat());

        originalRotationMap.put(player, boxDiffQuat);
        box.makeKinematic(true);
        return true;
    }

    /**
     * 掴んだブロックを開放する
     *
     * @param player
     */
    public void release(Player player) {
        if (!isGrabbing(player))
            return;

        if (grabbedPlayerMap.get(player) != null)
            grabbedPlayerMap.get(player).makeKinematic(false);
        grabbedPlayerMap.remove(player);
        originalRotationMap.remove(player);
    }

    /**
     * 掴んでいるかどうか
     *
     * @param player
     * @return
     */
    public boolean isGrabbing(Player player) {
        return grabbedPlayerMap.containsKey(player);
    }

    public void update() {
        updateGrabbingObjPos();
    }

    /**
     * 掴んでいる物理オブジェクトの座標をアップデートする
     */
    private void updateGrabbingObjPos() {
        for (Map.Entry<Player, DisplayedPhysxBox> entry : grabbedPlayerMap.entrySet()) {
            if (!PhysxMc.instance().getDisplayedBoxHolder().hasBox(entry.getValue()) || !originalRotationMap.containsKey(entry.getKey())) { //既に他の要因でboxが取り除かれている場合
                grabbedPlayerMap.remove(entry.getKey());
                originalRotationMap.remove(entry.getKey());
                return;
            }

            Location eyeLocation = entry.getKey().getEyeLocation().clone();
            Vector playerDir = eyeLocation.getDirection().clone().normalize().multiply(3);
            eyeLocation.add(playerDir);

            //元のブロックの回転を追加でかける
            Quaternionf playerQuat = new Quaternionf()
                    .rotateY((float) -Math.toRadians(eyeLocation.getYaw()))
                    .rotateX((float) Math.toRadians(eyeLocation.getPitch()));

            Quaternionf boxDiffQuat = originalRotationMap.get(entry.getKey());

            Quaternionf boxQuat = new Quaternionf()
                    .mul(playerQuat)
                    .mul(boxDiffQuat);

            entry.getValue().moveKinematic(eyeLocation.toVector(), boxQuat);
        }
    }

    /**
     * 掴んでいる状態を全リセット
     * 掴んでいるブロックは残るので注意
     */
    public void forceClear() {
        grabbedPlayerMap.clear();
        originalRotationMap.clear();
    }
}
