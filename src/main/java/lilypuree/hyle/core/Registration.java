package lilypuree.hyle.core;

import lilypuree.hyle.world.feature.StoneReplacer;
import lilypuree.hyle.world.feature.StoneReplacerConfiguration;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.RegistryObject;

public class Registration {
    public static final DeferredRegister FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES, "hyle");
    public static final RegistryObject<Feature<StoneReplacerConfiguration>> STONE_REPLACER = FEATURES.register("stone_replacer", () -> new StoneReplacer());

    public static void init(IEventBus bus) {
        FEATURES.register(bus);
    }
}
