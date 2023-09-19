package com.kamesuta.physxmc.command;

import com.kamesuta.physxmc.PhysxMc;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;

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
        
        mainCommand.withSubcommand(resetCommand);
        mainCommand.register();
    }
}
