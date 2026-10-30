USE querypilot_db;
CREATE TABLE employees (
  id INT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  department VARCHAR(100) NOT NULL,
  salary DECIMAL(10,2) NOT NULL
);
INSERT INTO employees VALUES
  (1, 'Asha', 'Engineering', 65000.00),
  (2, 'Rohan', 'Support', 42000.00);
REVOKE ALL PRIVILEGES, GRANT OPTION FROM 'querypilot'@'%';
GRANT SELECT ON querypilot_db.* TO 'querypilot'@'%';
