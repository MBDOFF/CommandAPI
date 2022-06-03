package dev.jorel.commandapi;

import java.util.ArrayList;
import java.util.List;

import dev.jorel.commandapi.arguments.ArgumentBase;

/**
 * A list of arguments which results in an execution. This is used for building branches in a {@link CommandTreeBase}
 */
record Execution<ImplementedSender, ArgumentImpl extends ArgumentBase<?, ImplementedSender, ArgumentImpl>> (
		List<ArgumentBase<?, ImplementedSender, ArgumentImpl>> arguments,
		CustomCommandExecutor<ImplementedSender> executor) {

	/**
	 * Register a command with the given arguments and executor to brigadier, by converting it into a {@link CommandAPICommandBase}
	 * @param meta The metadata to register the command with
	 */
	public void register(CommandMetaData<ImplementedSender> meta) {
		@SuppressWarnings("unchecked")
		CommandAPICommandBase<?, ImplementedSender, ArgumentImpl> command = CommandAPIHandler.getInstance().createCommandBase(meta).withArguments(arguments);
		command.setExecutor(executor);
		command.register();
	}

	public Execution<ImplementedSender, ArgumentImpl> prependedBy(ArgumentBase<?, ImplementedSender, ArgumentImpl> argument) {
		List<ArgumentBase<?, ImplementedSender, ArgumentImpl>> arguments = new ArrayList<>();
		arguments.add(argument);
		arguments.addAll(arguments());
		return new Execution<ImplementedSender, ArgumentImpl>(arguments, executor);
	}

}
