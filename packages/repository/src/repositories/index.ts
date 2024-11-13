import type { IRepository, LyricRepositoryConfig, SongRepositoryConfig } from '@overture-stack/maestro-common';

import { isLyricConfiguration, isSongConfiguration } from '../utils/utils';
import { lyricRepository } from './lyric/repository';
import { songRepository } from './song/repository';

export const repository = (config: SongRepositoryConfig | LyricRepositoryConfig): IRepository => {
	if (isLyricConfiguration(config)) {
		return lyricRepository(config);
	} else if (isSongConfiguration(config)) {
		return songRepository(config);
	}
	throw Error('Invalid repository configuration');
};
