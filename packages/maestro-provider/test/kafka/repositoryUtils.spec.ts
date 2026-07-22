import { expect } from 'chai';
import { describe, it } from 'mocha';

import {
	IndexableState,
	IndexingMode,
	type LyricRepositoryConfig,
	type SongRepositoryConfig,
} from '@overture-stack/maestro-common';

import { getConfiguredCategoryIds, getMatchingRepos, getRepoTopics, isDefined } from '../../src/kafka/repositoryUtils';

describe('repository utils', () => {
	const mockRepos: (SongRepositoryConfig | LyricRepositoryConfig)[] = [
		{
			baseUrl: 'song-server',
			code: 'SONG1',
			indexableStudyStates: [IndexableState.PUBLISHED],
			indexingMode: IndexingMode.analysisCentric,
			indexName: 'analysis_centric',
			kafkaTopic: 'song-analysis',
			name: 'Song 1',
			type: 'SONG',
		},
		{
			baseUrl: 'lyric-server',
			categoryId: '1',
			code: 'LYRIC123',
			indexName: 'lyric1',
			kafkaTopic: 'lyric_document',
			name: 'Lyric 123',
			type: 'LYRIC',
			validDataOnly: true,
		},
		{
			baseUrl: 'lyric-server',
			categoryId: '2',
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

	describe('getMatchingRepos', () => {
		const sharedTopic = 'lyric-document-updates';

		const lyricRepo = (categoryId: number | string, indexName = `index-${categoryId}`): LyricRepositoryConfig => ({
			baseUrl: 'lyric-server',
			categoryId,
			code: `code-${categoryId}-${indexName}`,
			indexName,
			kafkaTopic: sharedTopic,
			name: `Lyric ${categoryId}`,
			type: 'LYRIC',
			validDataOnly: true,
		});

		describe('when the topic maps to a single repo', () => {
			it('should return that repo without needing a category identifier on the message', () => {
				const repos = [lyricRepo('3')];

				const result = getMatchingRepos(repos, sharedTopic, {});

				expect(result).to.deep.equal(repos);
			});

			it('should return that repo even when the message carries a categoryId that does not match its configured value', () => {
				// Topic alone is unambiguous here, so category identifiers are informational, not required for a match.
				const repos = [lyricRepo('3')];

				const result = getMatchingRepos(repos, sharedTopic, { categoryId: 999 });

				expect(result).to.deep.equal(repos);
			});
		});

		describe('when the topic maps to more than one repo', () => {
			it('should match the repo configured with the message categoryId when no alias is present', () => {
				const repos = [lyricRepo('3'), lyricRepo('7')];

				const result = getMatchingRepos(repos, sharedTopic, { categoryId: 7 });

				expect(result.map((r) => r.categoryId)).to.deep.equal(['7']);
			});

			it('should match a repo configured with a numeric categoryId, for existing integrations that build the config with a plain number rather than an env-derived string', () => {
				const repos = [lyricRepo(3), lyricRepo(7)];

				const result = getMatchingRepos(repos, sharedTopic, { categoryId: 7 });

				expect(result.map((r) => r.categoryId)).to.deep.equal([7]);
			});

			it('should match the repo configured with the message categoryAlias', () => {
				const repos = [lyricRepo('3'), lyricRepo('donor')];

				const result = getMatchingRepos(repos, sharedTopic, { categoryAlias: 'donor', categoryId: 9 });

				expect(result.map((r) => r.categoryId)).to.deep.equal(['donor']);
			});

			it('should allow a numeric-looking alias to match, since it is compared by equality rather than by shape', () => {
				const repos = [lyricRepo('3'), lyricRepo('2024')];

				const result = getMatchingRepos(repos, sharedTopic, { categoryAlias: '2024', categoryId: 42 });

				expect(result.map((r) => r.categoryId)).to.deep.equal(['2024']);
			});

			it('should fan out to every repo sharing the same configured identifier', () => {
				// Deliberate duplicate configuration: same category indexed into two different mappings.
				const repos = [lyricRepo('donor', 'donor-mapping-a'), lyricRepo('donor', 'donor-mapping-b')];

				const result = getMatchingRepos(repos, sharedTopic, { categoryAlias: 'donor', categoryId: 9 });

				expect(result).to.have.length(2);
			});

			it('should return no matches when the message identifies a category no repo is configured for', () => {
				const repos = [lyricRepo('3'), lyricRepo('7')];

				const result = getMatchingRepos(repos, sharedTopic, { categoryAlias: 'unknown', categoryId: 99 });

				expect(result).to.have.length(0);
			});

			it('should return no matches when the message carries no category identifier at all', () => {
				// Documented, not a graceful fallback: a shared topic with no category identifier
				// can't be routed safely, goes to the DLQ with a warning at the consumer level.
				const repos = [lyricRepo('3'), lyricRepo('7')];

				const result = getMatchingRepos(repos, sharedTopic, {});

				expect(result).to.have.length(0);
			});
		});

		it('should not match repos configured for a different topic', () => {
			// A second same-topic repo avoids the single-candidate bypass, so this genuinely
			// tests topic filtering.
			const repos = [lyricRepo('3'), lyricRepo('7'), { ...lyricRepo('7', 'decoy'), kafkaTopic: 'other-topic' }];

			const result = getMatchingRepos(repos, sharedTopic, { categoryId: 7 });

			expect(result.map((r) => r.indexName)).to.deep.equal(['index-7']);
		});

		it('should return an empty array when no repo is configured for the topic at all', () => {
			const result = getMatchingRepos(mockRepos, 'nonexistent-topic', { categoryId: 1 });

			expect(result).to.deep.equal([]);
		});
	});

	describe('getConfiguredCategoryIds', () => {
		const sharedTopic = 'lyric-document-updates';

		const lyricRepo = (categoryId: number | string, indexName = `index-${categoryId}`): LyricRepositoryConfig => ({
			baseUrl: 'lyric-server',
			categoryId,
			code: `code-${categoryId}-${indexName}`,
			indexName,
			kafkaTopic: sharedTopic,
			name: `Lyric ${categoryId}`,
			type: 'LYRIC',
			validDataOnly: true,
		});

		it('should return the configured categoryId of every Lyric repo on the topic', () => {
			const repos = [lyricRepo('3'), lyricRepo('donor')];

			const result = getConfiguredCategoryIds(repos, sharedTopic);

			expect(result).to.deep.equal(['3', 'donor']);
		});

		it('should exclude repos configured for a different topic', () => {
			const repos = [lyricRepo('3'), { ...lyricRepo('7'), kafkaTopic: 'other-topic' }];

			const result = getConfiguredCategoryIds(repos, sharedTopic);

			expect(result).to.deep.equal(['3']);
		});

		it('should exclude SONG repos, which have no categoryId', () => {
			const repos = [lyricRepo('3'), { ...mockRepos[0], kafkaTopic: sharedTopic }];

			const result = getConfiguredCategoryIds(repos, sharedTopic);

			expect(result).to.deep.equal(['3']);
		});

		it('should return an empty array when no repo is configured for the topic', () => {
			const result = getConfiguredCategoryIds(mockRepos, 'nonexistent-topic');

			expect(result).to.deep.equal([]);
		});
	});
});
