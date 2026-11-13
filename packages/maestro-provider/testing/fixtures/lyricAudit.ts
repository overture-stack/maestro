/**
 * One row of Lyric's audit, in the shape Lyric's audit route returns it: by default, the deletion of `systemId` from
 * `organization`. `event` names any other change.
 */
export const lyricAuditRow = ({
	event = 'DELETE',
	organization,
	systemId,
}: {
	event?: string;
	organization: string;
	systemId: string;
}): Record<string, unknown> => ({
	createdAt: '2026-11-10T14:00:00.000Z',
	createdBy: 'submitter',
	dataDiff: { new: {}, old: { sample_id: 'SAMPLE-1' } },
	entityName: 'sample',
	errors: null,
	event,
	newIsValid: false,
	oldIsValid: true,
	organization,
	submissionId: 12,
	systemId,
});

/**
 * Lyric's answer to a query of its audit, as `GET /audit/category/:categoryId/organization/:organization` returns it,
 * holding `rows` on a single page.
 */
export const lyricAuditResponse = (
	rows: Array<Record<string, unknown>>,
): { pagination: Record<string, number>; records: Array<Record<string, unknown>> } => ({
	pagination: { currentPage: 1, pageSize: 20, totalPages: 1, totalRecords: rows.length },
	records: rows,
});
