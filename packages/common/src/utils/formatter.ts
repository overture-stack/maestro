const replace_special_characters = (input: string) => {
	const specialCharactersRegex = /[<" \\/,|>?*]/g;
	return input.replace(specialCharactersRegex, '_');
};

export const sanitize_index_name = (input: string) => {
	return replace_special_characters(input).toLowerCase();
};
