--
-- PostgreSQL database dump
--

SET statement_timeout = 0;
SET lock_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

--
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


SET search_path = public, pg_catalog;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: consonance_user; Type: TABLE; Schema: public; Owner: queue_user; Tablespace: 
--

CREATE TABLE consonance_user (
    user_id integer NOT NULL,
    create_timestamp timestamp without time zone,
    update_timestamp timestamp without time zone,
    admin boolean NOT NULL,
    hashed_password character varying(255) NOT NULL,
    name character varying(255) NOT NULL
);


ALTER TABLE consonance_user OWNER TO queue_user;

--
-- Name: consonance_user_user_id_seq; Type: SEQUENCE; Schema: public; Owner: queue_user
--

CREATE SEQUENCE consonance_user_user_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE consonance_user_user_id_seq OWNER TO queue_user;

--
-- Name: consonance_user_user_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: queue_user
--

ALTER SEQUENCE consonance_user_user_id_seq OWNED BY consonance_user.user_id;


--
-- Name: extra_files; Type: TABLE; Schema: public; Owner: queue_user; Tablespace: 
--

CREATE TABLE extra_files (
    job_id integer NOT NULL,
    content text,
    keep boolean,
    path text NOT NULL
);


ALTER TABLE extra_files OWNER TO queue_user;

--
-- Name: job; Type: TABLE; Schema: public; Owner: queue_user; Tablespace: 
--

CREATE TABLE job (
    job_id integer NOT NULL,
    create_timestamp timestamp without time zone,
    update_timestamp timestamp without time zone,
    container_image_descriptor text,
    container_image_descriptor_type text,
    container_runtime_descriptor text,
    end_user text,
    flavour text,
    job_hash text,
    message_type text,
    status text,
    stderr text,
    stdout text,
    job_uuid text,
    provision_uuid text
);


ALTER TABLE job OWNER TO queue_user;

--
-- Name: job_job_id_seq; Type: SEQUENCE; Schema: public; Owner: queue_user
--

CREATE SEQUENCE job_job_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE job_job_id_seq OWNER TO queue_user;

--
-- Name: job_job_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: queue_user
--

ALTER SEQUENCE job_job_id_seq OWNED BY job.job_id;


--
-- Name: provision; Type: TABLE; Schema: public; Owner: queue_user; Tablespace: 
--

CREATE TABLE provision (
    provision_id integer NOT NULL,
    create_timestamp timestamp without time zone,
    update_timestamp timestamp without time zone,
    cores integer,
    ip_address text,
    job_uuid text,
    mem_gb integer,
    provision_uuid text,
    status text,
    storage_gb integer
);


ALTER TABLE provision OWNER TO queue_user;

--
-- Name: provision_ansibleplaybooks; Type: TABLE; Schema: public; Owner: queue_user; Tablespace: 
--

CREATE TABLE provision_ansibleplaybooks (
    provision_provision_id integer NOT NULL,
    ansibleplaybooks character varying(255)
);


ALTER TABLE provision_ansibleplaybooks OWNER TO queue_user;

--
-- Name: provision_provision_id_seq; Type: SEQUENCE; Schema: public; Owner: queue_user
--

CREATE SEQUENCE provision_provision_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE provision_provision_id_seq OWNER TO queue_user;

--
-- Name: provision_provision_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: queue_user
--

ALTER SEQUENCE provision_provision_id_seq OWNED BY provision.provision_id;


--
-- Name: user_id; Type: DEFAULT; Schema: public; Owner: queue_user
--

ALTER TABLE ONLY consonance_user ALTER COLUMN user_id SET DEFAULT nextval('consonance_user_user_id_seq'::regclass);


--
-- Name: job_id; Type: DEFAULT; Schema: public; Owner: queue_user
--

ALTER TABLE ONLY job ALTER COLUMN job_id SET DEFAULT nextval('job_job_id_seq'::regclass);


--
-- Name: provision_id; Type: DEFAULT; Schema: public; Owner: queue_user
--

ALTER TABLE ONLY provision ALTER COLUMN provision_id SET DEFAULT nextval('provision_provision_id_seq'::regclass);


--
-- Name: consonance_user_pkey; Type: CONSTRAINT; Schema: public; Owner: queue_user; Tablespace: 
--

ALTER TABLE ONLY consonance_user
    ADD CONSTRAINT consonance_user_pkey PRIMARY KEY (user_id);


--
-- Name: extra_files_pkey; Type: CONSTRAINT; Schema: public; Owner: queue_user; Tablespace: 
--

ALTER TABLE ONLY extra_files
    ADD CONSTRAINT extra_files_pkey PRIMARY KEY (job_id, path);


--
-- Name: job_pkey; Type: CONSTRAINT; Schema: public; Owner: queue_user; Tablespace: 
--

ALTER TABLE ONLY job
    ADD CONSTRAINT job_pkey PRIMARY KEY (job_id);


--
-- Name: provision_pkey; Type: CONSTRAINT; Schema: public; Owner: queue_user; Tablespace: 
--

ALTER TABLE ONLY provision
    ADD CONSTRAINT provision_pkey PRIMARY KEY (provision_id);


--
-- Name: uk_fi4s6tg0ng09pfbkhdus2n2h8; Type: CONSTRAINT; Schema: public; Owner: queue_user; Tablespace: 
--

ALTER TABLE ONLY consonance_user
    ADD CONSTRAINT uk_fi4s6tg0ng09pfbkhdus2n2h8 UNIQUE (name);


--
-- Name: uk_i6afhcugr97k0viwvj67wno0j; Type: CONSTRAINT; Schema: public; Owner: queue_user; Tablespace: 
--

ALTER TABLE ONLY consonance_user
    ADD CONSTRAINT uk_i6afhcugr97k0viwvj67wno0j UNIQUE (hashed_password);


--
-- Name: fk_5dtb4x4tewkcfpjw58v68opmb; Type: FK CONSTRAINT; Schema: public; Owner: queue_user
--

ALTER TABLE ONLY provision_ansibleplaybooks
    ADD CONSTRAINT fk_5dtb4x4tewkcfpjw58v68opmb FOREIGN KEY (provision_provision_id) REFERENCES provision(provision_id);


--
-- Name: fk_d6ymmeamjm0wheh1jjwhoky6x; Type: FK CONSTRAINT; Schema: public; Owner: queue_user
--

ALTER TABLE ONLY extra_files
    ADD CONSTRAINT fk_d6ymmeamjm0wheh1jjwhoky6x FOREIGN KEY (job_id) REFERENCES job(job_id);


--
-- Name: public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM postgres;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO PUBLIC;


--
-- PostgreSQL database dump complete
--

