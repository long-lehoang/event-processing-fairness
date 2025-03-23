INSERT INTO event (id, event_name) VALUES
('evt-1', 'User Registered'),
('evt-2', 'Order Placed'),
('evt-3', 'Password Reset');

INSERT INTO segment (id, name) VALUES
('seg-1', 'Premium Users'),
('seg-2', 'Trial Users'),
('seg-3', 'Inactive Users');

INSERT INTO subscriber (id, email, first_name, last_name, status, source, optin_ip, optin_timestamp, custome_fields) VALUES
('sub-1', 'john.doe@example.com', 'John', 'Doe', 'active', 'website', '192.168.1.1', '2025-03-01 10:15:00', '{}'),
('sub-2', 'jane.smith@example.com', 'Jane', 'Smith', 'active', 'mobile', '192.168.1.2', '2025-03-02 11:00:00', '{}'),
('sub-3', 'bob.jones@example.com', 'Bob', 'Jones', 'inactive', 'referral', '192.168.1.3', '2025-03-03 12:30:00', '{}');

INSERT INTO subscriber_segment (id, segment_id, subscriber_id) VALUES
('ss-1', 'seg-1', 'sub-1'),
('ss-2', 'seg-2', 'sub-2'),
('ss-3', 'seg-3', 'sub-3');

INSERT INTO subscriber_created_event (id, event_name, event_time, subscriber_id, webhook_id) VALUES
('sce-1', 'User Registered', '2025-03-05 08:00:00', 'sub-1', 'wh-1'),
('sce-2', 'User Registered', '2025-03-06 09:15:00', 'sub-2', 'wh-2'),
('sce-3', 'User Registered', '2025-03-07 10:30:00', 'sub-3', 'wh-3');

INSERT INTO webhook (id, name, post_url) VALUES
('wh-1', 'User Registration Webhook', 'https://api.example.com/webhook1'),
('wh-2', 'Order Processing Webhook', 'https://api.example.com/webhook2'),
('wh-3', 'Password Reset Webhook', 'https://api.example.com/webhook3');

INSERT INTO webhook_event (id, event_id, webhook_id) VALUES
('we-1', 'evt-1', 'wh-1'),
('we-2', 'evt-2', 'wh-2'),
('we-3', 'evt-3', 'wh-3');