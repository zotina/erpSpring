WITH eligible_parents AS (
    SELECT DISTINCT parent
    FROM `tabSalary Detail`
    WHERE salary_component = 'Salaire Base'
        AND amount > 1300000
        AND parenttype = 'Salary Slip'
        
),
base_salaries AS (
    SELECT
        sd.amount,
        sd.parent
    FROM `tabSalary Detail` sd
    INNER JOIN eligible_parents ep ON sd.parent = ep.parent
    WHERE sd.salary_component = 'Salaire Base'
)
SELECT
    p.amount,
    p.parent,
    sl.salary_structure
FROM base_salaries p
INNER JOIN `tabSalary Slip` sl ON sl.name = p.parent
where sl.posting_date >= "2025-04-01" and sl.posting_date <= "2025-11-01"

            -- posting_date >= date_min and posting_date <= date_max