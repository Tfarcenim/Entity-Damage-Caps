package tfar.entitydamagecaps;

import it.unimi.dsi.fastutil.objects.Object2FloatArrayMap;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class DamageCapsConfig {
    public static class Server {
        public static final ForgeConfigSpec SPEC;

        public static final ForgeConfigSpec.BooleanValue IGNORE_TAMED;

        public static final ForgeConfigSpec.ConfigValue<List<? extends String>> DAMAGE_CAPS;


        static {
            ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

            builder.push("general");
            IGNORE_TAMED = builder
                    .comment("Ignore damage cap when attacking tamed animals")
                    .define("ignore_tamed", true);

            DAMAGE_CAPS = builder
                    .comment("Damage caps for mobs, format as entity_id|fraction")
                    .defineList("damage_caps", List.of("minecraft:wither|.2"),DamageCapsConfig::isValidEntry);
            builder.pop();

            SPEC = builder.build();
        }
    }

    static Object2FloatMap<EntityType<?>> CACHED_CAPS = new Object2FloatArrayMap<>();

    static void onLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() ==  Server.SPEC) {
            cache();
        }
    }

    static void onReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() ==  Server.SPEC) {
            cache();
        }
    }

    static void cache() {
        CACHED_CAPS.clear();
        for (String s : Server.DAMAGE_CAPS.get()) {
            var v = parse(s);
            CACHED_CAPS.put(v.getKey(),(float)v.getValue());
        }
    }

    static Pair<EntityType<?>,Float> parse(Object o) {
        String s = (String) o;
        String[] strings = s.split("\\|");
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(new ResourceLocation(strings[0]));
        float f = Float.parseFloat(strings[1]);
        return Pair.of(type,f);
    }

    static boolean isValidEntry(Object o) {
        try {
            parse(o);
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}