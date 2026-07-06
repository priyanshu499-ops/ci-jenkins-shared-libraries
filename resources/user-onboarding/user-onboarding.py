import requests
import os
import csv
import random
import string
import logging
import smtplib
from email.mime.text import MIMEText

# ==========================
# ENV
# ==========================

JENKINS_URL = os.environ.get("JENKINS_URL")
ADMIN_USER  = os.environ.get("ADMIN_USER")
ADMIN_TOKEN = os.environ.get("ADMIN_TOKEN")

MODE       = os.environ.get("MODE")
SEND_EMAIL = os.environ.get("SEND_EMAIL", "false").lower() == "true"

CSV_PATH = os.getenv("CSV_PATH", "resources/user-onboarding/users.csv")

SMTP_HOST = os.environ.get("SMTP_HOST")
SMTP_PORT = int(os.environ.get("SMTP_PORT", 25))

logging.basicConfig(level=logging.INFO, format="%(levelname)s: %(message)s")

# ==========================
# PASSWORD
# ==========================

def generate_password(length=12):
    chars = string.ascii_letters + string.digits + "@#%!"
    return ''.join(random.choice(chars) for _ in range(length))

# ==========================
# CRUMB
# ==========================

def get_crumb():
    url = f"{JENKINS_URL}/crumbIssuer/api/json"
    res = requests.get(url, auth=(ADMIN_USER, ADMIN_TOKEN))
    if res.status_code == 200:
        data = res.json()
        return data["crumbRequestField"], data["crumb"]
    return None, None

# ==========================
# CORRECT USER CHECK (FINAL FIX)
# ==========================

def user_exists(username):
    url = f"{JENKINS_URL}/asynchPeople/api/json"
    res = requests.get(url, auth=(ADMIN_USER, ADMIN_TOKEN))

    if res.status_code != 200:
        return False

    users = res.json().get("users", [])

    for u in users:
        if u.get("user", {}).get("id") == username:
            return True

    return False

# ==========================
# CREATE USER
# ==========================

def create_user(username, password, email):
    crumb_field, crumb = get_crumb()

    url = f"{JENKINS_URL}/securityRealm/createAccountByAdmin"

    payload = {
        "username": username,
        "password1": password,
        "password2": password,
        "fullname": username,
        "email": email
    }

    headers = {crumb_field: crumb} if crumb else {}

    res = requests.post(url, data=payload, headers=headers,
                        auth=(ADMIN_USER, ADMIN_TOKEN))

    if res.status_code in [200, 302]:
        logging.info(f"[CREATED] {username}")
        return True
    else:
        logging.error(f"[FAILED CREATE] {username} → {res.text}")
        return False

# ==========================
# ROLE ASSIGN
# ==========================

def assign_role(username, role):
    crumb_field, crumb = get_crumb()
    url = f"{JENKINS_URL}/role-strategy/strategy/assignRole"

    payload = {
        "type": "projectRoles",
        "roleName": role,
        "sid": username
    }

    headers = {crumb_field: crumb} if crumb else {}

    requests.post(url, data=payload, headers=headers,
                  auth=(ADMIN_USER, ADMIN_TOKEN))

    logging.info(f"[ROLE] {role} → {username}")

# ==========================
# EMAIL
# ==========================

def send_email(to_email, username, password, roles):
    if not SEND_EMAIL:
        return

    body = f"""
Hello,

Your Jenkins account is created.

Username: {username}
Password: {password}
Roles: {roles}

URL: {JENKINS_URL}
"""

    msg = MIMEText(body)
    msg["Subject"] = "Jenkins Access"
    msg["From"] = "jenkins@company.com"
    msg["To"] = to_email

    try:
        server = smtplib.SMTP(SMTP_HOST, SMTP_PORT)
        server.sendmail(msg["From"], [to_email], msg.as_string())
        server.quit()
        logging.info(f"[EMAIL SENT] {to_email}")
    except Exception as e:
        logging.error(f"[EMAIL FAILED] {e}")

# ==========================
# BULK MODE
# ==========================

def bulk_mode():
    try:
        with open(CSV_PATH) as f:
            reader = csv.DictReader(f)

            for row in reader:
                username = row["username"].strip()
                email = row["email"].strip()
                roles = [r.strip() for r in row["roles"].split(",")]

                password = generate_password()

                # 🔥 CHECK USER
                if user_exists(username):
                    logging.info(f"[SKIPPED - EXISTS] {username}")
                    continue   # 👈 FULL SKIP (no role change)

                # ✅ CREATE USER
                created = create_user(username, password, email)

                if not created:
                    continue

                # ✅ ASSIGN ROLES (ONLY FOR NEW USER)
                for role in roles:
                    assign_role(username, role)

                # ✅ EMAIL
                send_email(email, username, password, ",".join(roles))

        logging.info("✅ Bulk completed")

    except Exception as e:
        logging.error(f"❌ Bulk failed: {e}")

# ==========================
# MAIN
# ==========================

if __name__ == "__main__":
    if MODE == "bulk":
        bulk_mode()
