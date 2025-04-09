const { S3Client, GetObjectCommand } = require('@aws-sdk/client-s3');
const { SNSClient } = require('@aws-sdk/client-sns');
const { SESClient, SendEmailCommand } = require('@aws-sdk/client-ses');
const { Client } = require('pg');

const s3Client = new S3Client();
const snsClient = new SNSClient();
const sesClient = new SESClient();

// Database connection configuration
const dbConfig = {
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  host: process.env.DB_HOST,
  port: process.env.DB_PORT,
  database: process.env.DB_NAME,
  ssl: {
    rejectUnauthorized: false
  }
};

exports.handler = async (event) => {
  try {
    console.log('Event received:', JSON.stringify(event));
    
    // Parse the SNS message
    const snsMessage = JSON.parse(event.Records[0].Sns.Message);
    console.log('SNS message:', JSON.stringify(snsMessage));
    
    const { email, templateName, data } = snsMessage;

    // Get the template from S3
    const templateParams = {
      Bucket: process.env.TEMPLATE_BUCKET,
      Key: `${templateName}.html`
    };
    
    console.log('Fetching template from S3:', JSON.stringify(templateParams));
    const getObjectCommand = new GetObjectCommand(templateParams);
    const templateObject = await s3Client.send(getObjectCommand);
    
    // Convert stream to string
    const chunks = [];
    for await (const chunk of templateObject.Body) {
      chunks.push(chunk);
    }
    let templateContent = Buffer.concat(chunks).toString('utf-8');

    // Replace placeholders in the template
    Object.keys(data).forEach(key => {
      const regex = new RegExp(`{{${key}}}`, 'g');
      templateContent = templateContent.replace(regex, data[key]);
    });

    // Send the email using SES
    const emailParams = {
      Source: process.env.SENDER_EMAIL,
      Destination: {
        ToAddresses: [email]
      },
      Message: {
        Subject: {
          Data: data.subject || 'New Offer from Genwin'
        },
        Body: {
          Html: {
            Data: templateContent
          }
        }
      }
    };
    
    console.log('Sending email to:', email);
    const sendEmailCommand = new SendEmailCommand(emailParams);
    await sesClient.send(sendEmailCommand);
    console.log('Email sent successfully');

    // Log the email sent in the database
    try {
      console.log('Connecting to database with config:', {
        user: dbConfig.user,
        host: dbConfig.host,
        port: dbConfig.port,
        database: dbConfig.database
      });
      
      const client = new Client(dbConfig);
      await client.connect();
      console.log('Database connection established');

      const query = `
        INSERT INTO email_logs (recipient_email, template_name, sent_at)
        VALUES ($1, $2, NOW())
      `;

      await client.query(query, [email, templateName]);
      console.log('Email log inserted into database');
      
      await client.end();
      console.log('Database connection closed');
    } catch (dbError) {
      console.error('Database error:', dbError);
      // Continue execution even if database logging fails
      // We don't want to fail the email sending if just the logging fails
    }

    return {
      statusCode: 200,
      body: JSON.stringify({ message: 'Email sent successfully' })
    };
  } catch (error) {
    console.error('Error:', error);
    return {
      statusCode: 500,
      body: JSON.stringify({ error: error.message })
    };
  }
}; 