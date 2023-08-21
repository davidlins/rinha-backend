CREATE EXTENSION btree_gist;

DROP TABLE IF EXISTS pessoas;

CREATE TABLE pessoas (
	id VARCHAR(40) PRIMARY KEY,
	apelido VARCHAR(32) NOT NULL,
	nome VARCHAR(100) NOT NULL,
	nascimento DATE NOT NULL,
	stack TEXT NULL,
	text_searchable TEXT NOT NULL,
	UNIQUE(apelido)
);


CREATE INDEX text_searchable_idx ON pessoas USING GIN (text_searchable);



