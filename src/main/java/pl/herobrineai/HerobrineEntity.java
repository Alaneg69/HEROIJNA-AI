package pl.herobrineai;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class HerobrineEntity extends PathfinderMob {
    public HerobrineEntity(EntityType<? extends HerobrineEntity> entityType, Level level) {
        super(entityType, level);
        setNoCulling(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 40.0)
            .add(Attributes.MOVEMENT_SPEED, 0.24)
            .add(Attributes.FOLLOW_RANGE, 48.0)
            .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(3, new RandomStrollGoal(this, 0.8));
        goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 32.0f));
        goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && random.nextInt(800) == 0) {
            setDeltaMovement(getDeltaMovement().scale(0.1));
        }
    }
}
