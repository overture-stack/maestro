# Migration Guide

Each section covers one change to something you already use: what changed, who is affected, how to tell, and what to do. Sections are grouped by the release you are upgrading from.

## Upgrading from 5.0.x

<a id="lyric-listing-errors"></a>

### A reindex reads every page of Lyric records, and stops at one it cannot read

When Lyric answers a page of records with anything but success, the reindex ends and logs an error naming the status, such as `Lyric answered 503 for page 2 of organization 'ABC'`. Before, it ended without a log entry. A first page answered with `404` still means the organization or category holds no records. The pages read before the error stay indexed.

This affects anyone watching Maestro's logs for failed reindexes. Nothing needs to change.

Without a configured page size (`MAESTRO_REPOSITORIES_<n>_PAGINATION_SIZE`), a reindex of a Lyric repository now reads every page of records, at Lyric's default of 20 records per page. Before, it read only the first page. This affects deployments leaving the page size unset: they now index every record, and their reindexes run longer. Set a page size to choose how many records each request reads.
