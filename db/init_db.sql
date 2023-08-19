
DROP TABLE IF EXISTS pessoas;


CREATE TABLE pessoas (
	id uuid DEFAULT gen_random_uuid (),
	apelido VARCHAR(32) NOT NULL,
	nome VARCHAR(100) NOT NULL,
	nascimento DATE NOT NULL,
	stack VARCHAR(1000) NULL,
	text_searchable VARCHAR(1500) GENERATED ALWAYS AS (lower(coalesce(apelido, '') || ' ' || coalesce(nome, '')|| ' ' || coalesce(stack, '') )) STORED,
	PRIMARY KEY (id),
	UNIQUE(apelido)
);


CREATE INDEX text_searchable_idx ON pessoas (text_searchable);

