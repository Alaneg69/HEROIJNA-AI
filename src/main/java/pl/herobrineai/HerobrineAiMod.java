package pl.herobrineai;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HerobrineAiMod implements ModInitializer {
    public static final String MOD_ID = "herobrine-ai";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final HerobrineManager MANAGER = new HerobrineManager();

    @Override
    public void onInitialize() {
        ModEntities.register();

        ServerTickEvents.END_SERVER_TICK.register(MANAGER::tick);
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, boundChatType) -> {
            String text = message.decoratedContent().getString();
            MANAGER.onChat(sender, text);
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("herobrine")
                .then(Commands.literal("awaken").executes(ctx -> {
                    HerobrineManager.setEnabled(true);
                    ctx.getSource().sendSuccess(() -> Component.literal("Herobrine obudzony."), false);
                    return 1;
                }))
                .then(Commands.literal("sleep").executes(ctx -> {
                    HerobrineManager.setEnabled(false);
                    ctx.getSource().sendSuccess(() -> Component.literal("Herobrine zasnął."), false);
                    return 1;
                }))
                .then(Commands.literal("summon").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    MANAGER.spawnNear(player, true);
                    return 1;
                }))
                .then(Commands.literal("nightmare").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    MANAGER.startNightmare(player);
                    return 1;
                }))
                .then(Commands.literal("map").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    MANAGER.buildMap(player);
                    return 1;
                }))
                .then(Commands.literal("status").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    ctx.getSource().sendSuccess(() -> Component.literal(MANAGER.status(player)), false);
                    return 1;
                }))
                .then(Commands.literal("reset").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    MANAGER.reset(player);
                    ctx.getSource().sendSuccess(() -> Component.literal("Pamięć Herobrine'a została wyzerowana."), false);
                    return 1;
                }))
                .then(Commands.literal("say")
                    .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            MANAGER.forceChat(player, StringArgumentType.getString(ctx, "message"));
                            return 1;
                        })))
            );
        });

        LOGGER.info("Herobrine AI initialized.");
    }
}
