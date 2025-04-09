const { Client } = require('pg');
const AWS = require('aws-sdk');
const s3 = new AWS.S3();
const sns = new AWS.SNS();

exports.handler = async (event) => {
    try {
        // Get the S3 bucket and key from the event
        const bucket = event.Records[0].s3.bucket.name;
        const key = decodeURIComponent(event.Records[0].s3.object.key.replace(/\+/g, ' '));

        // Get the email template from S3
        const templateData = await s3.getObject({
            Bucket: bucket,
            Key: key
        }).promise();

        const emailTemplate = templateData.Body.toString();

        // Connect to PostgreSQL
        const client = new Client({
            connectionString: process.env.POSTGRES_URL
        });

        await client.connect();

        // Get all users who have opted in for promotions
        const result = await client.query(
            'SELECT email FROM users WHERE special_offers = true'
        );

        const users = result.rows;

        // Send emails to all opted-in users using SNS
        const emailPromises = users.map(user => {
            const params = {
                Message: emailTemplate,
                Subject: 'New Special Offer Available!',
                TopicArn: process.env.SNS_TOPIC_ARN
            };

            return sns.publish(params).promise();
        });

        await Promise.all(emailPromises);

        await client.end();

        return {
            statusCode: 200,
            body: JSON.stringify({
                message: `Successfully sent promotional emails to ${users.length} users`
            })
        };
    } catch (error) {
        console.error('Error:', error);
        throw error;
    }
}; 