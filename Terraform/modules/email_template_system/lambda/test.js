const { handler } = require('./index');

// Mock S3 event
const mockEvent = {
    Records: [{
        s3: {
            bucket: {
                name: 'test-bucket'
            },
            object: {
                key: 'test-template.html'
            }
        }
    }]
};

// Mock environment variables
process.env.DB_HOST = 'localhost';
process.env.DB_PORT = '5432';
process.env.DB_NAME = 'testdb';
process.env.DB_USER = 'testuser';
process.env.DB_PASSWORD = 'testpass';
process.env.SENDER_EMAIL = 'test@example.com';
process.env.SNS_TOPIC_ARN = 'arn:aws:sns:region:account:topic';

// Run the handler
handler(mockEvent)
    .then(response => {
        console.log('Success:', response);
    })
    .catch(error => {
        console.error('Error:', error);
    }); 