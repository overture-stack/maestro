import {
	type LyricRepositoryConfig,
	type Repository,
	RepositoryType,
	type SongRepositoryConfig,
} from '@overture-stack/maestro-common';

import { lyricRepository } from './lyric/repository';
import { songRepository } from './song/repository';

export const repository = (config: LyricRepositoryConfig | SongRepositoryConfig): Repository => {
	switch (config.type) {
		case RepositoryType.LYRIC:
			return lyricRepository(config);

		case RepositoryType.SONG:
			return songRepository(config);

		default:
			throw new Error('Invalid repository configuration');
	}
};
