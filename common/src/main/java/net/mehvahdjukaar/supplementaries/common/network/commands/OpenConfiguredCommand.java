package net.mehvahdjukaar.supplementaries.common.network.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.mehvahdjukaar.supplementaries.common.network.NetworkHandler;
import net.mehvahdjukaar.supplementaries.common.network.OpenConfigsPacket;
import net.mehvahdjukaar.supplementaries.configs.ServerConfigs;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class OpenConfiguredCommand implements Command<CommandSourceStack> {
    private static final OpenConfiguredCommand CMD = new OpenConfiguredCommand();

    public static ArgumentBuilder<CommandSourceStack, ?> register(CommandDispatcher<CommandSourceStack> dispatcher) {
        return Commands.literal("configs")
                .requires(cs -> cs.hasPermission(0))
                .executes(CMD);
    }

    @Override
    public int run(CommandContext<CommandSourceStack> context) {
        if (ServerConfigs.SERVER_SPEC.hasConfigScreen()) {
            if (context.getSource().getEntity() instanceof ServerPlayer serverPlayer) {
                NetworkHandler.CHANNEL.sendToClientPlayer(serverPlayer, new OpenConfigsPacket());
            }
        } else {
            context.getSource().sendSuccess(Component.translatable("message.supplementaries.command.configs"), false);
        }
        return 0;
    }
}
