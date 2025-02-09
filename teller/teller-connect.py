#!/usr/bin/env python3
import http.server
import socketserver
import json

PORT = 8000


class CustomHandler(http.server.SimpleHTTPRequestHandler):
    def do_POST(self):
        # Handle the POST request to save the enrollment object
        if self.path == "/save-enrollment":
            content_length = int(self.headers["Content-Length"])
            post_data = self.rfile.read(content_length)  # Read the POST data
            enrollment = json.loads(post_data)  # Parse the POST data as JSON

            # Save the enrollment object to a file
            with open("enrollment.json", "w") as f:
                json.dump(enrollment, f, indent=2)

            # Send a success response
            self.send_response(200)
            self.send_header("Content-type", "application/json")
            self.end_headers()
            self.wfile.write(json.dumps({"status": "success"}).encode())
            print("Enrollment saved to enrollment.json")

            # Gracefully shut down the server
            print("Shutting down the server...")
            self.server.shutdown()
        else:
            # Handle other POST requests or 404
            self.send_error(404, "Endpoint not found")

    def do_GET(self):
        # Serve static files by default
        super().do_GET()


def main():
    with socketserver.TCPServer(("", PORT), CustomHandler) as httpd:
        print(f"Serving static directory and POST requests on http://localhost:{PORT}")
        httpd.serve_forever()


if __name__ == '__main__':
    main()
