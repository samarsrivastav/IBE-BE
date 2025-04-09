const AWS = require('aws-sdk');
const { Client } = require('pg');
const s3 = new AWS.S3();
const sns = new AWS.SNS();

// Database connection configuration
const dbConfig = {
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  host: process.env.DB_HOST,
  port: process.env.DB_PORT || 5432,
  database: process.env.DB_NAME,
  ssl: {
    rejectUnauthorized: false
  }
};

exports.handler = async (event) => {
  const client = new Client(dbConfig);
  
  try {
    // Parse the SNS message
    const snsMessage = JSON.parse(event.Records[0].Sns.Message);
    const s3Event = snsMessage.Records[0];
    
    // Get the bucket and key from the S3 event
    const bucket = s3Event.s3.bucket.name;
    const key = decodeURIComponent(s3Event.s3.object.key.replace(/\+/g, ' '));
    
    // Get the email template from S3
    const templateData = await s3.getObject({
      Bucket: bucket,
      Key: key
    }).promise();
    
    const template = templateData.Body.toString('utf-8');
    
    // Connect to the database
    await client.connect();
    console.log('Connected to database');
    
    // Get all emails from the special_offers table
    const result = await client.query('SELECT email FROM special_offers');
    const emails = result.rows.map(row => row.email);
    
    console.log(`Found ${emails.length} emails to send to`);
    
    // Send emails to all users using SNS
    for (const email of emails) {
      const params = {
        Message: template,
        Subject: 'New Special Offer from Genwin',
        TopicArn: process.env.SNS_TOPIC_ARN,
        MessageAttributes: {
          'email': {
            DataType: 'String',
            StringValue: email
          }
        }
      };
      
      await sns.publish(params).promise();
      console.log(`Sent email to ${email}`);
    }
    
    return {
      statusCode: 200,
      body: JSON.stringify({
        message: 'Emails sent successfully',
        template: key,
        recipients: emails.length
      })
    };
  } catch (error) {
    console.error('Error:', error);
    throw error;
  } finally {
    // Always close the database connection
    await client.end();
    console.log('Database connection closed');
  }
}; 