import createdIndexTest from './createdIndex.spec';
import indexDataTest from './indexData.spec';
import pingTest from './ping.spec';
import updateIndexDataTest from './updateIndexedData.spec';

describe('Integration tests - Client V7', function () {
	before(function () {
		this.dockerImage = 'docker.elastic.co/elasticsearch/elasticsearch:7.10.1';
		this.clientVersion = 7;
	});

	// test operations
	describe('Create index', createdIndexTest.bind(this));
	describe('Index Data', indexDataTest.bind(this));
	describe('Ping server', pingTest.bind(this));
	describe('Update Data', updateIndexDataTest.bind(this));
});
