import { logger } from '../../logger/logger.js';
import { IndexingMode, type SongRepositoryConfig } from '../../types/config.js';
import { DataRecordNested } from '../../types/dataRecord.js';
import type { AnalysisCentricDocument, FileCentricDocument, FileEntry } from './types.js';

/**
 * Type guard to validate if a given object conforms to the `FileEntry` structure
 * expected from a Song file document.
 * @param obj A file object
 * @returns true if object contains all the required properties, otherwise returns false
 */
export const isFileEntry = (obj: unknown): obj is FileEntry => {
	return (
		typeof obj === 'object' &&
		obj !== null &&
		'objectId' in obj &&
		'studyId' in obj &&
		'fileName' in obj &&
		'fileSize' in obj &&
		'fileType' in obj &&
		'fileMd5sum' in obj &&
		'fileAccess' in obj &&
		'dataType' in obj
	);
};

export const isFileEntryArray = (value: unknown): value is FileEntry[] => {
	return Array.isArray(value) && value.length > 0 && value.every(isFileEntry);
};

export const convertToAnalysisCentricDocuments = (records: DataRecordNested[]) => {
	return records.map<AnalysisCentricDocument>((analysis) => ({
		_id: analysis.analysisId?.toString(),
		analysisId: analysis.analysisId?.toString(),
		studyId: analysis.studyId?.toString(),
		state: analysis.analysisState?.toString(),
		analysis: { ...analysis },
	}));
};

export const convertToFileCentricDocuments = (repoInfo: SongRepositoryConfig, records: DataRecordNested[]) => {
	return records.flatMap((record) => {
		const { files, studyId, ...analysisWithoutFiles } = record;

		if (!isFileEntryArray(files) || files.length === 0) {
			logger.error(`Analysis '${record.analysisId}' does not include any file`);
			return [];
		}

		return files.map<FileCentricDocument>((file) => ({
			_id: file.objectId,
			analysis: analysisWithoutFiles,
			data_type: file.dataType,
			file: {
				name: file.fileName,
				data_type: file.fileType,
				size: file.fileSize,
				md5sum: file.fileMd5sum,
			},
			file_access: file.fileAccess,
			file_type: file.fileType,
			object_id: file.objectId,
			repositories: [
				{
					code: repoInfo.code,
					organization: repoInfo.organization,
					name: repoInfo.name,
					type: repoInfo.type,
					country: repoInfo.country,
					url: repoInfo.baseUrl,
				},
			],
			study_id: studyId?.toString(),
		}));
	});
};

/**
 * This function converts any Song document into a fileCentric or AnalysisCentric
 * @param repoInfo - Repository information to be used
 * @param records - Raw document to be converted
 * @returns A formatted fileCentric or analysisCentric array
 */
export const convertAnalyses = (repoInfo: SongRepositoryConfig, records: DataRecordNested[]) => {
	if (repoInfo.indexingMode === IndexingMode.fileCentric) {
		return convertToFileCentricDocuments(repoInfo, records);
	}

	// Default to analysis-centric mode
	return convertToAnalysisCentricDocuments(records);
};
