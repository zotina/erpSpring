--client 
insert into client (nom,telephone) values('zaka','0349049881');
insert into client (nom,telephone) values('be','0381034567');
--espace
INSERT INTO espace (libelle, prix) VALUES ('rubis', 1500000);
INSERT INTO espace (libelle, prix) VALUES ('or', 1500000);

--admin
INSERT INTO admin (email, motdepasse) 
VALUES ('admin@example.com', 'admin1234');

INSERT INTO admin (email, motdepasse)
VALUES 
('admin@gmail.com', encode(digest('admin123', 'sha256'), 'hex'));

