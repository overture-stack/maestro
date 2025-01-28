import { ElasticSearchSupportedVersions } from '@overture-stack/maestro-common';

import bulkCreateDataTest from './bulk/create.spec';
import bulkDeleteDataTest from './bulk/delete.spec';
import bulkUpdateDataTest from './bulk/update.spec';
import bulkUpsertDataTest from './bulk/upsert.spec';
import createdIndexTest from './createdIndex.spec';
import deteleDataTest from './deleteData.spec';
import indexDataTest from './indexData.spec';
import pingTest from './ping.spec';
import updateIndexDataTest from './updateIndexedData.spec';

describe('Integration tests', function () {
	describe('Client V7', function () {
		before(function () {
			this.dockerImage = 'docker.elastic.co/elasticsearch/elasticsearch:7.10.1';
			this.clientVersion = ElasticSearchSupportedVersions.V7;
		});

		// test operations
		describe('Create index', function () {
			createdIndexTest.call(this);
		});
		describe('Index Data', function () {
			indexDataTest.call(this);
		});
		describe('Ping server', function () {
			pingTest.call(this);
		});
		describe('Update Data', function () {
			updateIndexDataTest.call(this);
		});
		describe('Delete Data', function () {
			deteleDataTest.call(this);
		});
		describe('Bulk Create Data', function () {
			bulkCreateDataTest.call(this);
		});
		describe('Bulk Update Data', function () {
			bulkUpdateDataTest.call(this);
		});
		describe('Bulk Delete Data', function () {
			bulkDeleteDataTest.call(this);
		});
		describe('Bulk Upsert Data', function () {
			bulkUpsertDataTest.call(this);
		});
	});

	describe('Client V8', function () {
		before(function () {
			this.dockerImage = 'docker.elastic.co/elasticsearch/elasticsearch:8.1.2';
			this.clientVersion = ElasticSearchSupportedVersions.V8;
		});

		// test operations
		describe('Create index', function () {
			createdIndexTest.call(this);
		});
		describe('Index Data', function () {
			indexDataTest.call(this);
		});
		describe('Ping server', function () {
			pingTest.call(this);
		});
		describe('Update Data', function () {
			updateIndexDataTest.call(this);
		});
		describe('Delete Data', function () {
			deteleDataTest.call(this);
		});
		describe('Bulk Create Data', function () {
			bulkCreateDataTest.call(this);
		});
		describe('Bulk Update Data', function () {
			bulkUpdateDataTest.call(this);
		});
		describe('Bulk Delete Data', function () {
			bulkDeleteDataTest.call(this);
		});
		describe('Bulk Upsert Data', function () {
			bulkUpsertDataTest.call(this);
		});
	});
});
