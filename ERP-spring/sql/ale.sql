CREATE TABLE update_base_assignment_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    update_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    salary_component VARCHAR(255) NOT NULL,
    old_base DOUBLE NOT NULL,
    new_base DOUBLE NOT NULL,
    employee_name VARCHAR(255) NOT NULL,
    period VARCHAR(50) NOT NULL,
    structure_name VARCHAR(255) NOT NULL,
    adjustment_type VARCHAR(50) NOT NULL,
    adjustment_percentage DOUBLE NOT NULL
);