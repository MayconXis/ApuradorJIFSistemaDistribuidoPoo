#!/bin/bash
# Cria seis chaves para a demonstração, para o painel do cluster mostrar chaves em todos os workers.
# Uso: BASE=http://<ip-do-nó> bash scripts/demo/criar-chaves-demo.sh   (padrão: http://localhost:4200)
set -euo pipefail
BASE=${BASE:-http://localhost:4200}
criar() {
    curl -fsS -X POST "$BASE/api/chaves" -H 'Content-Type: application/json' -d "$1" > /dev/null && echo "criada: $2"
}
criar '{"id":"demo-futsal-b","nome":"Futsal B","modalidade":"FUTSAL","equipes":["IFB","IFMT","IFTO"]}' "Futsal B"
criar '{"id":"demo-futsal-c","nome":"Futsal C","modalidade":"FUTSAL","equipes":["IFPA","IFAM","IFRR","IFAP"]}' "Futsal C"
criar '{"id":"demo-volei-a","nome":"Vôlei A","modalidade":"VOLEIBOL","equipes":["IFG","IFGoiano","IFMG"]}' "Vôlei A"
criar '{"id":"demo-volei-b","nome":"Vôlei B","modalidade":"VOLEIBOL","equipes":["IFSP","IFRJ","IFES","IFF"]}' "Vôlei B"
criar '{"id":"demo-hand-a","nome":"Handebol A","modalidade":"HANDEBOL","equipes":["IFPE","IFPB","IFRN"]}' "Handebol A"
criar '{"id":"demo-hand-b","nome":"Handebol B","modalidade":"HANDEBOL","equipes":["IFSC","IFRS","IFPR","IFSul","IFFar"]}' "Handebol B"
