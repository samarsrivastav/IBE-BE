const AWS = require('aws-sdk');
const { Client } = require('pg');
const nodemailer = require('nodemailer');
const fs = require('fs');
const path = require('path');

const s3 = new AWS.S3();
const sns = new AWS.SNS();

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

// Email configuration
const emailConfig = {
  host: 'smtp.gmail.com',
  port: 587,
  secure: false,
  auth: {
    user: process.env.SENDER_EMAIL,
    pass: process.env.EMAIL_PASSWORD
  }
};

const transporter = nodemailer.createTransport(emailConfig);

exports.handler = async (event) => {
  try {
    // Parse the SNS message
    const snsMessage = JSON.parse(event.Records[0].Sns.Message);
    const { email, templateName, data } = snsMessage;

    // Get the template from S3
    const templateParams = {
      Bucket: process.env.TEMPLATE_BUCKET,
      Key: `${templateName}.html`
    };

    const templateObject = await s3.getObject(templateParams).promise();
    let templateContent = templateObject.Body.toString('utf-8');

    // Replace placeholders in the template
    Object.keys(data).forEach(key => {
      const regex = new RegExp(`{{${key}}}`, 'g');
      templateContent = templateContent.replace(regex, data[key]);
    });

    // Send the email
    const mailOptions = {
      from: process.env.SENDER_EMAIL,
      to: email,
      subject: data.subject || 'New Offer from Genwin',
      html: templateContent
    };

    await transporter.sendMail(mailOptions);

    // Log the email sent in the database
    const client = new Client(dbConfig);
    await client.connect();

    const query = `
      INSERT INTO email_logs (recipient_email, template_name, sent_at)
      VALUES ($1, $2, NOW())
    `;

    await client.query(query, [email, templateName]);
    await client.end();

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