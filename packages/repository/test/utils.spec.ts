import { expect } from 'chai';
import { describe, it } from 'mocha';

import { type LyricRepositoryConfig, RepositoryType, type SongRepositoryConfig } from '@overture-stack/maestro-common';

import { getRepoInformation, isArrayOfObjects } from '../src/utils/utils.js';

describe('Utils', () => {
	describe('Checks if value is an array of objects', () => {
		it('should return true for an array of valid objects', () => {
			const input = [
				{ key1: 'value1', key2: 42, key3: true },
				{ key1: null, key2: { nestedKey: 'nestedValue' }, key3: false },
			];
			const result = isArrayOfObjects(input);
			expect(result).to.eql(true);
		});

		it('should return false for an array containing non-object items', () => {
			const input = [{ key1: 'value1' }, 'string', 123];
			const result = isArrayOfObjects(input);
			expect(result).to.eql(false);
		});

		it('should return false for a non-array input', () => {
			const input = { key1: 'value1' };
			const result = isArrayOfObjects(input);
			expect(result).to.eql(false);
		});

		it('should return false for an array with objects containing invalid property values', () => {
			const input = [{ key1: undefined }, { key2: () => {} }];
			const result = isArrayOfObjects(input);
			expect(result).to.eql(false);
		});

		it('should return true for an array of objects with nested objects', () => {
			const input = [
				{ key1: 'value1', nested: { subKey: 123 } },
				{ key2: null, nested: { anotherKey: true } },
			];
			expect(isArrayOfObjects(input)).to.eql(true);
		});

		it('should return true for an empty array', () => {
			const input: unknown[] = [];
			const result = isArrayOfObjects(input);
			expect(result).to.eql(true);
		});
	});

	describe('getRepoInformation', () => {
		const mockRepositories: (SongRepositoryConfig | LyricRepositoryConfig)[] = [
			{
				code: 'songRepo1',
				name: 'Song Repository 1',
				type: RepositoryType.SONG,
				baseUrl: 'localhost',
				indexName: 'index_1_1',
				indexAlias: 'index_1_1',
				country: 'CA',
				organization: 'oicr',
				analysisCentricEnabled: false,
				indexableStudyStates: 'PUBLISHED',
			},
			{
				code: 'lyricRepo1',
				name: 'Lyric Repository 1',
				type: RepositoryType.LYRIC,
				baseUrl: 'localhost',
				categoryId: 1,
				validDataOnly: true,
				indexName: 'index_1_1',
				indexAlias: 'index_1_1',
			},
		];

		it('should return the correct repository based on repoCode', () => {
			const result = getRepoInformation(mockRepositories, 'songRepo1');
			expect(result).to.deep.equal(mockRepositories[0]);
		});

		it('should return undefined if no repository matches the repoCode', () => {
			const result = getRepoInformation(mockRepositories, 'nonExistentRepo');
			expect(result).to.eql(undefined);
		});

		it('should return the first matching repository if multiple repositories share the same code', () => {
			const mockCommonRepositories: (SongRepositoryConfig | LyricRepositoryConfig)[] = [
				{
					code: 'commonRepo',
					name: 'Song Repository 1',
					type: RepositoryType.SONG,
					baseUrl: 'localhost',
					indexName: 'index_1_1',
					indexAlias: 'index_1_1',
					country: 'CA',
					organization: 'oicr',
					analysisCentricEnabled: false,
					indexableStudyStates: 'PUBLISHED',
				},
				{
					code: 'commonRepo',
					name: 'Lyric Repository 1',
					type: RepositoryType.LYRIC,
					baseUrl: 'localhost',
					categoryId: 1,
					validDataOnly: true,
					indexName: 'index_1_1',
					indexAlias: 'index_1_1',
				},
			];
			const result = getRepoInformation(mockCommonRepositories, 'commonRepo');
			expect(result).to.deep.equal(mockCommonRepositories[0]);
		});

		it('should return undefined when the repository list is empty', () => {
			const result = getRepoInformation([], 'songRepo1');
			expect(result).to.eql(undefined);
		});
	});
});
