/**
 * Function to replace unwanted characters and standarize key names for fields in data indexer
 * @param keyName
 * @returns
 */
export const sanitizeKeyName = (keyName: string): string => {
	return keyName.replace(/[\s()/]/g, '_');
};
