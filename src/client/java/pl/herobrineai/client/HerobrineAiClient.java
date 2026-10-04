package pl.herobrineai.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import pl.herobrineai.ModEntities;

public final class HerobrineAiClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityModelLayerRegistry.registerModelLayer(HerobrineModel.LAYER, HerobrineModel::createBodyLayer);
        EntityRenderers.register(ModEntities.HEROBRINE, HerobrineRenderer::new);
    }
}
