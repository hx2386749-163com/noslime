package com.example.noslime;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * 拦截所有「新史莱姆进入世界」的路径。
 *
 * <p>只在服务端取消：客户端不装本模组时也不会出现「看得见但服务端没有」的幽灵实体，
 * 双端都装时行为一致，因为史莱姆压根不会被加入服务端世界。</p>
 */
@EventBusSubscriber(modid = NoSlimeMod.MOD_ID)
public final class SlimeSpawnBlocker {

    private SlimeSpawnBlocker() {
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof Slime)) {
            return;
        }
        // 从存档里读出来的旧史莱姆保留，只阻止「新生成」的
        if (event.loadedFromDisk()) {
            return;
        }
        Level level = event.getLevel();
        if (!(level instanceof ServerLevel)) {
            return;
        }
        if (!NoSlimeMod.isSlimeSpawningAllowed(level.getGameRules())) {
            event.setCanceled(true);
        }
    }
}
