-- Десантник может находиться только на одном корабле.
-- Применяется один раз к схеме, созданной до этого правила.
ALTER TABLE starship_marine ADD CONSTRAINT uq_ship_marine_single UNIQUE (space_marine_id);
