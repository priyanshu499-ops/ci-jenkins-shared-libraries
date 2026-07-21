import requests
import os
import sys
import csv
import json
import random
import string
import logging

# ==========================
# ENV
# ==========================

JENKINS_URL = os.environ.get("JENKINS_URL")
ADMIN_USER  = os.environ.get("ADMIN_USER")
ADMIN_TOKEN = os.environ.get("ADMIN_TOKEN")

MODE       = os.environ.get("MODE")

CSV_PATH = os.getenv("CSV_PATH", "resources/user-onboarding/users.csv")
RESULTS_FILE = os.getenv("RESULTS_FILE", "onboarding_results.json")

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
# BULK MODE
# ==========================

def bulk_mode():
    results = []

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
                    results.append({
                        "username": username,
                        "email": email,
                        "roles": roles,
                        "status": "skipped",
                        "reason": "already exists"
                    })
                    continue   # 👈 FULL SKIP (no role change)

                # ✅ CREATE USER
                created = create_user(username, password, email)

                if not created:
                    results.append({
                        "username": username,
                        "email": email,
                        "roles": roles,
                        "status": "failed",
                        "reason": "create failed"
                    })
                    continue

                # ✅ ASSIGN ROLES (ONLY FOR NEW USER)
                for role in roles:
                    assign_role(username, role)

                # ✅ SAVE RESULT (for Jenkinsfile to send email/gchat)
                results.append({
                    "username": username,
                    "email": email,
                    "password": password,
                    "roles": roles,
                    "status": "created"
                })

        # Write results to JSON file
        with open(RESULTS_FILE, 'w') as f:
            json.dump(results, f, indent=2)

        logging.info(f"✅ Bulk completed — results saved to {RESULTS_FILE}")

    except Exception as e:
        logging.error(f"❌ Bulk failed: {e}")
        sys.exit(1)

# ==========================
# MAIN
# ==========================

if __name__ == "__main__":
    if MODE == "bulk":
        bulk_mode()
