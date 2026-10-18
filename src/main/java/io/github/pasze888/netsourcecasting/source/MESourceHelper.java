package io.github.pasze888.netsourcecasting.source;

import appeng.api.config.Actionable;
import appeng.api.features.GridLinkables;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.StorageHelper;
import appeng.integration.modules.curios.CuriosIntegration;
import appeng.items.tools.powered.WirelessTerminalItem;
import gripe._90.arseng.me.key.SourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * ME 网络这一路来源的直通存取。
 *
 * <p>本类引用 AE2 与 Ars Énergistique（{@link SourceKey} 由后者注册）的类型，因此<b>只在
 * {@link SourceChain} 确认这两个模组都在场之后才会被触碰</b>——只装超越维度的玩家根本不会走到这里。
 * 纯算术的换算见 {@link SourceCost}，也不放在本类。
 *
 * <p>魔源在 ME 网络中以 {@link SourceKey} 存储；网络侧的定量抽取沿用 AE2 的标准写法
 * {@link StorageHelper#poweredExtraction}（同时受网络可用能量与存储量约束）。
 *
 * <p>本模组的语义是「直通网络」：不缓存魔源，每次施法按需现取。
 *
 * <p>终端来源沿用 AE2 的终端定位方式：主背包用 {@code PlayerInventory}，饰品槽用
 * {@link CuriosIntegration#ITEM_HANDLER}——后者是 AE2 的 Curios 整合模块自己注册的能力值
 * （{@code appeng.integration.modules.curios}，MIT），所以「装 Ars Nouveau 就一定装了 Curios」
 * 这个前提不必再判断，能力值取不到就是跳过。
 */
public final class MESourceHelper {

    private MESourceHelper() {
    }

    /**
     * 把玩家身上第一台可用的无线终端所指的网络包成一路来源。
     *
     * <p>调用方必须先确认 AE2 与 Ars Énergistique 都在场——本类一被触碰就会用到它们的类型。
     *
     * @return 可用的来源；找不到时返回 {@code null}
     */
    public static @Nullable SourceProvider resolve(ServerPlayer player) {
        IGrid grid = findGrid(player);
        return grid == null ? null : new MESourceProvider(grid, player);
    }

    /**
     * 判断一个物品是不是「可绑定到 ME 网络的无线终端」。
     *
     * <p>走 AE2 自己的注册表 {@link GridLinkables}，而不是硬编码 {@code instanceof}，
     * 这样 AE2 原生终端以及第三方（含 AE2WTLib）注册的终端都能被认出来。
     */
    public static boolean isLinkableTerminal(ItemStack stack) {
        return !stack.isEmpty() && GridLinkables.get(stack.getItem()) != null;
    }

    /**
     * 找出玩家身上第一台「已绑定且网络可达」的 AE2 无线终端所对应的 ME 网络。
     *
     * <p>这里刻意 <b>不做接入点范围校验</b>：只要终端绑定过接入点、且该接入点方块仍是有效网格节点，
     * 就认为可用（即「绑一次即可用」）。这是本模组的既定设计。
     *
     * <p>失败原因不采集：本模组不发失败提示，判定交还原版报 {@code ars_nouveau.spell.no_mana}，
     * 因此也不把 AE2 的错误解释（{@code getLinkedGrid} 的 errorConsumer）转给玩家。
     *
     * @return 可用的 ME 网络；找不到则返回 {@code null}
     */
    public static IGrid findGrid(ServerPlayer player) {
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            IGrid grid = gridFrom(player, inventory.getItem(slot));
            if (grid != null) {
                return grid;
            }
        }
        // 再看 Curios 饰品槽：这里用的是 AE2 自带无线终端热键（{@code appeng.hotkeys.CuriosHotkeyAction}）
        // 用的同一个能力值 {@link CuriosIntegration#ITEM_HANDLER}，不引入 Curios 自己的 API。
        // 没装 Curios 时该能力值没有提供者，{@code getCapability} 返回 {@code null}，等价于跳过。
        var curios = player.getCapability(CuriosIntegration.ITEM_HANDLER);
        if (curios != null) {
            for (int slot = 0; slot < curios.getSlots(); slot++) {
                IGrid grid = gridFrom(player, curios.getStackInSlot(slot));
                if (grid != null) {
                    return grid;
                }
            }
        }
        return null;
    }

    /** 一件物品若是「已绑定且网络可达」的无线终端，返回它指向的 ME 网络。 */
    private static @Nullable IGrid gridFrom(ServerPlayer player, ItemStack stack) {
        if (!isLinkableTerminal(stack)) {
            return null;
        }
        if (stack.getItem() instanceof WirelessTerminalItem terminal) {
            return terminal.getLinkedGrid(stack, player.level(), null);
        }
        return null;
    }

    /**
     * 向网络魔源存储取指定数量的魔源。
     *
     * <p>定量抽取沿用 AE2 的标准写法 {@link StorageHelper#poweredExtraction}：同时受网络可用能量
     * 与存储量约束，取不满时返回实际取到的量。
     *
     * <p>{@code amount} 一律传「本次真正需要的量」，不要传 {@code Long.MAX_VALUE} 之类的哨兵值：
     * 该方法按 {@code retrieved / energyFactor} 计费，量级不同会让模拟与实扣落在不同的能量判定上，
     * 导致「判定通过但实扣为 0」。
     *
     * @param mode {@link Actionable#SIMULATE} 只试探可取出量（不改变网络内容），
     *             {@link Actionable#MODULATE} 实际取出
     * @return 实际（或可）取出量，不会为负
     */
    public static long extractSource(IGrid grid, ServerPlayer player, long amount, Actionable mode) {
        if (amount <= 0L) {
            return 0L;
        }
        long extracted = StorageHelper.poweredExtraction(
                grid.getEnergyService(),
                grid.getStorageService().getInventory(),
                SourceKey.KEY,
                amount,
                IActionSource.ofPlayer(player),
                mode);
        return Math.max(extracted, 0L);
    }
}
