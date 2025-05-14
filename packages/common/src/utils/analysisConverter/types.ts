import type { DataRecordNested } from '../../types/dataRecord';

/**
 * Type representing the basic structure of a analysisCentric document used by Song documents
 */
export type AnalysisCentricDocument = {
	_id?: string;
	analysisId?: string;
	studyId?: string;
	state?: string;
	analysis: DataRecordNested;
};

/**
 * Type representing the basic structure of a fileCentric document used by Song documents
 */
export type FileCentricDocument = {
	_id: string;
	analysis: Omit<DataRecordNested, 'files'>;
	data_type: string;
	file: {
		name: string;
		data_type: string;
		size: string;
		md5sum: string;
	};
	file_access: string;
	file_type: string;
	object_id: string;
	repositories: {
		code: string;
		organization?: string;
		name: string;
		type?: string;
		country?: string;
		url?: string;
	}[];
	study_id?: string;
};

/**
 * Type representing the structure of a file object returned by Song as part of an analysis.
 * This type is used to transform analysis data into fileCentric documents
 */
export type FileEntry = {
	objectId: string;
	studyId: string;
	analysisId: string;
	fileName: string;
	fileSize: string;
	fileType: string;
	fileMd5sum: string;
	fileAccess: string;
	dataType: string;
};
