package com.example.noslime;

import com.mojang.logging.LogUtils;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * 给 1.21.1 新增一条游戏规则：doSlimeSpawning（默认 true）。
 *
 * <p>注册必须发生在任何 {@link GameRules} 实例被创建之前，所以放在静态字段里，
 * 由 FML 加载模组主类时完成注册。之后创建的每个世界都会自动带上这条规则，
 * 并且会像原版规则一样随 level.dat 保存、出现在「编辑游戏规则」界面和 /gamerule 命令里。</p>
 */
@Mod(NoSlimeMod.MOD_ID)
public final class NoSlimeMod {

    public static final String MOD_ID = "noslime";
    public static final String RULE_NAME = "doSlimeSpawning";

    private static final Logger LOGGER = LogUtils.getLogger();

    /** 设为 false 时，不再允许新的史莱姆进入世界。 */
    public static final GameRules.Key<GameRules.BooleanValue> DO_SLIME_SPAWNING =
            GameRules.register(RULE_NAME, GameRules.Category.SPAWNING,
                    GameRules.BooleanValue.create(true));

    public NoSlimeMod(IEventBus modBus) {
        LOGGER.info("[NoSlime] gamerule {} registered (default: true)", RULE_NAME);
    }

    /**
     * 读取规则值。极端时序下（某个世界的 GameRules 实例早于本规则注册就被创建），
     * 回退为 true，保持原版行为，避免空指针导致崩溃。
     */
    public static boolean isSlimeSpawningAllowed(GameRules rules) {
        if (rules == null) {
            return true;
        }
        try {
            return rules.getBoolean(DO_SLIME_SPAWNING);
        } catch (RuntimeException ignored) {
            return true;
        }
    }
}
