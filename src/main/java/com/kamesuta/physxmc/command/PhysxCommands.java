package com.kamesuta.physxmc.command;

import com.kamesuta.physxmc.PhysxMc;
import com.kamesuta.physxmc.PhysxSetting;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.FloatArgument;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.List;

public class PhysxCommands {

    public static void registerCommands() {
        CommandAPICommand mainCommand = new CommandAPICommand("physxmc")
                .withPermission(CommandPermission.OP)
                .executesPlayer((player, args) -> {
                });

        CommandAPICommand resetCommand = new CommandAPICommand("reset")
                .withPermission(CommandPermission.OP)
                .executesPlayer((player, args) -> {
                    PhysxMc.instance().getGrabTool().forceClear();
                    PhysxMc.instance().getDisplayedBoxHolder().destroyAll();
                });

        CommandAPICommand debugCommand = new CommandAPICommand("debug")
                .withPermission(CommandPermission.OP)
                .executesPlayer((player, args) -> {
                    PhysxSetting.setDebugMode(!PhysxSetting.isDebugMode());
                    player.sendMessage("デバッグモードを" + (PhysxSetting.isDebugMode() ? "有効" : "無効") + "にしました");
                });

        CommandAPICommand densityCommand = new CommandAPICommand("density")
                .withPermission(CommandPermission.OP)
                .withArguments(new FloatArgument("密度"))
                .executesPlayer((player, args) -> {
                    float density = ((float) args.get(0));
                    PhysxSetting.setDefaultDensity(density);
                    player.sendMessage(String.format("既定の密度を%fに設定しました", density));
                });

        CommandAPICommand updateChunkCommand = new CommandAPICommand("updatechunk")
                .withPermission(CommandPermission.OP)
                .executesPlayer((player, args) -> {
                    PhysxMc.instance().getPhysxWorld().registerChunksToReloadNextSecond(player.getChunk());
                    player.sendMessage("プレイヤーが今いるチャンクをアップデートしました");
                });

        CommandAPICommand summonTestObjectCommand = new CommandAPICommand("summontestobject")
                .withPermission(CommandPermission.OP)
                .withArguments(new FloatArgument("x"))
                .withArguments(new FloatArgument("y"))
                .withArguments(new FloatArgument("z"))
                .executesPlayer((player, args) -> {
                    float x = ((float) args.get(0));
                    float y = ((float) args.get(1));
                    float z = ((float) args.get(2));

                    PhysxMc.instance().getDisplayedBoxHolder().createDisplayedBox(player.getLocation(), new Vector(x, y, z), new ItemStack(Material.COMMAND_BLOCK), List.of(new Vector()));
                    player.sendMessage("テストブロックを生成しました");
                });

        CommandAPICommand gravityCommand = new CommandAPICommand("gravity")
                .withPermission(CommandPermission.OP)
                .withArguments(new FloatArgument("x"))
                .withArguments(new FloatArgument("y"))
                .withArguments(new FloatArgument("z"))
                .executesPlayer((player, args) -> {
                    float x = ((float) args.get(0));
                    float y = ((float) args.get(1));
                    float z = ((float) args.get(2));

                    PhysxMc.instance().getPhysxWorld().setGravity(new Vector(x, y, z));
                    player.sendMessage("重力を変更しました");
                });
        
        mainCommand.withSubcommands(resetCommand, debugCommand, densityCommand, updateChunkCommand, summonTestObjectCommand, gravityCommand);
        mainCommand.register();
    }
}
