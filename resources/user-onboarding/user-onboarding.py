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

JENKINS_URL = (os.environ.get("JENKINS_URL") or "http://10.10.64.2:8080").rstrip('/')
ADMIN_USER  = os.environ.get("ADMIN_USER")
ADMIN_TOKEN = os.environ.get("ADMIN_TOKEN")

MODE       = os.environ.get("MODE")
USERNAME   = os.environ.get("USERNAME", "").strip()

CSV_PATH = os.getenv("CSV_PATH", "resources/user-onboarding/users.csv")
RESULTS_FILE = os.getenv("RESULTS_FILE", "onboarding_results.json")

logging.basicConfig(level=logging.INFO, format="%(levelname)s: %(message)s")

# ==========================
# PASSWORD GENERATOR
# ==========================

def generate_password(length=12):
    chars = string.ascii_letters + string.digits + "@#%!"
    return ''.join(random.choice(chars) for _ in range(length))

# ==========================
# CRUMB ISSUER
# ==========================

def get_crumb():
    url = f"{JENKINS_URL}/crumbIssuer/api/json"
    try:
        res = requests.get(url, auth=(ADMIN_USER, ADMIN_TOKEN))
        if res.status_code == 200:
            data = res.json()
            return data["crumbRequestField"], data["crumb"]
    except Exception as e:
        logging.warning(f"Could not get crumb: {e}")
    return None, None

# ==========================
# USER EXISTS CHECK
# ==========================

def user_exists(username):
    url = f"{JENKINS_URL}/user/{username}/api/json"
    try:
        res = requests.get(url, auth=(ADMIN_USER, ADMIN_TOKEN))
        if res.status_code == 200:
            data = res.json()
            if data.get("id") == username or data.get("fullName") == username:
                return True
    except Exception as e:
        logging.warning(f"Error checking user {username}: {e}")

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

    res = requests.post(url, data=payload, headers=headers, auth=(ADMIN_USER, ADMIN_TOKEN))

    if res.status_code in [200, 302]:
        logging.info(f"[CREATED] {username}")
        return True
    else:
        logging.error(f"[FAILED CREATE] {username} → Status {res.status_code}")
        return False

# ==========================
# UNASSIGN ROLES
# ==========================

def unassign_roles(username):
    crumb_field, crumb = get_crumb()
    headers = {crumb_field: crumb} if crumb else {}

    for rtype in ["projectRoles", "globalRoles"]:
        url = f"{JENKINS_URL}/role-strategy/strategy/unassignUser"
        payload = {"type": rtype, "sid": username}
        try:
            requests.post(url, data=payload, headers=headers, auth=(ADMIN_USER, ADMIN_TOKEN))
        except Exception:
            pass

# ==========================
# DELETE USER
# ==========================

def delete_user(username):
    crumb_field, crumb = get_crumb()
    headers = {crumb_field: crumb} if crumb else {}

    # Unassign roles first
    unassign_roles(username)

    # Try standard endpoints for user deletion
    endpoints = [
        f"{JENKINS_URL}/user/{username}/doDelete",
        f"{JENKINS_URL}/securityRealm/user/{username}/doDelete"
    ]

    for url in endpoints:
        try:
            res = requests.post(url, headers=headers, auth=(ADMIN_USER, ADMIN_TOKEN))
            if res.status_code in [200, 302]:
                logging.info(f"[DELETED] {username}")
                return True
        except Exception as e:
            logging.warning(f"Error calling {url}: {e}")

    logging.error(f"[FAILED DELETE] {username}")
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

    requests.post(url, data=payload, headers=headers, auth=(ADMIN_USER, ADMIN_TOKEN))
    logging.info(f"[ROLE] {role} → {username}")

# ==========================
# BULK / SINGLE CREATE MODE
# ==========================

def bulk_mode():
    results = []
    created_lines = []

    try:
        with open(CSV_PATH) as f:
            reader = csv.DictReader(f)

            for row in reader:
                username = row["username"].strip()
                email = row["email"].strip()
                roles = [r.strip() for r in row["roles"].split(",")]

                password = generate_password()

                # CHECK USER
                if user_exists(username):
                    logging.info(f"{username} user already exists")
                    results.append({
                        "username": username,
                        "email": email,
                        "roles": roles,
                        "status": "skipped",
                        "reason": "already exists"
                    })
                    continue

                # CREATE USER
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

                # ASSIGN ROLES
                for role in roles:
                    assign_role(username, role)

                # SAVE RESULT
                results.append({
                    "username": username,
                    "email": email,
                    "password": password,
                    "roles": roles,
                    "status": "created"
                })
                roles_str = ",".join(roles)
                created_lines.append(f"{username}|{email}|{password}|{roles_str}")

        with open(RESULTS_FILE, 'w') as f:
            json.dump(results, f, indent=2)

        with open('created_users.txt', 'w') as f:
            f.write("\n".join(created_lines))

        logging.info(f"Bulk completed — results saved to {RESULTS_FILE}")

    except Exception as e:
        logging.error(f"Bulk failed: {e}")
        sys.exit(1)

# ==========================
# DELETE MODE
# ==========================

def delete_mode():
    deleted_users = []

    try:
        if USERNAME:
            # Delete single user specified via USERNAME env var
            if not user_exists(USERNAME):
                logging.info(f"{USERNAME} user does not exist")
            else:
                if delete_user(USERNAME):
                    deleted_users.append(USERNAME)
        else:
            # Delete users listed in CSV
            with open(CSV_PATH) as f:
                reader = csv.DictReader(f)
                for row in reader:
                    u = row["username"].strip()
                    if not user_exists(u):
                        logging.info(f"{u} user does not exist")
                        continue
                    if delete_user(u):
                        deleted_users.append(u)

        with open('deleted_users.txt', 'w') as f:
            f.write("\n".join(deleted_users))

        logging.info(f"Delete completed — removed users: {deleted_users}")

    except Exception as e:
        logging.error(f"Delete failed: {e}")
        sys.exit(1)

# ==========================
# MAIN
# ==========================

if __name__ == "__main__":
    if MODE in ["delete", "remove"]:
        delete_mode()
    elif MODE in ["bulk", "single"]:
        bulk_mode()
    else:
        logging.error(f"Unknown MODE: {MODE}")
        sys.exit(1)
