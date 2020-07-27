package dev.jorel.commandapi.arguments;

import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.bukkit.command.CommandSender;

/**
 * An interface declaring methods required to override argument suggestions
 */
public interface ISafeOverrideableSuggestions<S> {

	/**
	 * Override the suggestions of this argument with a custom array. Typically,
	 * this is the supplier <code>s -> suggestions</code>.
	 * 
	 * @param suggestions the string array to override suggestions with
	 * @return the current argument
	 */
	@SuppressWarnings("unchecked")
	Argument safeOverrideSuggestions(S... suggestions);

	/**
	 * Override the suggestions of this argument with a function that maps the
	 * command sender to a custom array.
	 * 
	 * @param suggestions the function to override suggestions with
	 * @return the current argument
	 */
	Argument safeOverrideSuggestions(Function<CommandSender, S[]> suggestions);
	
	/**
	 * Override the suggestions of this argument with a function that maps the
	 * command sender to a custom array.
	 * 
	 * @param suggestions the function to override suggestions with
	 * @return the current argument
	 */
	Argument safeOverrideSuggestions(BiFunction<CommandSender, Object[], S[]> suggestions);
	
	default String[] stringMap(S[] arr, Function<S, String> mapper) {
		return Arrays.stream(arr).map(mapper).toArray(String[]::new);
	}
	
	@SuppressWarnings("unchecked")
	default BiFunction<CommandSender, Object[], String[]> sMap0(Function<S, String> mapper, S... suggestions) {
		return (c, m) -> stringMap(suggestions, mapper);
	}
	
	default BiFunction<CommandSender, Object[], String[]> sMap1(Function<S, String> mapper, Function<CommandSender, S[]> suggestions) {
		return (c, m) -> stringMap(suggestions.apply(c), mapper);
	}
	
	default BiFunction<CommandSender, Object[], String[]> sMap2(Function<S, String> mapper, BiFunction<CommandSender, Object[], S[]> suggestions) {
		return (c, m) -> stringMap(suggestions.apply(c, m), mapper);

	}
	
}
