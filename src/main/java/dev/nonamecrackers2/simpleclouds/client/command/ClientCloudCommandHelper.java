package dev.nonamecrackers2.simpleclouds.client.command;

import java.util.Objects;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import dev.nonamecrackers2.simpleclouds.client.world.ClientCloudManager;
import dev.nonamecrackers2.simpleclouds.common.command.CloudCommandSource;
import dev.nonamecrackers2.simpleclouds.common.command.CloudCommands;
import dev.nonamecrackers2.simpleclouds.common.world.CloudManager;
import dev.nonamecrackers2.simpleclouds.common.world.SyncType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class ClientCloudCommandHelper
{
	private static final SimpleCommandExceptionType ERROR_NOT_CLIENT_ONLY = new SimpleCommandExceptionType(Component.translatable("commands.simpleclouds.notClientSideOnly"));
	
	public static final CloudCommandSource<ClientLevel, ClientCloudManager> SOURCE = new CloudCommandSource<>()
	{
		@Override
		public Player getPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
		{
			return Objects.requireNonNull(Minecraft.getInstance().player, "Player is not available");
		}
		
		@Override
		public ClientCloudManager getCloudManager(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
		{
			ClientCloudManager manager = (ClientCloudManager)CloudManager.get(Objects.requireNonNull(Minecraft.getInstance().level, "Client level is not available"));
			if (manager.hasReceivedSync())
				throw ERROR_NOT_CLIENT_ONLY.create();
			return manager;
		}
		
		@Override
		public void onValueUpdated(ClientCloudManager cloudManager, SyncType sync) {}
	};
	
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
	{
		 CloudCommands.register(dispatcher, "clientClouds", src -> true, SOURCE);
	}
}
