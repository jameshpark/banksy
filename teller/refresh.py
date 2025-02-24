#!/usr/bin/env python3
import base64
import glob
import json
import socketserver
import subprocess
import threading
from http.server import SimpleHTTPRequestHandler

PORT = 8000


def get_enrollment_data(file_name):
    try:
        # Read enrollment data from enrollment.json
        with open(file_name, "r") as f:
            enrollment_data = json.load(f)
            if not enrollment_data.get("accessToken"):
                raise ValueError("Access token not found in enrollment.json")
            if not enrollment_data.get("enrollment") or not enrollment_data["enrollment"].get("id"):
                raise ValueError("Enrollment ID not found in enrollment.json")
            return enrollment_data
    except FileNotFoundError:
        print("Error: enrollment.json file not found. Please ensure the file exists.")
        raise
    except Exception as e:
        print(f"Error retrieving enrollment data: {e}")
        raise


def inject_enrollment_id(enrollment_id):
    try:
        # Read index.html
        with open("index.html", "r") as f:
            html_content = f.readlines()

        # Inject the enrollmentId into the content
        target_line = "environment: ENVIRONMENT,"
        inserted = False
        updated_content = ""
        for line in html_content:
            updated_content += line
            if target_line in line and not inserted:
                updated_content += f"            enrollmentId: '{enrollment_id}',\n"
                inserted = True

        if not inserted:
            raise ValueError(f"Failed to find the target line '{target_line}' in index.html")

        print("Enrollment ID injected successfully.")
        return updated_content
    except Exception as e:
        print(f"Error injecting enrollment ID: {e}")
        raise


def verify_access_token(access_token):
    # Run a curl command to verify the access token's validity
    curl_command = [
        "curl",
        "--cert", "../src/main/resources/secrets/certificate.pem",
        "--key", "../src/main/resources/secrets/private_key.pem",
        "-u", f"{access_token}:",
        "https://api.teller.io/accounts"
    ]
    try:
        result = subprocess.run(curl_command, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
        if result.returncode == 0 and "error" not in result.stdout.lower():
            print("Access token is valid.")
            return True
        else:
            print("Access token is invalid or expired.")
            print(f"Curl output: {result.stdout}")
            return False
    except Exception as e:
        print(f"Error verifying access token: {e}")
        return False


def serve_index_and_wait_for_post(index_content, enrollment_file):
    class CustomHandler(SimpleHTTPRequestHandler):
        def do_GET(self):
            # Serve index.html content from memory
            if self.path == "/index.html":
                self.send_response(200)
                self.send_header("Content-type", "text/html")
                self.end_headers()
                self.wfile.write(index_content.encode())
            else:
                self.send_error(404, "File not found")

        def do_POST(self):
            if self.path == "/save-enrollment":
                content_length = int(self.headers["Content-Length"])
                post_data = self.rfile.read(content_length).decode("utf-8")
                enrollment = json.loads(post_data)

                # Update enrollment.json with new enrollment data
                with open(enrollment_file, "w") as f:
                    json.dump(enrollment, f, indent=2)
                print(f"Updated enrollment data saved to {enrollment_file}.")

                # Fetch the updated access token and validate accounts
                access_token = enrollment.get("accessToken")
                if not access_token:
                    self.send_error(400, "Missing accessToken in request")
                    return

                print("Access token received:", access_token)
                teller_api_host = "api.teller.io"
                teller_api_path = "/accounts"

                try:
                    # Fetch accounts data from Teller API
                    auth_header = base64.b64encode(f"{access_token}:".encode()).decode("utf-8")
                    headers = {"Authorization": f"Basic {auth_header}"}

                    # Set up HTTPS connection with cert and key files
                    import ssl
                    import http.client
                    ssl_context = ssl.SSLContext(ssl.PROTOCOL_TLS_CLIENT)
                    ssl_context.load_cert_chain(
                        certfile="../src/main/resources/secrets/certificate.pem",
                        keyfile="../src/main/resources/secrets/private_key.pem"
                    )
                    ssl_context.load_default_certs()

                    conn = http.client.HTTPSConnection(teller_api_host, context=ssl_context)
                    conn.request("GET", teller_api_path, headers=headers)
                    response = conn.getresponse()
                    if response.status != 200:
                        raise Exception(f"Error response from Teller API: {response.status} {response.reason}")

                    accounts_data = json.loads(response.read().decode())
                    print("Accounts data fetched successfully.")

                    # Update enrollment.json with accounts data
                    enrollment["accounts"] = accounts_data
                    with open(enrollment_file, "w") as f:
                        json.dump(enrollment, f, indent=2)
                    print(f"Accounts data added to {enrollment_file}.")

                    # Send success response
                    self.send_response(200)
                    self.send_header("Content-type", "application/json")
                    self.end_headers()
                    self.wfile.write(json.dumps({"status": "success", "data": accounts_data}).encode())

                except Exception as e:
                    print(f"Error fetching accounts data: {e}")
                    self.send_error(500, "Failed to fetch accounts data")
                finally:
                    if conn:
                        conn.close()
                    # Shut down the server after handling the POST
                    print("Shutting down the server...")
                    threading.Thread(target=self.server.shutdown).start()

                return
            else:
                self.send_error(404, "Endpoint not found")

        def log_message(self, format, *args):
            return  # Suppress server logs

    # Serve the HTML content
    with socketserver.TCPServer(("", PORT), CustomHandler) as server:
        print(f"Serving index.html content on http://localhost:{PORT}/index.html")
        server_thread = threading.Thread(target=server.serve_forever)
        server_thread.daemon = True
        server_thread.start()
        server_thread.join()


def main():
    enrollment_files = glob.glob("enrollment*.json")

    if not enrollment_files:
        print("No enrollment JSON files found (enrollment*.json). Exiting.")
        return

    for enrollment_file in enrollment_files:
        print(f"Refreshing {enrollment_file}...")

        # Load enrollment data
        try:
            enrollment_data = get_enrollment_data(enrollment_file)
            access_token = enrollment_data["accessToken"]
            enrollment_id = enrollment_data["enrollment"]["id"]
        except Exception as e:
            print(f"Failed to load enrollment data from {enrollment_file}: {e}. Moving on to next file.")
            continue

        # Step 1: Verify the access token
        if verify_access_token(access_token):
            print(f"Access token in {enrollment_file} is valid. No further action required.")
            continue

        # Step 2: Inject enrollment ID and serve index.html
        try:
            index_content = inject_enrollment_id(enrollment_id)
            serve_index_and_wait_for_post(index_content, enrollment_file)
        except Exception as e:
            print(f"Error during workflow: {e}")


if __name__ == "__main__":
    main()
