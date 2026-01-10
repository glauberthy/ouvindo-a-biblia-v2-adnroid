#!/bin/bash

# Define a pasta de destino principal
DEST_DIR="/home/glauberthy/Desktop/ouvindo_a_biblia/gems_context"

# Limpa ou cria a pasta de destino principal
rm -rf "$DEST_DIR"
mkdir -p "$DEST_DIR"

echo "Copiando arquivos de forma 'flat' para '$DEST_DIR'..."

# --- 1. Copiar conteúdo de app/src/main para gems_context/app (flat) ---
SRC_APP="app/src/main"
DEST_APP="$DEST_DIR/app"
mkdir -p "$DEST_APP"
# Encontra todos os arquivos e copia diretamente para o destino, ignorando subdiretórios
find "$SRC_APP" -type f -exec cp {} "$DEST_APP" \;
echo "Conteúdo de $SRC_APP copiado para $DEST_APP"

# --- 2. Copiar conteúdo de data/local/src/main/java para gems_context/local (flat) ---
SRC_LOCAL="data/local/src/main/java"
DEST_LOCAL="$DEST_DIR/local"
mkdir -p "$DEST_LOCAL"
find "$SRC_LOCAL" -type f -exec cp {} "$DEST_LOCAL" \;
echo "Conteúdo de $SRC_LOCAL copiado para $DEST_LOCAL"

# --- 3. Copiar conteúdo de data/remote/src/main/java para gems_context/remote (flat) ---
SRC_REMOTE="data/remote/src/main/java"
DEST_REMOTE="$DEST_DIR/remote"
mkdir -p "$DEST_REMOTE"
find "$SRC_REMOTE" -type f -exec cp {} "$DEST_REMOTE" \;
echo "Conteúdo de $SRC_REMOTE copiado para $DEST_REMOTE"

# --- 4. Copiar conteúdo de data/repository/src/main/java para gems_context/repository (flat) ---
SRC_REPO="data/repository/src/main/java"
DEST_REPO="$DEST_DIR/repository"
mkdir -p "$DEST_REPO"
find "$SRC_REPO" -type f -exec cp {} "$DEST_REPO" \;
echo "Conteúdo de $SRC_REPO copiado para $DEST_REPO"

# --- 5. Copiar arquivos únicos restantes (estes não precisam ser flat, são só arquivos) ---
cp "gradle/libs.versions.toml" "$DEST_DIR/"
cp "app/build.gradle.kts" "$DEST_DIR/"
cp "app/src/main/AndroidManifest.xml" "$DEST_DIR/app/" # Coloca o manifest dentro da pasta 'app'

echo "Concluído! Todos os arquivos foram movidos de forma 'flat' para '$DEST_DIR'."
