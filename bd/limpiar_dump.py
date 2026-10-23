import re
import sys

entrada, salida = sys.argv[1], sys.argv[2]

with open(entrada, "r", encoding="utf-8") as f:
    texto = f.read()

# Quita todas las líneas de comentario condicional de mysqldump
# (/*!...*/;) que no aportan nada a una importación simple: SET de
# sesión (charset, GTID, SQL_MODE, UNIQUE_CHECKS...) y los pares
# saved_cs_client/restore alrededor de cada CREATE TABLE.
lineas = texto.splitlines()
limpias = [l for l in lineas if not re.match(r"^\s*/\*!.*\*/;\s*$", l)]
texto = "\n".join(limpias)

# CREATE DATABASE con comentarios de versión -> forma simple.
texto = re.sub(
    r"CREATE DATABASE /\*!32312 IF NOT EXISTS\*/ `alquiler` "
    r"/\*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci \*/ "
    r"/\*!80016 DEFAULT ENCRYPTION='N' \*/;",
    "CREATE DATABASE IF NOT EXISTS `alquiler` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;",
    texto,
)

# Colapsa líneas en blanco repetidas que quedaron tras quitar comentarios.
texto = re.sub(r"\n{3,}", "\n\n", texto).strip("\n") + "\n"

cuerpo = texto.split("CREATE DATABASE", 1)[1]
cuerpo = "CREATE DATABASE" + cuerpo

final = "SET NAMES utf8mb4;\nSET FOREIGN_KEY_CHECKS=0;\n\n" + cuerpo.strip("\n") + "\n\nSET FOREIGN_KEY_CHECKS=1;\n"

with open(salida, "w", encoding="utf-8") as f:
    f.write(final)
