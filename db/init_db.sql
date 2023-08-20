CREATE EXTENSION btree_gist;

DROP TABLE IF EXISTS pessoas;

CREATE TABLE pessoas (
	id uuid DEFAULT gen_random_uuid (),
	apelido VARCHAR(32) NOT NULL,
	nome VARCHAR(100) NOT NULL,
	nascimento DATE NOT NULL,
	stack TEXT NULL,
	text_searchable TEXT GENERATED ALWAYS AS (  
	    lower(apelido || ' ' || nome || ' ' || COALESCE(stack, ' '))
	) STORED,
	PRIMARY KEY (id),
	UNIQUE(apelido)
);


CREATE INDEX text_searchable_idx ON pessoas USING GIST (text_searchable);

