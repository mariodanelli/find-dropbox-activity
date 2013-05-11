-- DROP Section

DROP TABLE activities;

DROP TABLE users;

-- CREATE Section

CREATE TABLE users
(
  "id_users" integer NOT NULL,
  "user_email" character varying(100) NOT NULL,
  "token_key" character varying(20),
  "token_secret" character varying(20),
  "cursor_string" character varying(100),
  CONSTRAINT pk_users PRIMARY KEY (id_users)
)
WITH (OIDS=FALSE);
ALTER TABLE users OWNER TO postgres;

CREATE TABLE activities
(
  "id_users" integer NOT NULL,
  "id_activities" integer NOT NULL,
  "lc_path" character varying(300),
  "name" character varying(100),
  "rev" character varying(20),
  "doc_size" character varying(30),
  "doc_modified" character varying(50),
  "doc_type" character varying(1),
  "fl_delete" boolean DEFAULT false,
  "cursor_string" character varying(100),
  "timestamp" timestamp with time zone NOT NULL,
  CONSTRAINT pk_activities PRIMARY KEY ("id_users", "id_activities"),
  CONSTRAINT fk_activities_1 FOREIGN KEY ("id_users")
      REFERENCES users ("id_users") MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT type_constraint CHECK ("doc_type"::text = 'F'::text OR "doc_type"::text = 'D'::text)
)
WITH (OIDS=FALSE);
ALTER TABLE activities OWNER TO postgres;

-- INSERT Section

INSERT INTO users VALUES (1, 'test1@testmail.com', null, null, null);
INSERT INTO users VALUES (2, 'test2@testmail.com', null, null, null);
INSERT INTO users VALUES (3, 'test3@testmail.com', null, null, null);
