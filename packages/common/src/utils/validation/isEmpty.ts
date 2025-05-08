/**
 * Checks if a given value is considered empty.
 * @param value The value to check. This can be of any type.
 * @returns `true` if the value is `null`, `undefined`, an empty object, an empty array or an empty string; otherwise, `false`.
 */
const isEmpty = (value: unknown) => {
	if (value == null) {
		// null or undefined
		return true;
	} else if (typeof value === 'object') {
		return Object.keys(value).length === 0;
	} else if (typeof value === 'string') {
		return value.length === 0;
	}
	return false;
};

export default isEmpty;
