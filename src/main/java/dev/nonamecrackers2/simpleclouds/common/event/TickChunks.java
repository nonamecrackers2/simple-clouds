package dev.nonamecrackers2.simpleclouds.common.event;

import dev.nonamecrackers2.simpleclouds.common.world.CloudManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TickChunks
{
    //private static final Logger LOGGER = LogManager.getLogger();

    public static void rainAndSnowVanillaCompatibility(ServerLevel level, ChunkAccess chunk)
    {
        CloudManager<?> manager = CloudManager.get(level);

        // Random check to execute every 16 ticks
        if (level.random.nextInt(16) == 0)
        {
            // Get Chunk Corner
            int blockX = chunk.getPos().getMinBlockX();
            int blockZ = chunk.getPos().getMinBlockZ();

            // Get random heightmap position in the chunk
            BlockPos checkPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, level.getBlockRandomPos(blockX, 0, blockZ, 15));

            // Check if it is raining or snowing at the center of the chunk
            boolean isRaining = manager.isRainingAt(checkPos);
            boolean isSnowing = manager.isSnowingAt(checkPos);

            // Position below the checkPos to get biome and handle precipitation
            BlockPos belowPos = checkPos.below();
            Biome biome = level.getBiome(belowPos).value(); // Get biome at the position below
            Biome.Precipitation biomePrecipitation = biome.getPrecipitationAt(belowPos); // Check precipitation type

            // Handle precipitation for cauldrons
            if (biomePrecipitation != Biome.Precipitation.NONE)
            {
                BlockState blockState = level.getBlockState(belowPos);
                blockState.getBlock().handlePrecipitation(blockState, level, belowPos, biomePrecipitation);
            }

            // Check for snow accumulation
            if (level.random.nextInt(16) == 0)
            {
                int snowAccumulationHeight = level.getGameRules().getInt(GameRules.RULE_SNOW_ACCUMULATION_HEIGHT);
                if (snowAccumulationHeight > 0 && biome.shouldSnow(level, checkPos))
                {
                    BlockState blockStateAtCheckPos = level.getBlockState(checkPos);
                    if (blockStateAtCheckPos.is(Blocks.SNOW))
                    {
                        int layers = blockStateAtCheckPos.getValue(SnowLayerBlock.LAYERS);
                        if (layers < Math.min(snowAccumulationHeight, 8))
                        {
                            BlockState updatedBlockState = blockStateAtCheckPos.setValue(SnowLayerBlock.LAYERS, layers + 1);
                            Block.pushEntitiesUp(blockStateAtCheckPos, updatedBlockState, level, checkPos);
                            level.setBlockAndUpdate(checkPos, updatedBlockState);
                        }
                    }
                    else
                    {
                        level.setBlockAndUpdate(checkPos, Blocks.SNOW.defaultBlockState());
                    }
                }
            }
        }
    }
}
