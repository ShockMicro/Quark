package vazkii.quark.addons.oddities.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.PushReaction;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer.Builder;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import vazkii.arl.util.RegistryHelper;
import vazkii.quark.addons.oddities.magnetsystem.MagnetSystem;
import vazkii.quark.addons.oddities.module.MagnetsModule;
import vazkii.quark.addons.oddities.tile.MagnetTileEntity;
import vazkii.quark.addons.oddities.tile.MagnetizedBlockTileEntity;
import vazkii.quark.base.block.QuarkBlock;
import vazkii.quark.base.module.QuarkModule;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class MagnetBlock extends QuarkBlock {

	public static final DirectionProperty FACING = BlockStateProperties.FACING;
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

	public MagnetBlock(QuarkModule module) {
		super("magnet", module, ItemGroup.REDSTONE, Properties.from(Blocks.IRON_BLOCK));
		setDefaultState(getDefaultState().with(FACING, Direction.DOWN).with(POWERED, false));
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void addInformation(@Nonnull ItemStack stack, @Nullable IBlockReader worldIn, @Nonnull List<ITextComponent> tooltip, @Nonnull ITooltipFlag flagIn) {
		if (stack.getDisplayName().getUnformattedComponentText().equals("Q"))
			tooltip.add(new StringTextComponent("haha yes"));
	}

	@Override
	protected void fillStateContainer(Builder<Block, BlockState> builder) {
		builder.add(FACING, POWERED);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void neighborChanged(@Nonnull BlockState state, @Nonnull World worldIn, @Nonnull BlockPos pos, @Nonnull Block blockIn, @Nonnull BlockPos fromPos, boolean isMoving) {
		super.neighborChanged(state, worldIn, pos, blockIn, fromPos, isMoving);
		
		boolean wasPowered = state.get(POWERED);
		boolean isPowered = isPowered(worldIn, pos, state.get(FACING));
		if(isPowered != wasPowered)
			worldIn.setBlockState(pos, state.with(POWERED, isPowered));
	}

	@Override
	public boolean eventReceived(BlockState state, World world, BlockPos pos, int action, int data) {
		boolean push = action == 0;
		Direction moveDir = state.get(FACING);
		Direction dir = push ? moveDir : moveDir.getOpposite();

		BlockPos targetPos = pos.offset(dir, data);
		BlockState targetState = world.getBlockState(targetPos);

		TileEntity tile = world.getTileEntity(pos);
		if (!(tile instanceof MagnetTileEntity))
			return false;

		BlockPos endPos = targetPos.offset(moveDir);
		PushReaction reaction = MagnetSystem.getPushAction((MagnetTileEntity) tile, targetPos, targetState, moveDir);
		if (reaction != PushReaction.IGNORE && reaction != PushReaction.DESTROY)
			return false;

		TileEntity tilePresent = world.getTileEntity(targetPos);
		CompoundNBT tileData = new CompoundNBT();
		if (tilePresent != null && !(tilePresent instanceof MagnetizedBlockTileEntity))
			tilePresent.write(tileData);

		MagnetizedBlockTileEntity movingTile = new MagnetizedBlockTileEntity(targetState, tileData, moveDir);

		if (!world.isRemote && reaction == PushReaction.DESTROY) {
			BlockState blockstate = world.getBlockState(endPos);
			Block.spawnDrops(blockstate, world, endPos, tilePresent);
		}

		if (tilePresent != null)
			tilePresent.remove();

		world.setBlockState(endPos, MagnetsModule.magnetized_block.getDefaultState()
				.with(MovingMagnetizedBlock.FACING, moveDir), 68);
		world.setTileEntity(endPos, movingTile);

		world.setBlockState(targetPos, Blocks.AIR.getDefaultState(), 66);

		return true;
	}

	private boolean isPowered(World worldIn, BlockPos pos, Direction facing) {
		Direction opp = facing.getOpposite();
		for(Direction direction : Direction.values())
			if(direction != facing && direction != opp && worldIn.isSidePowered(pos.offset(direction), direction))
				return true;

		return false;
	}

	@Override
	public BlockState getStateForPlacement(BlockItemUseContext context) {
		Direction facing = context.getNearestLookingDirection().getOpposite();
		return getDefaultState().with(FACING, facing)
				.with(POWERED, isPowered(context.getWorld(), context.getPos(), facing));
	}

	@Nonnull
	@Override
	public BlockState rotate(@Nonnull BlockState state, Rotation rot) {
		return state.with(FACING, rot.rotate(state.get(FACING)));
	}

	@Nonnull
	@Override
	public BlockState mirror(@Nonnull BlockState state, Mirror mirrorIn) {
		return state.rotate(mirrorIn.toRotation(state.get(FACING)));
	}

	@Override
	public boolean hasTileEntity(BlockState state) {
		return true;
	}

	@Override
	public TileEntity createTileEntity(BlockState state, IBlockReader world) {
		return new MagnetTileEntity();
	}

}
