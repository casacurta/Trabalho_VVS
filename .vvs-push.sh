#!/bin/sh
# Script temporario de publicacao do projeto.
# Uso: sh /caminho/completo/.vvs-push.sh [private|public]   (padrao: private)
# Pode ser executado de qualquer diretorio: o script se posiciona na pasta
# onde ele mesmo reside (a raiz do projeto).
# O token e lido do teclado (entrada oculta), usado apenas em memoria e nunca
# e gravado em disco, no .git/config ou no historico do shell.
set -e

# Auto-localizacao: garante que git opera na raiz do projeto, independente do
# diretorio atual do shell. Sem isso, rodar de ~ falha silenciosamente.
case "$0" in
    */*) SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd) ;;
    *)   SCRIPT_DIR=$(pwd) ;;
esac
cd "$SCRIPT_DIR"

if [ ! -d .git ]; then
    echo "ERRO: $SCRIPT_DIR nao e um repositorio git." >&2
    exit 1
fi

echo "Diretorio de trabalho: $(pwd)"
if [ -n "$(git status --porcelain)" ]; then
    echo "ATENCAO: existem alteracoes nao commitadas; elas nao serao publicadas." >&2
else
    echo "Arvore de trabalho limpa."
fi
echo -n "Commit a publicar: "
git rev-parse --short HEAD
echo -n "Autor do commit   : "
git log -1 --format='%an <%ae>'

REPO="casacurta/Trabalho_VVS"
VISIBILITY="${1:-private}"
API="-H Accept:application/vnd.github+json"

case "$VISIBILITY" in
    private|public) ;;
    *) echo "ERRO: visibilidade deve ser 'private' ou 'public'."; exit 1 ;;
esac

TOKEN_FILE="${VVS_TOKEN_FILE:-/tmp/gh_token.txt}"

if [ -s "$TOKEN_FILE" ]; then
    echo "Lendo token de $TOKEN_FILE"
    TOKEN=$(tr -d '\r\n' < "$TOKEN_FILE")
else
    # A abertura e testada num subshell: em dash, falha de redirecionamento em
    # 'exec' encerraria o script antes de qualquer mensagem.
    if ! ( exec 3</dev/tty ) 2>/dev/null; then
        echo "ERRO: nenhum terminal interativo disponivel para ler o token." >&2
        echo "Grave o token em $TOKEN_FILE e execute novamente." >&2
        exit 1
    fi
    exec 3</dev/tty
    printf "Token do GitHub (entrada oculta, nao sera exibida): "
    stty -echo <&3 2>/dev/null || true
    IFS= read -r TOKEN <&3
    stty echo <&3 2>/dev/null || true
    exec 3<&-
    printf "\n"
fi

TOKEN=$(printf '%s' "$TOKEN" | tr -d '[:space:]')

if [ -z "$TOKEN" ]; then
    echo "ERRO: token vazio. Abortando."
    exit 1
fi

auth() { echo "Authorization: Bearer $TOKEN"; }

echo "=== 1/5 Validando token ==="
code=$(curl -s -o /tmp/vvs_user.json -w "%{http_code}" \
    -H "$(auth)" $API https://api.github.com/user)
echo "GET /user -> HTTP $code"
if [ "$code" != "200" ]; then
    echo "ERRO: token invalido ou expirado."
    grep -o '"message"[^,}]*' /tmp/vvs_user.json 2>/dev/null || true
    exit 1
fi
LOGIN=$(grep -o '"login"[[:space:]]*:[[:space:]]*"[^"]*"' /tmp/vvs_user.json \
    | head -1 | cut -d'"' -f4)
echo "Conta autenticada: $LOGIN"

echo "=== 2/5 Verificando repositorio $REPO ==="
code=$(curl -s -o /tmp/vvs_repo.json -w "%{http_code}" \
    -H "$(auth)" $API "https://api.github.com/repos/$REPO")
echo "GET /repos/$REPO -> HTTP $code"

if [ "$code" = "404" ]; then
    echo "Repositorio nao encontrado. Criando como $VISIBILITY..."
    BODY=$(printf '{"name":"Trabalho_VVS","private":%s,"description":"Sistema de gestao de consultas medicas (Quarkus + JPA)","has_issues":true,"has_wiki":false}' \
        "$( [ "$VISIBILITY" = "private" ] && echo true || echo false )")
    code=$(curl -s -o /tmp/vvs_create.json -w "%{http_code}" -X POST \
        -H "$(auth)" $API -d "$BODY" https://api.github.com/user/repos)
    echo "POST /user/repos -> HTTP $code"
    if [ "$code" != "201" ] && [ "$code" != "422" ]; then
        echo "ERRO ao criar repositorio:"
        grep -o '"message"[^,}]*' /tmp/vvs_create.json 2>/dev/null || true
        exit 1
    fi
elif [ "$code" != "200" ]; then
    echo "ERRO: sem acesso ao repositorio. O token precisa dos escopos 'repo'."
    grep -o '"message"[^,}]*' /tmp/vvs_repo.json 2>/dev/null || true
    exit 1
fi

echo "=== 3/5 Enviando commits para origin/main ==="
# --force substitui todo o historico remoto: necessario para eliminar os
# commits antigos e qualquer referencia ao autor anterior no GitHub.
export GH_TOKEN="$TOKEN"
git -c credential.helper='!f() { echo username=x-access-token; echo password=$GH_TOKEN; }; f' \
    push -u --force origin main
unset GH_TOKEN

echo -n "Ref remota local origin/main: "
if ! ORIGIN_SHA=$(git rev-parse --verify -q refs/remotes/origin/main); then
    echo "AUSENTE"
    echo "ERRO: o push nao atualizou origin/main. Abortando." >&2
    exit 1
fi
echo "$ORIGIN_SHA"
if [ "$ORIGIN_SHA" != "$(git rev-parse main)" ]; then
    echo "ERRO: origin/main ($ORIGIN_SHA) difere de main. Abortando." >&2
    exit 1
fi

echo "=== 4/5 Garantindo default branch = main ==="
code=$(curl -s -o /tmp/vvs_patch.json -w "%{http_code}" -X PATCH \
    -H "$(auth)" $API -d '{"default_branch":"main"}' \
    "https://api.github.com/repos/$REPO")
echo "PATCH default_branch -> HTTP $code"

echo "=== 5/5 Verificacao final ==="
echo -n "Commit local  : "
LOCAL_SHA=$(git rev-parse main)
echo "$LOCAL_SHA"
echo -n "Commit remoto : "
REMOTE_SHA=$(curl -s -H "$(auth)" $API \
    "https://api.github.com/repos/$REPO/commits/main" \
    | grep -o '"sha"[[:space:]]*:[[:space:]]*"[0-9a-f]*"' | head -1 | cut -d'"' -f4)
echo "$REMOTE_SHA"
if [ "$LOCAL_SHA" = "$REMOTE_SHA" ]; then
    echo "OK: remoto identico ao local."
else
    echo "ERRO: SHAs diferentes (remoto nao corresponde ao local)." >&2
    exit 1
fi

if git log -1 --pretty=%B | grep -qi 'co-authored-by'; then
    echo "ATENCAO: o commit contem trailer de coautoria."
else
    echo "OK: nenhum trailer de coautoria no commit."
fi
echo "Branch remota disponivel:"
git ls-remote --heads origin | sed 's/^/  /'

rm -f /tmp/vvs_user.json /tmp/vvs_repo.json /tmp/vvs_create.json /tmp/vvs_patch.json
echo "=== PUSH CONCLUIDO ==="
echo "URL: https://github.com/$REPO"
