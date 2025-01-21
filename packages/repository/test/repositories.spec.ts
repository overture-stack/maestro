import { expect } from 'chai';
import { describe, it } from 'mocha';

import { type LyricRepositoryConfig, RepositoryType, type SongRepositoryConfig } from '@overture-stack/maestro-common';

import { repository } from '../src/repositories';

describe('repositories', () => {
	it('should call lyricRepository when type is LYRIC', () => {
		const config: LyricRepositoryConfig = {
			type: RepositoryType.LYRIC,
			name: 'Lyric Repository',
			categoryId: 123,
			code: 'lyricRepo1',
			baseUrl: 'http://localhost',
			validDataOnly: true,
			indexName: 'index_1_1',
		};
		const result = repository(config);

		expect(result).to.have.property('getRepositoryRecords').that.is.a('function');
		expect(result).to.have.property('getOrganizationRecords').that.is.a('function');
		expect(result).to.have.property('getRecord').that.is.a('function');
	});

	it('should call songRepository when type is SONG', () => {
		const config: SongRepositoryConfig = {
			type: RepositoryType.SONG,
			name: 'Song Repository',
			code: 'songRepo2',
			baseUrl: 'http://localhost',
			indexName: 'index_1_2',
			analysisCentricEnabled: true,
			indexableStudyStates: 'PUBLISHED',
		};
		const result = repository(config);

		expect(result).to.have.property('getRepositoryRecords').that.is.a('function');
		expect(result).to.have.property('getOrganizationRecords').that.is.a('function');
		expect(result).to.have.property('getRecord').that.is.a('function');
	});
});
