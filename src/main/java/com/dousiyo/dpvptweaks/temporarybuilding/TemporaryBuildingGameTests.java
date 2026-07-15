package com.dousiyo.dpvptweaks.temporarybuilding;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;
import com.mojang.authlib.GameProfile;

@GameTestHolder(DpvpTweaks.MODID)
@PrefixGameTestTemplate(false)
public final class TemporaryBuildingGameTests {
    private TemporaryBuildingGameTests() {}

    @GameTest(template = "inteldraftgametests.empty", timeoutTicks = 100)
    public static void adventurePlacementAndRootCollapseUseVanillaFlow(GameTestHelper helper) {
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "temporary-building-test"));
        player.getAbilities().mayBuild = false;
        BlockPos support = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.STONE);

        var server = helper.getLevel().getServer();
        var context = new TemporaryBuildingMatchContext(UUID.randomUUID(), "game_test",
                helper.getLevel().dimension(), support.getX() - 8, support.getX() + 8,
                support.getZ() - 8, support.getZ() + 8);
        if (!TemporaryBuildingManager.beginMatch(server, context, ignored -> true))
            helper.fail("Temporary building manager did not start");

        ItemStack blocks = new ItemStack(ModTemporaryBlocks.WOOD.get(), 20);
        player.getInventory().setItem(TemporaryBuildingLoadout.WOOD_SLOT, blocks);
        player.getInventory().selected = TemporaryBuildingLoadout.WOOD_SLOT;
        useOn(player, blocks, support, Direction.UP);
        BlockPos root = support.above();
        if (!helper.getLevel().getBlockState(root).is(ModTemporaryBlocks.WOOD.get()))
            helper.fail("Adventure mode did not use vanilla BlockItem placement");

        useOn(player, blocks, root, Direction.UP);
        BlockPos child = root.above();
        if (!helper.getLevel().getBlockState(child).is(ModTemporaryBlocks.WOOD.get()))
            helper.fail("Supported temporary block was not placed");

        ItemStack tool = TemporaryBuildingLoadout.baseToolForTest();
        player.getInventory().setItem(TemporaryBuildingLoadout.TOOL_SLOT, tool);
        player.getInventory().selected = TemporaryBuildingLoadout.TOOL_SLOT;
        if (!tool.hasAdventureModeBreakTagForBlock(helper.getLevel().registryAccess().registryOrThrow(Registries.BLOCK),
                new BlockInWorld(helper.getLevel(), root, false)))
            helper.fail("Temporary pickaxe is missing its vanilla CanDestroy rule");
        helper.getLevel().setBlock(root, Blocks.AIR.defaultBlockState(), 3);
        TemporaryBuildingManager.recordRemoval(helper.getLevel(), root);
        helper.runAfterDelay(5, () -> {
            if (!helper.getLevel().getBlockState(child).isAir()) helper.fail("Unsupported child did not collapse");
            TemporaryBuildingManager.endMatch(server);
            helper.succeed();
        });
    }

    private static void useOn(ServerPlayer player, ItemStack stack, BlockPos clicked, Direction face) {
        stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(clicked), face, clicked, false)));
    }
}
