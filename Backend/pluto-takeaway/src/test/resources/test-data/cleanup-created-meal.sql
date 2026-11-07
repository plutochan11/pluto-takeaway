DELETE o
FROM meal_option o
JOIN meal_option_group g ON g.id = o.group_id
JOIN meal m ON m.id = g.meal_id
WHERE m.shop_id = 1
    AND m.name = 'Create Meal Integration Fixture';

DELETE g
FROM meal_option_group g
JOIN meal m ON m.id = g.meal_id
WHERE m.shop_id = 1
       AND m.name = 'Create Meal Integration Fixture';

DELETE FROM meal
WHERE shop_id = 1
    AND name = 'Create Meal Integration Fixture';
