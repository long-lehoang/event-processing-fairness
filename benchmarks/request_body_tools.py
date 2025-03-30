import csv
import random

def generate_api_request_bodies_csv(output_file="api_request_bodies.csv"):
    # Users cycle for subscriber IDs (sub-1 -> user-1, sub-2 -> user-2, sub-3 -> user-3, etc.)
    users = ['user-1', 'user-2', 'user-3']

    # Open CSV file to write
    with open(output_file, 'w', newline='') as f:
        writer = csv.DictWriter(f, fieldnames=["event_id", "event_type", "account_id"])
        writer.writeheader()  # Write column headers

        # Generate 896 request bodies
        for i in range(1, 897):  # 1 to 897 inclusive
            event_id = f"sce-{i}"
            event_type = "subscriber.created"
            # Random subscriber ID from 1 to 50
            sub_id_num = random.randint(1, 50)
            # Map subscriber ID to account_id (created_by): sub-1 -> user-1, sub-2 -> user-2, etc.
            account_id = users[(sub_id_num - 1) % 3]

            # Write row to CSV
            writer.writerow({
                "event_id": event_id,
                "event_type": event_type,
                "account_id": account_id
            })

    print(f"Generated 896 API request bodies and saved to {output_file}")

# Run the function
if __name__ == "__main__":
    generate_api_request_bodies_csv()