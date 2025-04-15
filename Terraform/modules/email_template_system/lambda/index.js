const { S3Client, GetObjectCommand } = require('@aws-sdk/client-s3');
const { Client } = require('pg');
const nodemailer = require('nodemailer');

const s3Client = new S3Client();

// Database connection configuration
const dbConfig = {
    host: process.env.DB_HOST,
    port: process.env.DB_PORT,
    database: process.env.DB_NAME,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    ssl: true
};

// SMTP configuration
const smtpConfig = {
    host: process.env.SMTP_HOST,
    port: process.env.SMTP_PORT,
    secure: 'true',
    auth: {
        user: process.env.SMTP_USER,
        pass: process.env.SMTP_PASSWORD
    }
};

// Create reusable transporter object
const transporter = nodemailer.createTransport(smtpConfig);

exports.handler = async (event) => {
    try {
        console.log('Event received:', JSON.stringify(event));
        
        // Parse the S3 event
        const s3Event = event.Records[0].s3;
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
        const dbClient = new Client(dbConfig);
        await dbClient.connect();
        console.log('Connected to database');
        
        const result = await dbClient.query('SELECT email FROM special_offers');
        console.log(result)
        const subscribers = result.rows.map(row => row.email);
        console.log(`Found ${subscribers.length} subscribers`);
        
        await dbClient.end();
        console.log('Database connection closed');

        // Send email to each subscriber via SMTP
        console.log('Sending emails to subscribers...');
        for (const email of subscribers) {
            console.log(`Sending email to ${email}`);
            
            const mailOptions = {
                from: process.env.SMTP_FROM,
                to: email,
                subject: 'New Special Offer from Genwin',
                html: templateContent
            };

            await transporter.sendMail(mailOptions);
            console.log(`Email sent to ${email}`);
        }

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
        throw error;
    }
}; 