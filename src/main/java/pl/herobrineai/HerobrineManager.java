package pl.herobrineai;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.Vec3;
import net.fabricmc.fabric.api.entity.FabricEntityTypeBuilder;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

public final class HerobrineManager {
    private static volatile boolean enabled = true;
    private final Map<UUID, PlayerMemory> memory = new ConcurrentHashMap<>();
    private final Map<UUID, Long> nextDecision = new ConcurrentHashMap<>();
    private final Map<UUID, NightmareSession> nightmares = new ConcurrentHashMap<>();
    private final HerobrineAiClient ai = new HerobrineAiClient();

    public static void setEnabled(boolean value) { enabled = value; }

    public void tick(MinecraftServer server) {
        if (!enabled) return;
        long tick = server.getTickCount();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            tickPlayer(server, player, tick);
        }
    }

    private void tickPlayer(MinecraftServer server, ServerPlayer player, long tick) {
        PlayerMemory m = memory.computeIfAbsent(player.getUUID(), id -> new PlayerMemory());

        if (m.lastActionTick >= 0 && tick - m.lastActionTick == 100) {
            double distance = player.position().distanceTo(new Vec3(m.lastX, m.lastY, m.lastZ));
            m.recordReaction(distance > 7.0);
        }

        if (player.isSleeping() && !nightmares.containsKey(player.getUUID())) {
            if (tick % 20 == 0) startNightmare(player);
        }

        for (Entity e : player.level().getEntities(player, player.getBoundingBox().inflate(48), e -> e instanceof HerobrineEntity)) {
            double dot = e.position().subtract(player.position()).normalize().dot(player.getLookAngle());
            if (dot > 0.985) m.lookedAtHerobrine++;
        }

        Long next = nextDecision.get(player.getUUID());
        if (next != null && tick < next) return;
        nextDecision.put(player.getUUID(), tick + 160 + player.getRandom().nextInt(240));

        String context = context(player, m);
        if (ai.enabled()) {
            askAi(server, player, m, context);
        } else {
            heuristic(server, player, m);
        }

        finishNightmare(server, player);
    }

    public void onChat(ServerPlayer player, String message) {
        PlayerMemory m = memory.computeIfAbsent(player.getUUID(), id -> new PlayerMemory());
        m.chatCount++;
        m.lastMessage = message.length() > 180 ? message.substring(0, 180) : message;
        if (!enabled) return;

        String lower = message.toLowerCase(Locale.ROOT);
        if (!lower.contains("herobrine") && !lower.contains("herobrine'a") && m.lastActionTick < 0) return;
        String context = context(player, m) + "\nPLAYER_MESSAGE=" + m.lastMessage;
        if (ai.enabled()) {
            ai.ask(chatInstructions(), context).thenAccept(reply -> player.getServer().execute(() -> {
                if (!reply.isBlank()) sendHerobrineChat(player, clean(reply));
            }));
        } else {
            sendHerobrineChat(player, heuristicReply(message, m));
        }
    }

    public void forceChat(ServerPlayer player, String message) {
        PlayerMemory m = memory.computeIfAbsent(player.getUUID(), id -> new PlayerMemory());
        m.lastMessage = message;
        if (ai.enabled()) {
            ai.ask(chatInstructions(), context(player, m) + "\nPLAYER_MESSAGE=" + message)
                .thenAccept(reply -> player.getServer().execute(() -> sendHerobrineChat(player, clean(reply))));
        } else {
            sendHerobrineChat(player, heuristicReply(message, m));
        }
    }

    private void askAi(MinecraftServer server, ServerPlayer player, PlayerMemory m, String context) {
        ai.ask(planningInstructions(), context).thenAccept(answer -> server.execute(() -> {
            if (answer == null || answer.isBlank()) return;
            Decision d = Decision.parse(answer);
            executeDecision(player, m, d);
        })).exceptionally(error -> {
            HerobrineAiMod.LOGGER.warn("Herobrine AI request failed", error);
            return null;
        });
    }

    private void heuristic(MinecraftServer server, ServerPlayer player, PlayerMemory m) {
        int roll = player.getRandom().nextInt(6);
        if (m.reactions > m.ignoredEvents + 2 && roll <= 2) roll = 1;
        switch (roll) {
            case 0 -> spawnNear(player, false);
            case 1 -> placeSign(player, heuristicSign(m));
            case 2 -> buildStructure(player);
            case 3 -> possessAnimal(player);
            case 4 -> sendHerobrineChat(player, heuristicReply("", m));
            default -> {}
        }
    }

    private void executeDecision(ServerPlayer player, PlayerMemory m, Decision d) {
        switch (d.action()) {
            case "APPEAR" -> spawnNear(player, false);
            case "SIGN" -> placeSign(player, d.message().isBlank() ? heuristicSign(m) : d.message());
            case "BUILD" -> buildStructure(player);
            case "POSSESS" -> possessAnimal(player);
            case "NIGHTMARE" -> startNightmare(player);
            case "CHAT" -> sendHerobrineChat(player, d.message().isBlank() ? heuristicReply("", m) : d.message());
            default -> {}
        }
    }

    public void spawnNear(ServerPlayer player, boolean dramatic) {
        ServerLevel level = player.serverLevel();
        Vec3 backwards = player.getLookAngle().normalize().scale(-(14 + player.getRandom().nextInt(12)));
        Vec3 pos = player.position().add(backwards);
        HerobrineEntity h = new HerobrineEntity(ModEntities.HEROBRINE, level);
        h.moveTo(pos.x, pos.y, pos.z, player.getYRot() + 180, 0);
        h.setCustomName(Component.literal("Herobrine").withStyle(Style.EMPTY.withItalic(false)));
        h.setCustomNameVisible(false);
        level.addFreshEntity(h);
        memory.computeIfAbsent(player.getUUID(), id -> new PlayerMemory())
            .recordAction("APPEAR", level.getGameTime(), player.getX(), player.getY(), player.getZ());
        if (dramatic) player.displayClientMessage(Component.literal("Coś stoi za tobą.").withStyle(s -> s.withColor(0x8B0000)), true);
    }

    private void placeSign(ServerPlayer player, String raw) {
        ServerLevel level = player.serverLevel();
        BlockPos base = findNearbySolid(level, player.blockPosition(), player.getRandom());
        if (base == null) return;
        BlockPos signPos = base.above();
        level.setBlockAndUpdate(signPos, Blocks.OAK_SIGN.defaultBlockState());
        if (level.getBlockEntity(signPos) instanceof SignBlockEntity sign) {
            String[] lines = wrap(raw, 4, 15);
            net.minecraft.network.chat.Component[] texts = new net.minecraft.network.chat.Component[]{
                line(lines, 0), line(lines, 1), line(lines, 2), line(lines, 3)
            };
            SignText signText = new SignText(
                texts,
                new net.minecraft.network.chat.Component[]{texts[0], texts[1], texts[2], texts[3]},
                DyeColor.BLACK,
                true
            );
            sign.setText(signText, true);
            sign.setChanged();
            PlayerMemory m = memory.computeIfAbsent(player.getUUID(), id -> new PlayerMemory());
            m.signsPlaced++;
            m.recordAction("SIGN", level.getGameTime(), player.getX(), player.getY(), player.getZ());
        }
    }

    private void buildStructure(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos c = player.blockPosition().relative(oppositeLook(player), 10);
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                set(level, c.offset(x, 0, z), Blocks.POLISHED_BLACKSTONE);
            }
        }
        for (int y = 1; y <= 3; y++) {
            set(level, c.offset(-2, y, -2), Blocks.POLISHED_BLACKSTONE_BRICKS);
            set(level, c.offset(2, y, -2), Blocks.POLISHED_BLACKSTONE_BRICKS);
            set(level, c.offset(-2, y, 2), Blocks.POLISHED_BLACKSTONE_BRICKS);
            set(level, c.offset(2, y, 2), Blocks.POLISHED_BLACKSTONE_BRICKS);
        }
        set(level, c.above(), Blocks.SOUL_SAND);
        set(level, c.above(2), Blocks.CRYING_OBSIDIAN);
        set(level, c.offset(0, 2, 0), Blocks.REDSTONE_WIRE);
        placeSignAt(level, c.offset(0, 0, -3), new String[]{"NIE", "ŚPISZ", "SAM", ""});
        PlayerMemory m = memory.computeIfAbsent(player.getUUID(), id -> new PlayerMemory());
        m.recordAction("BUILD", level.getGameTime(), player.getX(), player.getY(), player.getZ());
    }

    private void possessAnimal(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Animal target = level.getEntitiesOfClass(Animal.class, player.getBoundingBox().inflate(12), Entity::isAlive)
            .stream().findFirst().orElse(null);
        if (target == null) return;
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 220, 0, false, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 160, 2, false, false, true));
        target.setCustomName(Component.literal("?").withStyle(s -> s.withColor(0x111111)));
        target.setCustomNameVisible(false);
        PlayerMemory m = memory.computeIfAbsent(player.getUUID(), id -> new PlayerMemory());
        m.recordAction("POSSESS", level.getGameTime(), player.getX(), player.getY(), player.getZ());
    }

    public void startNightmare(ServerPlayer player) {
        if (nightmares.containsKey(player.getUUID())) return;
        ServerLevel level = player.serverLevel();
        BlockPos origin = player.blockPosition();
        BlockPos dream = origin.offset(1600 + player.getRandom().nextInt(250), 0, 1600 + player.getRandom().nextInt(250));
        buildNightmareArena(level, dream, player.getName().getString());
        nightmares.put(player.getUUID(), new NightmareSession(level, origin, dream, level.getGameTime() + 260));
        player.teleportTo(level, dream.getX() + 0.5, dream.getY() + 1, dream.getZ() + 0.5, player.getYRot(), player.getXRot());
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0, false, false, true));
        player.displayClientMessage(Component.literal("KOSZMAR").withStyle(s -> s.withColor(0x111111).withBold(true)), true);
    }

    private void finishNightmare(MinecraftServer server, ServerPlayer player) {
        NightmareSession session = nightmares.get(player.getUUID());
        if (session == null) return;
        if (session.deadline > player.serverLevel().getGameTime()) return;
        player.teleportTo(session.level, session.original.getX() + 0.5, session.original.getY(), session.original.getZ() + 0.5, player.getYRot(), player.getXRot());
        player.removeEffect(MobEffects.DARKNESS);
        nightmares.remove(player.getUUID());
        sendHerobrineChat(player, "Obudziłeś się. Szkoda, że coś z tobą wróciło.");
    }

    private void buildNightmareArena(ServerLevel level, BlockPos c, String playerName) {
        for (int x = -6; x <= 6; x++) {
            for (int z = -6; z <= 6; z++) {
                set(level, c.offset(x, 0, z), Blocks.DEEPSLATE);
                for (int y = 1; y <= 4; y++) set(level, c.offset(x, y, z), Blocks.AIR);
            }
        }
        for (int x = -6; x <= 6; x++) {
            set(level, c.offset(x, 5, -6), Blocks.BLACK_CONCRETE);
            set(level, c.offset(x, 5, 6), Blocks.BLACK_CONCRETE);
        }
        for (int z = -6; z <= 6; z++) {
            set(level, c.offset(-6, 5, z), Blocks.BLACK_CONCRETE);
            set(level, c.offset(6, 5, z), Blocks.BLACK_CONCRETE);
        }
        for (int i = -3; i <= 3; i++) {
            set(level, c.offset(i, 1, 0), Blocks.COBWEB);
        }
        placeSignAt(level, c.offset(0, 1, -4), new String[]{"WIDZĘ", trim(playerName), "OD", "DAWNA"});
        placeSignAt(level, c.offset(4, 1, 0), new String[]{"NIE", "BUDŹ", "MNIE", ""});
    }

    public void buildMap(ServerPlayer player) {
        buildStructure(player);
        BlockPos c = player.blockPosition().relative(oppositeLook(player), 10);
        buildNightmareArena(player.serverLevel(), c.offset(20, 0, 0), player.getName().getString());
        sendHerobrineChat(player, "Pierwsza scena jest gotowa. Reszta należy do mnie.");
    }

    public String status(ServerPlayer player) {
        PlayerMemory m = memory.computeIfAbsent(player.getUUID(), id -> new PlayerMemory());
        return String.format(Locale.ROOT,
            "Herobrine AI: %s | API: %s | fear=%.2f | encounters=%d | reactions=%d | ignored=%d | signs=%d | looked=%d | last=%s",
            enabled ? "ON" : "OFF", ai.enabled() ? "ON" : "OFF", m.fear, m.encounters, m.reactions,
            m.ignoredEvents, m.signsPlaced, m.lookedAtHerobrine, m.lastAction);
    }

    public void reset(ServerPlayer player) {
        memory.computeIfAbsent(player.getUUID(), id -> new PlayerMemory()).reset();
        nextDecision.remove(player.getUUID());
    }

    private String context(ServerPlayer player, PlayerMemory m) {
        return "PLAYER=" + player.getName().getString()
            + "\nTIME=" + player.serverLevel().getGameTime()
            + "\nX=" + round(player.getX()) + " Y=" + round(player.getY()) + " Z=" + round(player.getZ())
            + "\nHEALTH=" + round(player.getHealth())
            + "\nSLEEPING=" + player.isSleeping()
            + "\nENCOUNTERS=" + m.encounters
            + "\nREACTIONS=" + m.reactions
            + "\nIGNORED=" + m.ignoredEvents
            + "\nFEAR=" + round(m.fear)
            + "\nSIGNS=" + m.signsPlaced
            + "\nLOOKED_AT_HEROBRINE=" + m.lookedAtHerobrine
            + "\nLAST_ACTION=" + m.lastAction
            + "\nLAST_MESSAGE=" + m.lastMessage;
    }

    private String planningInstructions() {
        return "You are Herobrine, an adaptive supernatural antagonist inside Minecraft. "
            + "Do not explain your reasoning. Choose ONE action that best unsettles the player. "
            + "Use only this JSON format: {\"action\":\"APPEAR|SIGN|BUILD|POSSESS|NIGHTMARE|CHAT|NONE\",\"message\":\"short text\"}. "
            + "Never invent commands or coordinates. Never ask the mod to damage files, run OS commands, or access anything outside the game. "
            + "Favor psychological continuity and adaptation over random jumpscares. Most actions should be subtle.";
    }

    private String chatInstructions() {
        return "You are Herobrine speaking to a Minecraft player. Be eerie, restrained, personal and cryptic. "
            + "Reply in Polish, one or two short sentences, without quotes, emoji, or explanations. "
            + "Use the supplied memory to reference previous behavior, but do not claim impossible access to the player's device.";
    }

    private String heuristicReply(String message, PlayerMemory m) {
        if (message == null || message.isBlank()) return m.ignoredEvents > m.reactions ? "Widziałem, jak mnie ignorujesz." : "Nie odwracaj się jeszcze.";
        String lower = message.toLowerCase(Locale.ROOT);
        if (lower.contains("gdzie")) return "Bliżej, niż myślisz.";
        if (lower.contains("czego") || lower.contains("czemu")) return "Bo za szybko uznałeś, że jesteś sam.";
        if (lower.contains("boję") || lower.contains("strach")) return "Dobrze. Teraz słuchasz.";
        return m.lookedAtHerobrine > 0 ? "W końcu spojrzałeś." : "Jeszcze mnie nie znalazłeś.";
    }

    private String heuristicSign(PlayerMemory m) {
        if (m.ignoredEvents > m.reactions) return "WIEM, ŻE\nMNIE\nIGNORUJESZ";
        if (m.lookedAtHerobrine > 0) return "ZA KAŻDYM\nRAZEM\nPATRZYSZ";
        return "NIE\nJESTEŚ\nSAM";
    }

    private void sendHerobrineChat(ServerPlayer player, String message) {
        if (message == null || message.isBlank()) return;
        player.sendSystemMessage(Component.literal("Herobrine: ").withStyle(s -> s.withColor(0x555555))
            .append(Component.literal(message).withStyle(s -> s.withColor(0xAAAAAA))));
    }

    private BlockPos findNearbySolid(ServerLevel level, BlockPos origin, Random random) {
        for (int i = 0; i < 20; i++) {
            BlockPos p = origin.offset(random.nextInt(11) - 5, 0, random.nextInt(11) - 5);
            if (level.getBlockState(p).isSolidRender(level, p) && level.getBlockState(p.above()).canBeReplaced()) return p;
        }
        return origin;
    }

    private void placeSignAt(ServerLevel level, BlockPos pos, String[] lines) {
        level.setBlockAndUpdate(pos, Blocks.OAK_SIGN.defaultBlockState());
        if (level.getBlockEntity(pos) instanceof SignBlockEntity sign) {
            net.minecraft.network.chat.Component[] texts = new net.minecraft.network.chat.Component[]{
                Component.literal(trim(lines, 0)), Component.literal(trim(lines, 1)), Component.literal(trim(lines, 2)), Component.literal(trim(lines, 3))
            };
            sign.setText(new SignText(texts, texts, DyeColor.BLACK, true), true);
            sign.setChanged();
        }
    }

    private static Component line(String[] lines, int index) {
        return Component.literal(index < lines.length ? lines[index] : "");
    }

    private static String[] wrap(String text, int count, int width) {
        String normalized = text.replace("\n", " ").trim();
        String[] out = new String[count];
        Arrays.fill(out, "");
        String[] words = normalized.split("\\s+");
        int line = 0;
        for (String word : words) {
            if (line >= count) break;
            String candidate = out[line].isBlank() ? word : out[line] + " " + word;
            if (candidate.length() <= width) out[line] = candidate;
            else line++;
            if (line < count && out[line].isBlank()) out[line] = word.length() > width ? word.substring(0, width) : word;
        }
        return out;
    }

    private static String trim(String s) {
        if (s == null) return "";
        return s.length() <= 15 ? s : s.substring(0, 15);
    }

    private static String trim(String[] a, int i) { return i < a.length ? trim(a[i]) : ""; }
    private static double round(double value) { return Math.round(value * 100.0) / 100.0; }
    private static void set(ServerLevel level, BlockPos pos, Block block) { level.setBlockAndUpdate(pos, block.defaultBlockState()); }

    private Direction oppositeLook(ServerPlayer player) {
        return player.getDirection().getOpposite();
    }

    private static String clean(String raw) {
        String s = raw.trim();
        if (s.startsWith("```") && s.endsWith("```")) {
            s = s.substring(3, s.length() - 3).trim();
            if (s.startsWith("json")) s = s.substring(4).trim();
        }
        return s;
    }

    private record Decision(String action, String message) {
        static Decision parse(String raw) {
            try {
                String s = clean(raw);
                int a = s.indexOf('{');
                int b = s.lastIndexOf('}');
                if (a >= 0 && b > a) s = s.substring(a, b + 1);
                JsonObject o = JsonParser.parseString(s).getAsJsonObject();
                return new Decision(o.has("action") ? o.get("action").getAsString().toUpperCase(Locale.ROOT) : "NONE",
                    o.has("message") ? o.get("message").getAsString() : "");
            } catch (Exception e) {
                return new Decision("NONE", "");
            }
        }
    }

    private record NightmareSession(ServerLevel level, BlockPos original, BlockPos dream, long deadline) {}
}
