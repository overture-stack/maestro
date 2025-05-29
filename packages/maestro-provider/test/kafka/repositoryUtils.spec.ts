import { expect } from 'chai';
import { describe, it } from 'mocha';

import {
	IndexableState,
	IndexingMode,
	type LyricRepositoryConfig,
	type SongRepositoryConfig,
} from '@overture-stack/maestro-common';

import { getRepoByTopic, getRepoTopics, isDefined } from '../../src/kafka/repositoryUtils';

describe('repository utils', () => {
	const mockRepos: (SongRepositoryConfig | LyricRepositoryConfig)[] = [
		{
			indexingMode: IndexingMode.analysisCentric,
			baseUrl: 'song-server',
			code: 'SONG1',
			indexName: 'analysis_centric',
			indexableStudyStates: [IndexableState.PUBLISHED],
			kafkaTopic: 'song-analysis',
			name: 'Song 1',
			type: 'SONG',
		},
		{
			baseUrl: 'lyric-server',
			categoryId: 1,
			code: 'LYRIC123',
			indexName: 'lyric1',
			kafkaTopic: 'lyric_document',
			name: 'Lyric 123',
			type: 'LYRIC',
			validDataOnly: true,
		},
		{
			baseUrl: 'lyric-server',
			categoryId: 2,
			code: 'LYRICXYZ',
			indexName: 'lyric1',
			name: 'Lyric XYZ',
			type: 'LYRIC',
			validDataOnly: true,
		},
	];

	describe('isDefined', () => {
		it('should return true for a defined string', () => {
			expect(isDefined('hello')).to.eql(true);
		});

		it('should return false for undefined', () => {
			expect(isDefined(undefined)).to.eql(false);
		});

		it('should return false for empty string', () => {
			expect(isDefined('')).to.eql(false);
		});
	});

	describe('getRepoTopics', () => {
		it('should return only defined kafkaTopic values', () => {
			const result = getRepoTopics(mockRepos);
			expect(result).to.deep.equal(['song-analysis', 'lyric_document']);
		});
	});

	describe('getRepoByTopic', () => {
		it('should return the correct repository by topic', () => {
			const result = getRepoByTopic(mockRepos, 'lyric_document');
			expect(result).to.include({ code: 'LYRIC123' });
		});

		it('should return undefined if no topic matches', () => {
			const result = getRepoByTopic(mockRepos, 'nonexistent-topic');
			expect(result).to.eql(undefined);
		});
	});
});
