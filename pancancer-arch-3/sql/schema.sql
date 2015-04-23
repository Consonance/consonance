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
    cores integer,
    mem_gb integer,
    storage_gb integer,
    timestamp timestamp default current_timestamp
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
    provision_uuid text,
    cores integer,
    mem_gb integer,
    storage_gb integer,
    timestamp timestamp default current_timestamp
);
