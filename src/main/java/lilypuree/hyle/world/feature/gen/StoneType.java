package lilypuree.hyle.world.feature.gen;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lilypuree.hyle.Constants;
import lilypuree.hyle.compat.IStoneType;
import lilypuree.hyle.core.HyleTags;
import lilypuree.hyle.misc.HyleDataLoaders;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class StoneType implements IStoneType {
    public static final StoneType EMPTY = new StoneType(null);
    public static final StoneType NO_REPLACE = new StoneType(null) {
        @Override
        public BlockState replace(BlockState original) {
            return original;
        }
    };

    private static final Codec<BlockState> BLOCK_OR_BLOCKSTATE_CODEC = Codec.either(ResourceLocation.CODEC.comapFlatMap(loc ->
                    Optional.ofNullable(ForgeRegistries.BLOCKS.getValue(loc)).map(DataResult::success).orElse(DataResult.error(() -> String.format("block %s doesn't exist!", loc))), ForgeRegistries.BLOCKS::getKey), BlockState.CODEC)
            .xmap(either -> either.map(Block::defaultBlockState, b -> b), Either::right);
    public static final Codec<StoneType> CODEC = Codec.STRING.comapFlatMap(string -> {
        StoneType stoneType = HyleDataLoaders.getStoneType(string);
        if (stoneType == null) return DataResult.error(() -> "Stone type " + string + " doesn't exist!", StoneType.NO_REPLACE);
        else return DataResult.success(stoneType);
    }, HyleDataLoaders::getNameForStoneType);

    public static Codec<StoneType> DIRECT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BLOCK_OR_BLOCKSTATE_CODEC.fieldOf("base").forGetter(state -> state.baseBlock),
            BLOCK_OR_BLOCKSTATE_CODEC.optionalFieldOf("cobble").forGetter(state -> Optional.ofNullable(state.cobbleBlock)),
            BLOCK_OR_BLOCKSTATE_CODEC.optionalFieldOf("dirt").forGetter(state -> Optional.ofNullable(state.dirtReplace)),
            BLOCK_OR_BLOCKSTATE_CODEC.optionalFieldOf("grass").forGetter(state -> Optional.ofNullable(state.grassReplace)),
            Codec.unboundedMap(ResourceLocation.CODEC, BLOCK_OR_BLOCKSTATE_CODEC).optionalFieldOf("ores", Collections.emptyMap()).forGetter(StoneType::getOreMapInner),
            Codec.BOOL.optionalFieldOf("ignore_biome", false).forGetter(state -> state.ignoreBiome)
    ).apply(instance, StoneType::new));

    public StoneType(BlockState baseBlock, Optional<BlockState> cobbleBlock, Optional<BlockState> dirtReplace, Optional<BlockState> grassReplace, Map<ResourceLocation, BlockState> oreMap, boolean ignoreBiome) {
        this.baseBlock = baseBlock;
        this.cobbleBlock = Constants.CONFIG.disableCobbleReplacement() ? null : cobbleBlock.orElse(null);
        this.dirtReplace = Constants.CONFIG.disableDirtReplacement() ? null : dirtReplace.orElse(null);
        this.grassReplace = Constants.CONFIG.disableDirtReplacement() ? null : grassReplace.orElse(null);
        this.oreMap = new HashMap<>();
        oreMap.forEach((key, blockState) -> {
            Block block = ForgeRegistries.BLOCKS.getValue(key);
            this.oreMap.put(block, blockState);
        });
        this.ignoreBiome = ignoreBiome;
    }

    public StoneType(BlockState baseBlock) {
        this.baseBlock = baseBlock;
        this.cobbleBlock = baseBlock;
        this.dirtReplace = baseBlock;
        this.grassReplace = baseBlock;
        this.oreMap = Collections.emptyMap();
        this.ignoreBiome = false;
    }

    private final BlockState baseBlock;
    private BlockState cobbleBlock;
    private BlockState dirtReplace;
    private BlockState grassReplace;
    private Map<Block, BlockState> oreMap;
    private boolean ignoreBiome;

    public boolean ignoresBiome() {
        return ignoreBiome;
    }

    private BlockState getGrassReplace(BlockState original) {
        if (grassReplace.hasProperty(BlockStateProperties.SNOWY)) {
            return grassReplace.setValue(BlockStateProperties.SNOWY, original.getValue(BlockStateProperties.SNOWY));
        } else {
            return grassReplace;
        }
    }

    private Map<ResourceLocation, BlockState> getOreMapInner() {
        Map<ResourceLocation, BlockState> newMap = new HashMap<>();
        oreMap.forEach((block, state) -> {
            newMap.put(ForgeRegistries.BLOCKS.getKey(block), state);
        });
        return newMap;
    }

    public void toImmutable() {
        if (Constants.CONFIG.disableOreReplacement()) {
            this.oreMap = Collections.emptyMap();
        } else {
            this.oreMap = ImmutableMap.<Block, BlockState>builder().putAll(oreMap).build();
        }
    }

    @Override
    public BlockState getBaseBlock() {
        return baseBlock;
    }

    @Override
    public BlockState getCobbleBlock() {
        return cobbleBlock;
    }

    @Override
    public void setCobbleBlock(BlockState cobbleBlock) {
        this.cobbleBlock = cobbleBlock;
    }

    @Override
    public BlockState getDirtReplace() {
        return dirtReplace;
    }

    @Override
    public void setDirtReplace(BlockState dirtReplace) {
        this.dirtReplace = dirtReplace;
    }

    @Override
    public BlockState getGrassReplace() {
        return grassReplace;
    }

    @Override
    public void setGrassReplace(BlockState grassReplace) {
        this.grassReplace = grassReplace;
    }

    @Override
    public Map<Block, BlockState> getOreMap() {
        return oreMap;
    }

    public BlockState replace(BlockState original) {
        if (original.is(HyleTags.Blocks.REPLACABLE)) {
            return baseBlock;
        } else if (original.is(Blocks.COBBLESTONE) && cobbleBlock != null) {
            return cobbleBlock;
        } else if (original.is(HyleTags.Blocks.REPLACE_DIRT) && dirtReplace != null) {
            return dirtReplace;
        } else if (original.is(HyleTags.Blocks.REPLACE_GRASS) && grassReplace != null) {
            return getGrassReplace(original);
        }
        for (Block block : oreMap.keySet()) {
            if (original.is(block)) {
                return oreMap.get(block);
            }
        }
        return original;
    }
}
