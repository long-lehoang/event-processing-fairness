import random
from datetime import datetime, timedelta

def generate_sql_batch_inserts(output_file="test_data.sql"):
    # Open file to write SQL statements
    with open(output_file, 'w') as f:
        # Helper lists
        users = ['user-1', 'user-2', 'user-3']
        base_date = datetime(2024, 1, 1)

        # 1. event Table (3 records)
        f.write("-- Batch Insert into event table\n")
        events = [
            ('event-1', base_date + timedelta(days=1), 'user-1', 'subscriber.created'),
            ('event-2', base_date + timedelta(days=2), 'user-2', 'subscriber.added_to_segment'),
            ('event-3', base_date + timedelta(days=3), 'user-3', 'subscriber.unsubscribed')
        ]
        f.write("INSERT INTO public.\"event\" (id, created_at, created_by, event_name) VALUES\n")
        f.write(",\n".join(
            f"('{eid}', '{created_at.strftime('%Y-%m-%d %H:%M:%S+00')}', '{euser}', '{ename}')"
            for eid, created_at, euser, ename in events
        ) + ";\n\n")

        # 2. segment Table (10 records)
        f.write("-- Batch Insert into segment table\n")
        segment_names = ['New Users', 'VIP Customers', 'Inactive Users', 'Trial Users', 'Engaged Leads',
                         'Loyal Subscribers', 'Promo Targets', 'Newsletter Subs', 'Free Tier', 'Beta Testers']
        segments = [
            (f'seg-{i}', base_date + timedelta(days=i * 30), users[(i-1) % 3], segment_names[i-1])
            for i in range(1, 11)
        ]
        f.write("INSERT INTO public.segment (id, created_at, created_by, \"name\") VALUES\n")
        f.write(",\n".join(
            f"('{sid}', '{created_at.strftime('%Y-%m-%d %H:%M:%S+00')}', '{suser}', '{sname}')"
            for sid, created_at, suser, sname in segments
        ) + ";\n\n")

        # 3. subscriber Table (50 records)
        f.write("-- Batch Insert into subscriber table\n")
        first_names = ['John', 'Jane', 'Mike', 'Sara', 'Tom', 'Emma', 'Alex', 'Lisa', 'Chris', 'Anna']
        last_names = ['Doe', 'Smith', 'Jones', 'Lee', 'Brown', 'Wilson', 'Davis', 'Clark', 'Taylor', 'Moore']
        subscribers = []
        for i in range(1, 51):
            fname = random.choice(first_names)
            lname = random.choice(last_names)
            email = f"{fname.lower()}.{lname.lower()}{i}@example.com"
            created_at = base_date + timedelta(days=random.randint(0, 450))
            optin_time = created_at + timedelta(minutes=5)
            created_by = users[(i-1) % 3]
            ip = f"192.168.1.{i}"
            custom_fields = f'{{"age": {random.randint(20, 40)}}}'
            status = random.choice(['active', 'inactive'])
            subscribers.append((f'sub-{i}', created_at, created_by, custom_fields, email, fname, lname,
                                ip, optin_time, 'website', status))
        f.write("INSERT INTO public.subscriber (id, created_at, created_by, custom_fields, email, first_name, last_name, "
                "optin_ip, optin_timestamp, \"source\", status) VALUES\n")
        f.write(",\n".join(
            f"('{sid}', '{created_at.strftime('%Y-%m-%d %H:%M:%S+00')}', '{suser}', '{cf}', '{email}', '{fname}', '{lname}', "
            f"'{ip}', '{optin_time.strftime('%Y-%m-%d %H:%M:%S+00')}', '{source}', '{status}')"
            for sid, created_at, suser, cf, email, fname, lname, ip, optin_time, source, status in subscribers
        ) + ";\n\n")

        # 4. webhook Table (6 records)
        f.write("-- Batch Insert into webhook table\n")
        webhooks = [
            ('wh-1', base_date + timedelta(hours=1), 'user-1', 'User1 Success Hook', 'http://host.docker.internal:8085/webhooks/success'),
            ('wh-2', base_date + timedelta(hours=2), 'user-1', 'User1 Success Hook', 'http://host.docker.internal:8085/webhooks/success'),
            ('wh-3', base_date + timedelta(hours=3), 'user-2', 'User2 Success Hook', 'http://host.docker.internal:8085/webhooks/success'),
            ('wh-4', base_date + timedelta(hours=4), 'user-2', 'User2 Success Hook', 'http://host.docker.internal:8085/webhooks/success'),
            ('wh-5', base_date + timedelta(hours=5), 'user-3', 'User3 Success Hook', 'http://host.docker.internal:8085/webhooks/success'),
            ('wh-6', base_date + timedelta(hours=6), 'user-3', 'User3 Success Hook', 'http://host.docker.internal:8085/webhooks/success')
        ]
        f.write("INSERT INTO public.webhook (id, created_at, created_by, \"name\", post_url) VALUES\n")
        f.write(",\n".join(
            f"('{wid}', '{created_at.strftime('%Y-%m-%d %H:%M:%S+00')}', '{wuser}', '{wname}', '{wurl}')"
            for wid, created_at, wuser, wname, wurl in webhooks
        ) + ";\n\n")

        # 5. subscriber_created_event Table (896 records)
        f.write("-- Batch Insert into subscriber_created_event table\n")
        subscriber_events = []
        for i in range(1, 10000):
            sub_id = f"sub-{random.randint(1, 50)}"
            wh_id = f"wh-{random.randint(1, 6)}"
            event_time = base_date + timedelta(days=random.randint(0, 450), hours=random.randint(0, 23))
            subscriber_events.append((f'sce-{i}', 'subscriber.created', event_time, sub_id, wh_id))
        f.write("INSERT INTO public.subscriber_created_event (id, event_name, event_time, subscriber_id, webhook_id) VALUES\n")
        f.write(",\n".join(
            f"('{seid}', '{ename}', '{etime.strftime('%Y-%m-%d %H:%M:%S+00')}', '{subid}', '{whid}')"
            for seid, ename, etime, subid, whid in subscriber_events
        ) + ";\n\n")

        # 6. subscriber_segment Table (20 records)
        f.write("-- Batch Insert into subscriber_segment table\n")
        subscriber_segments = []
        for i in range(1, 21):
            sub_id = f"sub-{random.randint(1, 50)}"
            seg_id = f"seg-{random.randint(1, 10)}"
            created_at = base_date + timedelta(days=random.randint(0, 450))
            created_by = users[(i-1) % 3]
            subscriber_segments.append((f'ss-{i}', created_at, created_by, seg_id, sub_id))
        f.write("INSERT INTO public.subscriber_segment (id, created_at, created_by, segment_id, subscriber_id) VALUES\n")
        f.write(",\n".join(
            f"('{ssid}', '{created_at.strftime('%Y-%m-%d %H:%M:%S+00')}', '{suser}', '{segid}', '{subid}')"
            for ssid, created_at, suser, segid, subid in subscriber_segments
        ) + ";\n\n")

        # 7. webhook_event Table (15 records)
        f.write("-- Batch Insert into webhook_event table\n")
        webhook_events = []
        for i in range(1, 16):
            event_id = f"event-{random.randint(1, 3)}"
            wh_id = f"wh-{random.randint(1, 6)}"
            created_at = base_date + timedelta(days=random.randint(0, 450), hours=random.randint(0, 23))
            created_by = random.choice(users)
            webhook_events.append((f'we-{i}', created_at, created_by, event_id, wh_id))
        f.write("INSERT INTO public.webhook_event (id, created_at, created_by, event_id, webhook_id) VALUES\n")
        f.write(",\n".join(
            f"('{weid}', '{created_at.strftime('%Y-%m-%d %H:%M:%S+00')}', '{weuser}', '{eid}', '{whid}')"
            for weid, created_at, weuser, eid, whid in webhook_events
        ) + ";\n")

    print(f"Batch SQL insert statements generated and saved to {output_file}")

# Run the function
if __name__ == "__main__":
    generate_sql_batch_inserts()