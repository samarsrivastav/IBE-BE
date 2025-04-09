resource "aws_s3_bucket_notification" "email_templates" {
  bucket = aws_s3_bucket.email_templates.id

  lambda_function {
    lambda_function_arn = aws_lambda_function.promotional_email_sender.arn
    events              = ["s3:ObjectCreated:*"]
    filter_prefix       = "templates/"
  }
}

resource "aws_lambda_permission" "allow_s3" {
  statement_id  = "AllowS3Invoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.promotional_email_sender.function_name
  principal     = "s3.amazonaws.com"
  source_arn    = aws_s3_bucket.email_templates.arn
} 