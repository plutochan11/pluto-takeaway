DELETE o
FROM meal_option o
JOIN meal_option_group g ON g.id = o.group_id
JOIN meal m ON m.id = g.meal_id
WHERE m.shop_id = 2
  AND m.name = 'Public Meal Availability Fixture';

DELETE g
FROM meal_option_group g
JOIN meal m ON m.id = g.meal_id
WHERE m.shop_id = 2
  AND m.name = 'Public Meal Availability Fixture';

DELETE FROM meal
WHERE shop_id = 2
  AND name = 'Public Meal Availability Fixture';

UPDATE product SET status = 'AVAILABLE' WHERE shop_id = 2 AND id = 200;
