#!/usr/bin/env bash
# Publica arquivos do build numa release fixa por branch ("build-<branch>"), substituindo a versão
# anterior. Assets de release não contam na cota de armazenamento do GitHub Actions (artefatos
# contam, e cada push enviava GBs que ficavam 90 dias).
#
# Uso: publicar-build.sh <padrão-a-apagar> <arquivo>[#nome] [<arquivo>[#nome] ...]
#   <padrão-a-apagar>: regex (grep -E) dos assets antigos que este job substitui, ex. '-windows\.zip$'.
#                      Use '' para só sobrescrever assets de mesmo nome.
# Precisa de GH_TOKEN e permissions: contents: write.
set -euo pipefail

padrao="$1"; shift
branch="${GITHUB_REF_NAME//\//-}"
tag="build-${branch}"
repo="${GITHUB_REPOSITORY}"
curto="${GITHUB_SHA:0:12}"
notas="Build mais recente da branch ${GITHUB_REF_NAME} (commit ${curto}, $(date -u '+%Y-%m-%d %H:%M UTC')). Esta release é substituída a cada push."

# Vários jobs (APK, PC por sistema) podem chegar juntos: quem perder a corrida da criação segue.
if ! gh release view "$tag" -R "$repo" >/dev/null 2>&1; then
  gh release create "$tag" -R "$repo" --target "$GITHUB_SHA" --prerelease \
    --title "Build mais recente — ${GITHUB_REF_NAME}" --notes "$notas" \
    || gh release view "$tag" -R "$repo" >/dev/null
fi

if [ -n "$padrao" ]; then
  gh release view "$tag" -R "$repo" --json assets -q '.assets[].name' \
    | { grep -E -- "$padrao" || true; } \
    | while read -r antigo; do
        gh release delete-asset "$tag" "$antigo" -R "$repo" -y || true
      done
fi

gh release upload "$tag" "$@" -R "$repo" --clobber
gh release edit "$tag" -R "$repo" --notes "$notas" >/dev/null || true
echo "Publicado em: https://github.com/${repo}/releases/tag/${tag}"
