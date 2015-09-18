--
-- Name: provision_id_seq; Type: SEQUENCE; Schema: public; Owner: seqware
--

CREATE SEQUENCE provision_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;

--
-- Name: provision; Type: TABLE; Schema: public; Owner: seqware; Tablespace:
--

CREATE TABLE provision (
    provision_id integer DEFAULT nextval('provision_id_seq'::regclass) NOT NULL,
    status text,
    provision_uuid text,
    job_uuid text,
    ip_address text,
    cores integer,
    mem_gb integer,
    storage_gb integer,
    update_timestamp timestamp default current_timestamp,
    create_timestamp timestamp default current_timestamp
);

--
-- Name: job_id_seq; Type: SEQUENCE; Schema: public; Owner: seqware
--

CREATE SEQUENCE job_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;

--
-- Name: provision; Type: TABLE; Schema: public; Owner: seqware; Tablespace:
--

CREATE TABLE job (
    job_id integer DEFAULT nextval('job_id_seq'::regclass) NOT NULL,
    status text,
    job_uuid text,
    provision_uuid text,
    workflow text,
    workflow_version text,
    job_hash text,
    ini text,
    end_user text, 
    flavour text,
    stdout text,
    stderr text,
    update_timestamp timestamp default current_timestamp,
    create_timestamp timestamp default current_timestamp
);

