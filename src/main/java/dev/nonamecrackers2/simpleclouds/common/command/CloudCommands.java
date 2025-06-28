package dev.nonamecrackers2.simpleclouds.common.command;

import java.util.function.Predicate;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import dev.nonamecrackers2.simpleclouds.api.SimpleCloudsAPI;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudTypeSource;
import dev.nonamecrackers2.simpleclouds.common.command.argument.CloudTypeArgument;
import dev.nonamecrackers2.simpleclouds.common.world.CloudManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.commands.arguments.coordinates.Vec2Argument;

//TODO: Docs, including with API
public class CloudCommands
{
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher, String baseName, Predicate<CommandSourceStack> requirement, CloudCommandSource<?, ?> source, CloudTypeSource cloudTypeSource)
	{
		LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(SimpleCloudsMod.MODID);
		
		root.then(Commands.literal(baseName).requires(requirement)
				.then(Commands.literal("clear")
						.then(Commands.literal("all")
								.executes(ctx -> source.clearClouds(ctx, CloudCommandSource.ALL))
						)
						.then(Commands.literal("storms")
								.executes(ctx -> source.clearClouds(ctx, CloudCommandSource.storms(cloudTypeSource)))
						)
				)
		);
		
		root.then(Commands.literal(baseName).requires(requirement)
				.then(Commands.literal("spawn")
						.then(Commands.argument("type", CloudTypeArgument.type(cloudTypeSource))
								.then(Commands.argument("position", Vec2Argument.vec2())
										.then(Commands.argument("radius", FloatArgumentType.floatArg(0.0F))
												.then(Commands.argument("stretchFactor", FloatArgumentType.floatArg(0.01F))
														.then(Commands.argument("rotation", FloatArgumentType.floatArg())
																.then(Commands.argument("lifeTime", TimeArgument.time(0))
																		.then(Commands.argument("growTime", TimeArgument.time(0))
																				.then(Commands.argument("direction", Vec2Argument.vec2(false))
																						.then(Commands.argument("maxSpeed", FloatArgumentType.floatArg(0.0F))
																								.then(Commands.argument("accelerationFactor", FloatArgumentType.floatArg(0.0F))
																										.executes(source::spawnCloud)
																								)
																						)
																				)
																		)
																)
														)
												)
										)
								)
								.then(Commands.literal("extreme")
										.executes(ctx -> source.spawnModifiedCloud(ctx, CloudCommandSource.EXTREME_CLOUD_INFO))
								)
								.then(Commands.literal("temperate")
										.executes(ctx -> source.spawnModifiedCloud(ctx, CloudCommandSource.TEMPERATE_CLOUD_INFO))
								)
								.then(Commands.literal("random")
										.executes(ctx -> source.spawnModifiedCloud(ctx, i -> i))
								)
						)
						.then(Commands.literal("random")
								.executes(source::spawnRandomCloud)
						)
				)
		);
		
		root.then(Commands.literal(baseName).requires(requirement)
				.then(Commands.literal("get")
						.then(Commands.literal("at")
								.then(Commands.argument("position", Vec2Argument.vec2())
										.executes(source::getCloudTypeAt)
								)
						)
						.then(Commands.literal("count")
								.then(Commands.argument("position", Vec2Argument.vec2())
										.then(Commands.argument("radius", IntegerArgumentType.integer(0))
												.executes(ctx -> source.getCloudTypeCount(ctx, true, true))
										)
										.executes(ctx -> source.getCloudTypeCount(ctx, true, false))
								)
								.executes(ctx -> source.getCloudTypeCount(ctx, false, false))
						)
				)
		);
		
		if (!SimpleCloudsAPI.getApi().getHooks().isExternalWeatherControlEnabled())
		{
			root.then(Commands.literal(baseName).requires(requirement)
					.then(Commands.literal("refresh")
							.executes(source::refreshClouds)
							)
					);
		}
		
		root.then(Commands.literal(baseName).requires(requirement)
				.then(Commands.literal("speed")
						.then(Commands.literal("get")
								.executes(source::getSpeed)
						)
						.then(Commands.literal("set")
								.then(Commands.argument("amount", FloatArgumentType.floatArg(0.0F))
										.executes(source::setSpeed)
								)
						)
				)
		);
		
		root.then(Commands.literal(baseName).requires(requirement)
				.then(Commands.literal("seed")
						.then(Commands.literal("get")
								.executes(source::getSeed)
						)
				)
		);
		
		root.then(Commands.literal(baseName).requires(requirement)
				.then(Commands.literal("height")
						.then(Commands.literal("get")
								.executes(source::getCloudHeight)
						)
						.then(Commands.literal("set")
								.then(Commands.argument("height", IntegerArgumentType.integer(CloudManager.CLOUD_HEIGHT_MIN, CloudManager.CLOUD_HEIGHT_MAX))
										.executes(source::setCloudHeight)
								)
						)
				)
		);
		
		dispatcher.register(root);
	}
}
