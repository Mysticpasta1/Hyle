package lilypuree.hyle.world.feature;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lilypuree.hyle.world.feature.gen.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

import java.util.Map;
import java.util.Set;

public class DeepslateReplacer extends Feature<DeepslateReplacerConfiguration> {
    public DeepslateReplacer() {
        super(DeepslateReplacerConfiguration.CODEC);
    }

    private static final Set<Block> DEEPSLATE_BLOCKS = Set.of(Blocks.DEEPSLATE, Blocks.TUFF);

    private boolean isSeedSet = false;

    private boolean canReplace(ChunkGenerator generator) {
        if (generator instanceof NoiseBasedChunkGenerator nbcg) {
            return nbcg.generatorSettings().get().defaultBlock().is(Blocks.STONE);
        }
        return false;
    }

    @Override
    public boolean place(FeaturePlaceContext<DeepslateReplacerConfiguration> ctx) {
        if (!canReplace(ctx.chunkGenerator())) return false;
        WorldGenLevel level = ctx.level();
        DeepslateReplacerConfiguration config = ctx.config();
        if (!isSeedSet) {
            int seed = (int) level.getSeed();
            var frequencies = config.getFrequencies();
            NoiseHolder.setSeed(seed - 193864, frequencies.region());
            PrimaryNoiseSampler.setSeed(seed + 777261, frequencies.primary());
            SecondaryNoiseSampler.setSeed(seed + 390271, frequencies.secondary());
            RoughNoiseSampler.setSeed(seed + 1567241, frequencies.tertiary(), frequencies.unconformity());
            isSeedSet = true;
        }

        BlockPos pos = ctx.origin();
        int minY = level.getMinBuildHeight();
        ChunkAccess chunkAccess = level.getChunk(pos);
        int baseX = pos.getX();
        int baseZ = pos.getZ();
        int maxHeight = Integer.MIN_VALUE;
        int[][] heights = new int[17][17];
        for (int x = 0; x < 17; x++) {
            for (int z = 0; z < 17; z++) {
                int height;
                if (x < 16 && z < 16) {
                    height = chunkAccess.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
                } else {
                    height = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x + baseX, z + baseZ);
                }
                maxHeight = Math.max(height, maxHeight);
                heights[x][z] = height;
            }
        }
        if (maxHeight <= minY) return false;

        NoiseCache noiseCache = new NoiseCache(config, minY, heights, baseX, baseZ);
        ChunkFiller filler = new ChunkFiller(level.getSeed(), new BlockPos(pos.getX(), minY, pos.getZ()), maxHeight - minY + 1);
        filler.fillIn(16, noiseCache::getRoughNoise);
        filler.fillInEdgesAndFaces(8);
        filler.fillInCenters(8, noiseCache::getRoughNoise);
        filler.fillEmpty(8, noiseCache::getStrataNoise);
        filler.fillInEdgesAndFaces(4);
        filler.fillInCenters(4, noiseCache::getCombinedNoise);
        filler.fillInEdgesAndFaces(2);
        filler.fillInCentersFast(2);
        filler.fillInEdgesAndFaces(1);
        filler.fillInCentersRandom(1);
        replaceDeepslate(chunkAccess, minY, heights, filler.results());
        return true;
    }

    private static class NoiseCache {
        private final DeepslateReplacerConfiguration config;
        private final int minY;
        private final int[][] heights;
        private final int baseX;
        private final int baseZ;
        private final Map<Integer, Map<Integer, ChunkFiller.StonetypeGetter>> roughNoiseSamplers = new Int2ObjectOpenHashMap<>(3);
        private final Map<Integer, Map<Integer, ChunkFiller.StonetypeGetter>> strataNoiseSamplers = new Int2ObjectArrayMap<>(5);

        NoiseCache(DeepslateReplacerConfiguration config, int minY, int[][] heights, int baseX, int baseZ) {
            this.config = config;
            this.minY = minY;
            this.heights = heights;
            this.baseX = baseX;
            this.baseZ = baseZ;
        }

        StoneType getRoughNoise(int posX, int posY, int posZ) {
            return roughNoiseSamplers
                    .computeIfAbsent(posX, i -> new Int2ObjectOpenHashMap<>(3))
                    .computeIfAbsent(posZ, i -> createRoughNoiseSampler(posX, posZ))
                    .get(posX, posY, posZ);
        }

        StoneType getStrataNoise(int posX, int posY, int posZ) {
            return strataNoiseSamplers
                    .computeIfAbsent(posX, i -> new Int2ObjectArrayMap<>(5))
                    .computeIfAbsent(posZ, i -> createStrataNoiseSampler(posX, posZ))
                    .get(posX, posY, posZ);
        }

        StoneType getCombinedNoise(int posX, int posY, int posZ) {
            StoneType state = getRoughNoise(posX, posY, posZ);
            return state == StoneType.EMPTY ? getStrataNoise(posX, posY, posZ) : state;
        }

        private ChunkFiller.StonetypeGetter createRoughNoiseSampler(int posX, int posZ) {
            int height = getHeight(posX, posZ);
            StoneRegion region = config.getRegions(NoiseHolder.getRegionNoise(posX, posZ));
            return new RoughNoiseSampler(region, posX, posZ, minY, height);
        }

        private ChunkFiller.StonetypeGetter createStrataNoiseSampler(int posX, int posZ) {
            StoneRegion region = config.getRegions(NoiseHolder.getRegionNoise(posX, posZ));
            return new PrimaryNoiseSampler(region.primary(), posX, posZ, minY);
        }

        private int getHeight(int posX, int posZ) {
            int x = posX - baseX;
            int z = posZ - baseZ;
            if (x >= 0 && x < 17 && z >= 0 && z < 17) {
                return heights[x][z];
            }
            return Integer.MIN_VALUE;
        }
    }

    private void replaceDeepslate(ChunkAccess chunkAccess, int minY, int[][] heights, StoneType[][][] results) {
        LevelChunkSection chunkSection = chunkAccess.getSection(chunkAccess.getSectionIndex(minY));
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int height = heights[x][z];
                for (int posY = minY; posY <= height; posY++) {
                    int sectionIndex = chunkAccess.getSectionIndex(posY);
                    if (chunkAccess.getSectionIndex(minY) != sectionIndex) {
                        chunkSection = chunkAccess.getSection(sectionIndex);
                    }
                    if (chunkSection.hasOnlyAir()) continue;

                    BlockState block = chunkSection.getBlockState(x, posY & 15, z);
                    if (block.isAir() || !DEEPSLATE_BLOCKS.contains(block.getBlock())) continue;
                    StoneType stoneType = results[x][z][posY - minY];
                    BlockState replaced = null;
                    if (stoneType instanceof DeepslateType dt) {
                        replaced = dt.replace(block);
                    } else if (stoneType.getBaseBlock() != null) {
                        replaced = stoneType.getBaseBlock();
                    }
                    if (replaced != null && block != replaced) {
                        chunkSection.setBlockState(x, posY & 15, z, replaced, false);
                    }
                }
            }
        }
    }
}
