package pl.herobrineai.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import pl.herobrineai.HerobrineEntity;

public final class HerobrineRenderer extends MobRenderer<HerobrineEntity, HerobrineRenderState, HerobrineModel> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        "herobrine-ai", "textures/entity/herobrine.png");

    public HerobrineRenderer(EntityRendererProvider.Context context) {
        super(context, new HerobrineModel(context.bakeLayer(HerobrineModel.LAYER)), 0.4f);
    }

    @Override
    public HerobrineRenderState createRenderState() {
        return new HerobrineRenderState();
    }

    @Override
    public ResourceLocation getTextureLocation(HerobrineRenderState state) {
        return TEXTURE;
    }
}
