package tfar.entitydamagecaps;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.DamageTypeTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.concurrent.CompletableFuture;

@Mod(EntityDamageCaps.MOD_ID)
public class EntityDamageCapsForge {

    public static final TagKey<DamageType> BYPASS = TagKey.create(Registries.DAMAGE_TYPE,new ResourceLocation(EntityDamageCaps.MOD_ID,"bypass"));

    public EntityDamageCapsForge() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, DamageCapsConfig.Server.SPEC);

        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(DamageCapsConfig::onLoad);
        bus.addListener(DamageCapsConfig::onReload);
        bus.addListener(this::gather);
        // This method is invoked by the Forge mod loader when it is ready
        // to load your mod. You can access Forge and Common code in this
        // project.
    
        // Use Forge to bootstrap the Common mod.
        EntityDamageCaps.init();
        MinecraftForge.EVENT_BUS.addListener(this::onDamage);
    }

    void gather(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        generator.addProvider(true,new EDCDamageTypeTagsProvider(packOutput,lookupProvider,existingFileHelper));
    }

    public static class EDCDamageTypeTagsProvider extends DamageTypeTagsProvider {
        public EDCDamageTypeTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                                         ExistingFileHelper existingFileHelper) {
            super(output, lookupProvider, EntityDamageCaps.MOD_ID, existingFileHelper);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            tag(BYPASS).addTag(DamageTypeTags.BYPASSES_INVULNERABILITY);
        }
    }

    void onDamage(LivingDamageEvent event) {
        LivingEntity livingEntity = event.getEntity();
        boolean isTame = livingEntity instanceof TamableAnimal tamableAnimal && tamableAnimal.isTame();
        if (!event.getSource().is(BYPASS) && !(isTame && DamageCapsConfig.Server.IGNORE_TAMED.get())) {
            Float f = DamageCapsConfig.CACHED_CAPS.get(livingEntity.getType());
            if (f != null) {
                float cappedDamage = Math.min(event.getAmount(), f * livingEntity.getMaxHealth());
                event.setAmount(cappedDamage);
            }
        }
    }
}