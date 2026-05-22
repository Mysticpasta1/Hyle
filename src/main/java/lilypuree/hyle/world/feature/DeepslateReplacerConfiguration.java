package lilypuree.hyle.world.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lilypuree.hyle.Constants;
import lilypuree.hyle.misc.HyleDataLoaders;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class DeepslateReplacerConfiguration implements FeatureConfiguration {

    public static final Codec<DeepslateReplacerConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.listOf().fieldOf("regions").forGetter(x -> x.regions),
            Frequencies.CODEC.fieldOf("frequencies").forGetter(x -> x.frequencies)
    ).apply(instance, DeepslateReplacerConfiguration::new));

    public DeepslateReplacerConfiguration(List<ResourceLocation> regions, Frequencies frequencies) {
        this.regions = regions;
        this.frequencies = frequencies;
    }

    private final List<ResourceLocation> regions;
    private final Frequencies frequencies;

    public record Frequencies(float region, float unconformity, float primary, float secondary, float tertiary) {
        public static Codec<Frequencies> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.fieldOf("region").forGetter(Frequencies::region),
                Codec.FLOAT.fieldOf("unconformity").forGetter(Frequencies::unconformity),
                Codec.FLOAT.fieldOf("primary").forGetter(Frequencies::primary),
                Codec.FLOAT.fieldOf("secondary").forGetter(Frequencies::secondary),
                Codec.FLOAT.fieldOf("tertiary").forGetter(Frequencies::tertiary)
        ).apply(instance, Frequencies::new));
    }

    public Frequencies getFrequencies() {
        return frequencies;
    }

    private Set<ResourceLocation> missingRegions = new HashSet<>();

    public StoneRegion getRegions(float regionNoise) {
        if (Constants.CONFIG.enableDebug()) return DebugRegion.DEBUG_REGION;
        if (regions.isEmpty()) return DebugRegion.EMPTY_REGION;
        ResourceLocation regionID = regions.get((int) Mth.clampedMap(regionNoise, -1, 1, 0, regions.size()));
        Optional<StoneRegion> region = HyleDataLoaders.getRegion(regionID);
        if (region.isPresent()) {
            return region.get();
        } else {
            if (!missingRegions.contains(regionID)) {
                Constants.LOG.error("Region " + regionID + " doesn't exist!");
                missingRegions.add(regionID);
            }
            return DebugRegion.EMPTY_REGION;
        }
    }
}
