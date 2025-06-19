CREATE DATABASE coworking;

CREATE TABLE _option (
    id SERIAL PRIMARY KEY,
    code VARCHAR(10) UNIQUE NOT NULL,
    libelle VARCHAR(100) NOT NULL,
    prix DECIMAL(10,2) CHECK (prix >= 0) NOT NULL
);

CREATE TABLE espace (
    id SERIAL PRIMARY KEY,
    libelle VARCHAR(100) UNIQUE NOT NULL,
    prix DECIMAL(10,2) CHECK (prix >= 0) NOT NULL
);

CREATE TABLE client (
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    telephone VARCHAR(20) UNIQUE NOT NULL
);
 
CREATE TABLE reservation (
    id SERIAL PRIMARY KEY,
    ref VARCHAR(100) UNIQUE NOT NULL,
    id_espace INT NOT NULL,
    id_client INT NOT NULL,
    date_reservation DATE NOT NULL,
    heure_debut TIME NOT NULL CHECK (heure_debut BETWEEN '08:00' AND '18:00'), 
    duree INT NOT NULL CHECK (duree BETWEEN 1 AND 4), 
    FOREIGN KEY (id_espace) REFERENCES espace(id) ON DELETE CASCADE,
    FOREIGN KEY (id_client) REFERENCES client(id) ON DELETE CASCADE
);

CREATE TABLE reservation_option(
    id SERIAL PRIMARY KEY,
    id_reservation INT NOT NULL,
    id_option INT NOT NULL,
    FOREIGN KEY (id_reservation) REFERENCES reservation(id) ON DELETE CASCADE,
    FOREIGN KEY (id_option) REFERENCES _option (id) ON DELETE CASCADE
);

CREATE TABLE payement (
    id SERIAL PRIMARY KEY,
    id_reservation INT NOT NULL,
    ref VARCHAR(100) UNIQUE NOT NULL,
    date_payement DATE DEFAULT NULL,
    FOREIGN KEY (id_reservation) REFERENCES reservation(id) ON DELETE CASCADE
);

CREATE TABLE admin(
    id SERIAL PRIMARY KEY,
    email VARCHAR(250) NOT NULL,
    motdepasse VARCHAR(250)
);