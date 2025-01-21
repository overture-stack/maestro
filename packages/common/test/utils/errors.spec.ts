import { expect } from 'chai';
import { describe, it } from 'mocha';

import {
	BadRequest,
	getErrorMessage,
	InternalServerError,
	NotFound,
	NotImplemented,
	ServiceUnavailable,
} from '../../src/utils/errors.js';

describe('Errors', () => {
	describe('Error classes', () => {
		describe('BadRequest', () => {
			it('should return a BadRequest instance with details', () => {
				const result = new BadRequest('Invalid request');
				expect(result).to.be.an.instanceOf(BadRequest);
				expect(result.message).to.eql('Bad Request');
				expect(result.details).to.eql('Invalid request');
				expect(result.timestamp).to.not.eql(undefined);
			});
			it('should return a BadRequest instance without details', () => {
				const result = new BadRequest();
				expect(result).to.be.an.instanceOf(BadRequest);
				expect(result.message).to.eql('Bad Request');
				expect(result.details).to.eql(undefined);
				expect(result.timestamp).to.not.eql(undefined);
			});
		});
		describe('NotFound', () => {
			it('should return a NotFound instance with details', () => {
				const result = new NotFound('Data not found');
				expect(result).to.be.an.instanceOf(NotFound);
				expect(result.message).to.eql('Not Found');
				expect(result.details).to.eql('Data not found');
				expect(result.timestamp).to.not.eql(undefined);
			});
			it('should return a NotFound instance without details', () => {
				const result = new NotFound();
				expect(result).to.be.an.instanceOf(NotFound);
				expect(result.message).to.eql('Not Found');
				expect(result.details).to.eql(undefined);
				expect(result.timestamp).to.not.eql(undefined);
			});
		});
		describe('NotImplemented', () => {
			it('should return a NotImplemented instance with details', () => {
				const result = new NotImplemented('Service not implemented');
				expect(result).to.be.an.instanceOf(NotImplemented);
				expect(result.message).to.eql('Not Implemented');
				expect(result.details).to.eql('Service not implemented');
				expect(result.timestamp).to.not.eql(undefined);
			});
			it('should return a NotImplemented instance with default details', () => {
				const result = new NotImplemented();
				expect(result).to.be.an.instanceOf(NotImplemented);
				expect(result.message).to.eql('Not Implemented');
				expect(result.details).to.eql('This functionallity is not yet implemented');
				expect(result.timestamp).to.not.eql(undefined);
			});
		});
		describe('ServiceUnavailable', () => {
			it('should return a ServiceUnavailable instance with details', () => {
				const result = new ServiceUnavailable('Service not available');
				expect(result).to.be.an.instanceOf(ServiceUnavailable);
				expect(result.message).to.eql('Service unavailable');
				expect(result.details).to.eql('Service not available');
				expect(result.timestamp).to.not.eql(undefined);
			});
			it('should return a ServiceUnavailable instance with default details', () => {
				const result = new ServiceUnavailable();
				expect(result).to.be.an.instanceOf(ServiceUnavailable);
				expect(result.message).to.eql('Service unavailable');
				expect(result.details).to.eql(
					'Server is unable to access the necessary resources to process the request. Please try again later.',
				);
				expect(result.timestamp).to.not.eql(undefined);
			});
		});
		describe('InternalServerError', () => {
			it('should return a InternalServerError instance with details', () => {
				const result = new InternalServerError('An error occurred');
				expect(result).to.be.an.instanceOf(InternalServerError);
				expect(result.message).to.eql('Internal Server Error');
				expect(result.details).to.eql('An error occurred');
				expect(result.timestamp).to.not.eql(undefined);
			});
			it('should return a InternalServerError instance with default details', () => {
				const result = new InternalServerError();
				expect(result).to.be.an.instanceOf(InternalServerError);
				expect(result.message).to.eql('Internal Server Error');
				expect(result.details).to.eql('Something unexpected happened');
				expect(result.timestamp).to.not.eql(undefined);
			});
		});
	});
	describe('Error utils', () => {
		it('should return the message from InternalServerError instance', () => {
			const error = new InternalServerError();
			const message = getErrorMessage(error);
			expect(message).to.eql('Internal Server Error');
		});
		it('should return the message from Error instance', () => {
			const error = new Error('An error occurred');
			const message = getErrorMessage(error);
			expect(message).to.eql('An error occurred');
		});
		it('should return a string from an String instance', () => {
			const error = new String('An error occurred');
			const message = getErrorMessage(error);
			expect(message).to.eql('An error occurred');
		});
	});
});
