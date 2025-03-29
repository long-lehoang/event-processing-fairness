
-- public."event" definition

-- DROP TABLE IF EXISTS

 DROP TABLE IF EXISTS public."event";

CREATE TABLE public."event" (
	id varchar(255) NOT NULL,
	created_at timestamptz(6) NULL,
	created_by varchar(255) NULL,
	event_name varchar(255) NULL,
	CONSTRAINT event_pkey PRIMARY KEY (id),
	CONSTRAINT ukpv13n3n7ggmpvk1rsfqi7r15l UNIQUE (event_name)
);


-- public.segment definition

-- DROP TABLE IF EXISTS

 DROP TABLE IF EXISTS public.segment;

CREATE TABLE public.segment (
	id varchar(255) NOT NULL,
	created_at timestamptz(6) NULL,
	created_by varchar(255) NULL,
	"name" varchar(255) NULL,
	CONSTRAINT segment_pkey PRIMARY KEY (id)
);


-- public.subscriber definition

-- DROP TABLE IF EXISTS

 DROP TABLE IF EXISTS public.subscriber;

CREATE TABLE public.subscriber (
	id varchar(255) NOT NULL,
	created_at timestamptz(6) NULL,
	created_by varchar(255) NULL,
	custom_fields varchar(255) NULL,
	email varchar(255) NOT NULL,
	first_name varchar(255) NULL,
	last_name varchar(255) NULL,
	optin_ip varchar(255) NULL,
	optin_timestamp timestamptz(6) NULL,
	"source" varchar(255) NULL,
	status varchar(255) NULL,
	CONSTRAINT subscriber_pkey PRIMARY KEY (id),
	CONSTRAINT ukewxd6mo4yfd3pmkcfoaec1sun UNIQUE (email)
);


-- public.subscriber_created_event definition

-- DROP TABLE IF EXISTS

 DROP TABLE IF EXISTS public.subscriber_created_event;

CREATE TABLE public.subscriber_created_event (
	id varchar(255) NOT NULL,
	event_name varchar(255) NULL,
	event_time timestamptz(6) NULL,
	subscriber_id varchar(255) NULL,
	webhook_id varchar(255) NULL,
	CONSTRAINT subscriber_created_event_pkey PRIMARY KEY (id)
);


-- public.subscriber_segment definition

-- DROP TABLE IF EXISTS

 DROP TABLE IF EXISTS public.subscriber_segment;

CREATE TABLE public.subscriber_segment (
	id varchar(255) NOT NULL,
	created_at timestamptz(6) NULL,
	created_by varchar(255) NULL,
	segment_id varchar(255) NULL,
	subscriber_id varchar(255) NULL,
	CONSTRAINT subscriber_segment_pkey PRIMARY KEY (id)
);


-- public.webhook definition

-- DROP TABLE IF EXISTS

 DROP TABLE IF EXISTS public.webhook;

CREATE TABLE public.webhook (
	id varchar(255) NOT NULL,
	created_at timestamptz(6) NULL,
	created_by varchar(255) NULL,
	"name" varchar(255) NULL,
	post_url varchar(255) NULL,
	CONSTRAINT webhook_pkey PRIMARY KEY (id)
);


-- public.webhook_event definition

-- DROP TABLE IF EXISTS

 DROP TABLE IF EXISTS public.webhook_event;

CREATE TABLE public.webhook_event (
	id varchar(255) NOT NULL,
	created_at timestamptz(6) NULL,
	created_by varchar(255) NULL,
	event_id varchar(255) NULL,
	webhook_id varchar(255) NULL,
	CONSTRAINT webhook_event_pkey PRIMARY KEY (id)
);
