package lilypuree.hyle.world.feature.gen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public class DeepslateType extends StoneType {
    private final BlockState deepslateBlock;

    public DeepslateType(BlockState baseBlock, Optional<BlockState> deepslateBlock, Optional<BlockState> cobbleBlock, Optional<BlockState> dirtReplace, Optional<BlockState> grassReplace, Map<ResourceLocation, BlockState> oreMap, boolean ignoreBiome) {
        super(baseBlock, cobbleBlock, dirtReplace, grassReplace, oreMap, ignoreBiome);
        this.deepslateBlock = deepslateBlock.orElse(null);
    }

    public static Codec<DeepslateType> DIRECT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BLOCK_OR_BLOCKSTATE_CODEC.fieldOf("base").forGetter(s -> s.getBaseBlock()),
            BLOCK_OR_BLOCKSTATE_CODEC.optionalFieldOf("deepslate").forGetter(s -> Optional.ofNullable(s.deepslateBlock)),
            BLOCK_OR_BLOCKSTATE_CODEC.optionalFieldOf("cobble").forGetter(s -> Optional.ofNullable(s.getCobbleBlock())),
            BLOCK_OR_BLOCKSTATE_CODEC.optionalFieldOf("dirt").forGetter(s -> Optional.ofNullable(s.getDirtReplace())),
            BLOCK_OR_BLOCKSTATE_CODEC.optionalFieldOf("grass").forGetter(s -> Optional.ofNullable(s.getGrassReplace())),
            Codec.unboundedMap(ResourceLocation.CODEC, BLOCK_OR_BLOCKSTATE_CODEC).optionalFieldOf("ores", Collections.emptyMap()).forGetter(s -> {
                Map<ResourceLocation, BlockState> newMap = new java.util.HashMap<>();
                s.getOreMap().forEach((block, state) -> newMap.put(ForgeRegistries.BLOCKS.getKey(block), state));
                return newMap;
            }),
            Codec.BOOL.optionalFieldOf("ignore_biome", false).forGetter(s -> s.ignoresBiome())
    ).apply(instance, DeepslateType::new));

    @Override
    public BlockState replace(BlockState original) {
        if ((original.is(Blocks.DEEPSLATE) || original.is(Blocks.TUFF)) && deepslateBlock != null) {
            return deepslateBlock;
        }
        return super.replace(original);
    }
}
