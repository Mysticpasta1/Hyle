package lilypuree.hyle;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Constants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EventHandler {
    @SubscribeEvent
    public static void onServerStarting(ServerAboutToStartEvent event) {
        if (Constants.CONFIG.disableReplacement()) return;

        MinecraftServer server = event.getServer();
        BiomeInjector.apply(server.registryAccess());
    }
}
