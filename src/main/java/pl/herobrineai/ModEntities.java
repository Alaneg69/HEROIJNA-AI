package pl.herobrineai;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.Entity;

public final class ModEntities {
    public static final ResourceKey<EntityType<?>> HEROBRINE_KEY = ResourceKey.create(
        Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(HerobrineAiMod.MOD_ID, "herobrine"));

    public static final EntityType<HerobrineEntity> HEROBRINE = register(
        EntityType.Builder.of(HerobrineEntity::new, MobCategory.MONSTER)
            .sized(0.6f, 1.95f)
            .clientTrackingRange(64),
        HEROBRINE_KEY
    );

    private static <T extends Entity> EntityType<T> register(EntityType.Builder<T> builder, ResourceKey<EntityType<?>> key) {
        return BuiltInRegistries.ENTITY_TYPE.register(key, builder.build(key));
    }

    public static void register() {
        FabricDefaultAttributeRegistry.register(HEROBRINE, HerobrineEntity.createAttributes());
    }

    private ModEntities() {}
}
