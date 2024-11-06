import createdIndexTest from './createdIndex.spec';
import indexDataTest from './indexData.spec';
import pingTest from './ping.spec';
import updateIndexDataTest from './updateIndexedData.spec';
import bulkUpsertDataTest from './bulkUpsertIndexedData.spec';

describe('Integration tests', function () {
	describe('Client V7', function () {
		before(function () {
			this.dockerImage = 'docker.elastic.co/elasticsearch/elasticsearch:7.10.1';
			this.clientVersion = 7;
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
		describe('Bulk upsert Data', function () {
			bulkUpsertDataTest.call(this);
		});
	});

	describe('Client V8', function () {
		before(function () {
			this.dockerImage = 'docker.elastic.co/elasticsearch/elasticsearch:8.1.2';
			this.clientVersion = 8;
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
		describe('Bulk upsert Data', function () {
			bulkUpsertDataTest.call(this);
		});
	});
});
