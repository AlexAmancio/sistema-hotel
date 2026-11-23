-- Migración para bases ya instaladas (defecto D13): el documento del cliente y
-- el nombre de usuario no pueden repetirse. El script bd/respaldo_bd_actual.sql
-- ya trae estas restricciones; esto es para una base creada antes del 23/11/2026.
--
-- Antes de ejecutarla, verificar que no haya repetidos:
--   SELECT identificacion, COUNT(*) FROM cliente GROUP BY identificacion HAVING COUNT(*) > 1;
--   SELECT username, COUNT(*) FROM usuario GROUP BY username HAVING COUNT(*) > 1;
--
-- Uso: mysql -u root alquiler < bd/migracion_d13_unicidad.sql
ALTER TABLE `cliente` ADD UNIQUE KEY `uk_cliente_identificacion` (`identificacion`);
ALTER TABLE `usuario` ADD UNIQUE KEY `uk_usuario_username` (`username`);
