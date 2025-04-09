const { S3Client, GetObjectCommand } = require('@aws-sdk/client-s3');
const { SNSClient, PublishCommand } = require('@aws-sdk/client-sns');
const { SESClient, SendEmailCommand } = require('@aws-sdk/client-ses');
const { Client } = require('pg');

const s3Client = new S3Client();
const snsClient = new SNSClient();
const sesClient = new SESClient();

exports.handler = async (event) => {
    try {
        console.log('Event received:', JSON.stringify(event));
        
        // Parse the event - handle both direct S3 events and SNS-wrapped S3 events
        let s3Event;
        
        if (event.Records && event.Records[0].Sns) {
            // This is an SNS event wrapping an S3 event
            console.log('Processing SNS-wrapped S3 event');
            const snsMessage = JSON.parse(event.Records[0].Sns.Message);
            s3Event = snsMessage.Records[0].s3;
        } else if (event.Records && event.Records[0].s3) {
            // This is a direct S3 event
            console.log('Processing direct S3 event');
            s3Event = event.Records[0].s3;
        } else {
            throw new Error('Unsupported event format');
        }
        
        const bucketName = s3Event.bucket.name;
        const objectKey = decodeURIComponent(s3Event.object.key.replace(/\+/g, ' '));
        
        console.log(`Processing template from bucket: ${bucketName}, key: ${objectKey}`);

        // Get the template from S3
        const getObjectCommand = new GetObjectCommand({
            Bucket: bucketName,
            Key: objectKey
        });
        const templateResponse = await s3Client.send(getObjectCommand);
        const templateContent = await templateResponse.Body.transformToString();
        console.log('Template retrieved successfully');

        // Get subscribers from the database
        console.log('Connecting to database...');
        const dbClient = new Client({
            host: process.env.DB_HOST,
            port: process.env.DB_PORT || 5432,
            database: process.env.DB_NAME,
            user: process.env.DB_USER,
            password: process.env.DB_PASSWORD,
            ssl: true
        });

        await dbClient.connect();
        console.log('Connected to database');
        
        const result = await dbClient.query('SELECT email FROM subscribers WHERE is_active = true');
        const subscribers = result.rows.map(row => row.email);
        console.log(`Found ${subscribers.length} subscribers`);
        
        await dbClient.end();
        console.log('Database connection closed');

        // Send email to each subscriber
        console.log('Sending emails to subscribers...');
        for (const email of subscribers) {
            console.log(`Sending email to ${email}`);
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
            console.log(`Email sent to ${email}`);
        }

        // Publish success message to SNS
        console.log('Publishing success message to SNS');
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
        console.log('Success message published to SNS');

        return {
            statusCode: 200,
            body: JSON.stringify({
                message: 'Emails sent successfully',
                subscribersCount: subscribers.length
            })
        };
    } catch (error) {
        console.error('Error:', error);
        console.error('Error stack:', error.stack);
        
        // Publish error message to SNS
        try {
            const publishCommand = new PublishCommand({
                TopicArn: process.env.SNS_TOPIC_ARN,
                Message: JSON.stringify({
                    status: 'error',
                    error: error.message,
                    timestamp: new Date().toISOString()
                })
            });

            await snsClient.send(publishCommand);
            console.log('Error message published to SNS');
        } catch (snsError) {
            console.error('Failed to publish error to SNS:', snsError);
        }

        throw error;
    }
}; 