import os, json, re, smtplib, psycopg2
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart
from pathlib import Path
from dotenv import load_dotenv
from confluent_kafka import Consumer, KafkaError

# Resolve directories
BASE_DIR = Path(__file__).resolve().parent
ENV_PATH = BASE_DIR / ".env"
CA_PATH = BASE_DIR / "ca.pem"

print(f"Loading environment from {ENV_PATH}")
load_dotenv(dotenv_path=ENV_PATH)

# DB connection helper
def get_db_connection():
    host = os.getenv("DB_HOST")
    port = os.getenv("DB_PORT", "5432")
    dbname = os.getenv("DB_NAME")
    user = os.getenv("DB_USERNAME")
    password = os.getenv("DB_PASSWORD")
    
    print("Connecting to database...")
    try:
        if not all([host, dbname, user, password]):
            raise ValueError("Database configuration variables (DB_HOST, DB_NAME, DB_USERNAME, DB_PASSWORD) are not fully set")
            
        conn = psycopg2.connect(
            host=host,
            port=port,
            database=dbname,
            user=user,
            password=password,
            sslmode="require"
        )
        print("Database connection established successfully.")
        return conn
    except Exception as e:
        print(f"[ERROR] Failed to connect to database: {e}")
        raise e

# Email helper
def send_email(to_email, subject, message_content):
    smtp_host = os.getenv("MAIL_HOST")
    smtp_port = os.getenv("MAIL_PORT", "587")
    username = os.getenv("MAIL_USERNAME")
    password = os.getenv("MAIL_PASSWORD")
    
    if not all([smtp_host, username, password]):
        print("[WARNING] SMTP configuration incomplete. Skipping email delivery.")
        return False
        
    print(f"Sending notification email to {to_email}...")
    try:
        msg = MIMEMultipart('alternative')
        msg['Subject'] = subject
        msg['From'] = f"CloudFlows <{username}@cloudflows.com>"
        msg['To'] = to_email

        # Simple HTML wrapper
        html_content = f"""
        <html>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px; border: 1px style=solid #eee; border-radius: 5px;">
                    <h2 style="color: #4f46e5; border-bottom: 2px solid #f3f4f6; padding-bottom: 10px;">CloudFlows Notification</h2>
                    <p style="font-size: 16px;">{message_content}</p>
                    <p style="margin-top: 30px; font-size: 12px; color: #9ca3af; border-top: 1px solid #f3f4f6; padding-top: 10px;">
                        This is an automated notification. Please do not reply directly to this email.
                    </p>
                </div>
            </body>
        </html>
        """
        
        part = MIMEText(html_content, 'html')
        msg.attach(part)

        with smtplib.SMTP(smtp_host, int(smtp_port)) as server:
            server.starttls()
            server.login(username, password)
            server.sendmail(msg['From'], to_email, msg.as_string())
            
        print(f"Email sent successfully to {to_email}")
        return True
    except Exception as e:
        print(f"[ERROR] Failed to send email to {to_email}: {e}")
        return False

# Database update helper
def update_notification_status(conn, appointment_id):
    print(f"Updating notification status for appointment ID: {appointment_id}")
    try:
        with conn.cursor() as cur:
            cur.execute(
                "UPDATE appointments SET notification_sent = TRUE, updated_at = NOW() WHERE id = %s",
                (appointment_id,)
            )
            conn.commit()
            print(f"Successfully updated notification_sent to TRUE for appointment ID: {appointment_id}")
    except Exception as e:
        conn.rollback()
        print(f"[ERROR] Failed to update database for appointment ID {appointment_id}: {e}")
        raise e

# Kafka Event Processing Handlers
def handle_notification_event(event_data, conn):
    print(f"Processing notification event: {event_data.get('eventId')}")
    
    recipient_email = event_data.get("recipientEmail")
    subject = event_data.get("subject")
    message = event_data.get("message")
    appointment_id = event_data.get("appointmentId")
    
    if not recipient_email or not appointment_id:
        print("[ERROR] Invalid notification event data: missing recipientEmail or appointmentId")
        return
        
    # 1. Send Email
    success = send_email(recipient_email, subject, message)
    
    # 2. Update Database Status
    if success:
        update_notification_status(conn, appointment_id)
    else:
        print(f"[WARNING] Failed to send email. Skipping database update for appointment ID: {appointment_id}")

def handle_appointment_event(event_data):
    event_type = event_data.get("eventType")
    appointment_id = event_data.get("appointmentId")
    user_email = event_data.get("userEmail")
    doctor_name = event_data.get("doctorName")
    status = event_data.get("status")
    
    print(f"=== APPOINTMENT EVENT [{event_type}] ===")
    print(f"Appointment ID   : {appointment_id}")
    print(f"User Email       : {user_email}")
    print(f"Doctor Name      : {doctor_name}")
    print(f"New Status       : {status}")
    print("========================================")

def main():
    # Initialize DB connection
    conn = get_db_connection()
    
    # Kafka Consumer Configuration
    bootstrap_servers = os.getenv("KAFKA_BOOTSTRAP_SERVERS")
    username = os.getenv("KAFKA_SASL_USERNAME")
    password = os.getenv("KAFKA_SASL_PASSWORD")
    security_protocol = os.getenv("KAFKA_SECURITY_PROTOCOL", "SASL_SSL")
    sasl_mechanism = os.getenv("KAFKA_SASL_MECHANISM", "SCRAM-SHA-256")
    group_id = os.getenv("KAFKA_CONSUMER_GROUP_ID", "python-worker-group")
    
    conf = {
        'bootstrap.servers': bootstrap_servers,
        'group.id': group_id,
        'auto.offset.reset': 'earliest',
        'security.protocol': security_protocol,
        'sasl.mechanism': sasl_mechanism,
        'sasl.username': username,
        'sasl.password': password
    }
    
    # SSL verification using the Aiven CA file
    if security_protocol in ["SSL", "SASL_SSL"]:
        if CA_PATH.exists():
            conf['ssl.ca.location'] = str(CA_PATH)
            print(f"Using CA certificate at: {CA_PATH}")
        else:
            print(f"[WARNING] CA certificate not found at {CA_PATH}. SSL handshake might fail.")
            
    print("Initializing Kafka Consumer...")
    consumer = Consumer(conf)
    
    # Read topics from env or default
    appointment_topic = os.getenv("KAFKA_TOPIC_APPOINTMENT", "appointment-events")
    notification_topic = os.getenv("KAFKA_TOPIC_NOTIFICATION", "notification-events")
    
    topics = [appointment_topic, notification_topic]
    print(f"Subscribing to topics: {topics}")
    consumer.subscribe(topics)
    
    print("Starting message consumption loop. Press Ctrl+C to exit.")
    
    try:
        while True:
            msg = consumer.poll(timeout=1.0)
            if msg is None:
                continue
                
            if msg.error():
                if msg.error().code() == KafkaError._PARTITION_EOF:
                    # End of partition event
                    print(f"[DEBUG] Reached end of partition {msg.topic()} [{msg.partition()}] at offset {msg.offset()}")
                elif msg.error():
                    print(f"[ERROR] Kafka error: {msg.error()}")
                continue
                
            # Process received message
            topic = msg.topic()
            payload = msg.value().decode('utf-8')
            
            try:
                event_data = json.loads(payload)
                print(f"Received message from topic [{topic}]")
                
                # Check topic and handle
                if topic == notification_topic:
                    handle_notification_event(event_data, conn)
                elif topic == appointment_topic:
                    handle_appointment_event(event_data)
                else:
                    print(f"[WARNING] Received message on unhandled topic [{topic}]: {event_data}")
                    
            except json.JSONDecodeError:
                print(f"[ERROR] Failed to decode JSON payload: {payload}")
            except Exception as e:
                print(f"[ERROR] Error processing message: {e}")
                # Re-verify DB connection in case of failure
                try:
                    if conn.closed:
                        conn = get_db_connection()
                except Exception as db_err:
                    print(f"[CRITICAL] Failed to restore DB connection: {db_err}")
                    
    except KeyboardInterrupt:
        print("Aborting by user request.")
    finally:
        # Clean up
        print("Closing Kafka consumer and DB connection...")
        consumer.close()
        if conn:
            conn.close()
        print("Clean shutdown complete.")

if __name__ == "__main__":
    main()
