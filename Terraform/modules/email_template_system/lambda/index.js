const { S3Client, GetObjectCommand } = require('@aws-sdk/client-s3');
const { SNSClient, PublishCommand } = require('@aws-sdk/client-sns');
const { SESClient, SendEmailCommand } = require('@aws-sdk/client-ses');
const { Client } = require('pg');

const s3Client = new S3Client();
const snsClient = new SNSClient();
const sesClient = new SESClient();

exports.handler = async (event) => {
    try {
        // Parse the S3 event
        const s3Event = event.Records[0].s3;
        const bucketName = s3Event.bucket.name;
        const objectKey = decodeURIComponent(s3Event.object.key.replace(/\+/g, ' '));

        // Get the template from S3
        const getObjectCommand = new GetObjectCommand({
            Bucket: bucketName,
            Key: objectKey
        });
        const templateResponse = await s3Client.send(getObjectCommand);
        const templateContent = await templateResponse.Body.transformToString();

        // Get subscribers from the database
        const dbClient = new Client({
            host: process.env.DB_HOST,
            port: process.env.DB_PORT,
            database: process.env.DB_NAME,
            user: process.env.DB_USER,
            password: process.env.DB_PASSWORD,
            ssl: true
        });

        await dbClient.connect();
        const result = await dbClient.query('SELECT email FROM subscribers WHERE is_active = true');
        const subscribers = result.rows.map(row => row.email);
        await dbClient.end();

        // Send email to each subscriber
        for (const email of subscribers) {
            const sendEmailCommand = new SendEmailCommand({
                Source: process.env.SENDER_EMAIL,
                Destination: {
                    ToAddresses: [email]
                },
                Message: {
                    Subject: {
                        Data: 'New Special Offer from Genwin'
                    },
                    Body: {
                        Html: {
                            Data: templateContent
                        }
                    }
                }
            });

            await sesClient.send(sendEmailCommand);
        }

        // Publish success message to SNS
        const publishCommand = new PublishCommand({
            TopicArn: process.env.SNS_TOPIC_ARN,
            Message: JSON.stringify({
                status: 'success',
                template: objectKey,
                subscribersCount: subscribers.length,
                timestamp: new Date().toISOString()
            })
        });

        await snsClient.send(publishCommand);

        return {
            statusCode: 200,
            body: JSON.stringify({
                message: 'Emails sent successfully',
                subscribersCount: subscribers.length
            })
        };
    } catch (error) {
        console.error('Error:', error);
        
        // Publish error message to SNS
        const publishCommand = new PublishCommand({
            TopicArn: process.env.SNS_TOPIC_ARN,
            Message: JSON.stringify({
                status: 'error',
                error: error.message,
                timestamp: new Date().toISOString()
            })
        });

        await snsClient.send(publishCommand);

        throw error;
    }
}; 