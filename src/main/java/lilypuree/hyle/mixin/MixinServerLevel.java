package lilypuree.hyle.mixin;

import lilypuree.hyle.BiomeInjector;
import lilypuree.hyle.Constants;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.ServerLevelData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.concurrent.Executor;

@Mixin(ServerLevel.class)
public abstract class MixinServerLevel {

    @Shadow @Final private MinecraftServer server;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(MinecraftServer server, Executor pDispatcher, LevelStorageSource.LevelStorageAccess pLevelStorageAccess, ServerLevelData pServerLevelData, ResourceKey key, LevelStem pLevelStem, ChunkProgressListener pProgressListener, boolean pIsDebug, long pSeed, List pCustomSpawners, boolean pTickTime, CallbackInfo ci) {
        if (key.equals(Level.OVERWORLD) && !Constants.CONFIG.disableReplacement()) {
            Constants.LOG.log(org.apache.logging.log4j.Level.INFO, "Hyle biomesource cache modification started");
            long start = System.currentTimeMillis();

            ChunkGenerator generator = pLevelStem.generator();

            if (generator.biomeSource instanceof FixedBiomeSource) {
                BiomeInjector.apply(server.registryAccess());
            }

            BiomeInjector.apply(generator.getBiomeSource());
            if (generator.biomeSource != generator.getBiomeSource()) {
                BiomeInjector.apply(generator.biomeSource);
            }
            long timeTook = System.currentTimeMillis() - start;
            Constants.LOG.log(org.apache.logging.log4j.Level.INFO, "Hyle biomesource cache modification took {} ms to complete.", timeTook);
        }
    }
}
