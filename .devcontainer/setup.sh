#!/usr/bin/env bash
# Автонастройка окружения для сборки RuVideoHub в GitHub Codespaces.
# Запускается автоматически (postCreateCommand), но можно перезапустить вручную:
#   bash .devcontainer/setup.sh
set -e

echo "==> Устанавливаю системные пакеты..."
sudo apt-get update -y
sudo apt-get install -y unzip curl python3

export ANDROID_HOME="$HOME/android-sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
mkdir -p "$ANDROID_HOME/cmdline-tools"

if [ ! -d "$ANDROID_HOME/cmdline-tools/latest" ]; then
  echo "==> Определяю актуальную ссылку на Android cmdline-tools..."
  MANIFEST_URL="https://dl.google.com/android/repository/repository2-3.xml"
  curl -fsSL "$MANIFEST_URL" -o /tmp/repo.xml

  CMDLINE_URL=$(python3 - <<'PYEOF'
import re
with open('/tmp/repo.xml', encoding='utf-8', errors='ignore') as f:
    data = f.read()
blocks = re.findall(r'<remotePackage path="cmdline-tools;([^"]+)">(.*?)</remotePackage>', data, re.S)
best_ver, best_url = None, None
for ver, block in blocks:
    if ver == 'latest':
        continue
    m = re.search(r'host-os="linux"[^>]*>\s*<url>([^<]+)</url>', block)
    if not m:
        continue
    url = m.group(1)
    try:
        v = float(ver)
    except ValueError:
        continue
    if best_ver is None or v > best_ver:
        best_ver, best_url = v, url
if best_url:
    if not best_url.startswith('http'):
        best_url = 'https://dl.google.com/android/repository/' + best_url
    print(best_url)
PYEOF
)

  if [ -z "$CMDLINE_URL" ]; then
    echo "!! Не удалось определить ссылку автоматически."
    echo "   Откройте https://developer.android.com/studio#command-line-tools-only,"
    echo "   скопируйте ссылку на Linux-архив и выполните вручную:"
    echo "     curl -L -o /tmp/cmdline-tools.zip <ССЫЛКА>"
    echo "     unzip -q /tmp/cmdline-tools.zip -d \$ANDROID_HOME/cmdline-tools"
    echo "     mv \$ANDROID_HOME/cmdline-tools/cmdline-tools \$ANDROID_HOME/cmdline-tools/latest"
    exit 1
  fi

  echo "==> Скачиваю $CMDLINE_URL"
  curl -fsSL "$CMDLINE_URL" -o /tmp/cmdline-tools.zip
  unzip -q /tmp/cmdline-tools.zip -d "$ANDROID_HOME/cmdline-tools"
  mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
fi

export PATH="$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools"

echo "==> Принимаю лицензии и ставлю пакеты SDK (platform-tools, android-36, build-tools 36.0.0)..."
yes | sdkmanager --licenses >/dev/null 2>&1 || true
sdkmanager "platform-tools" "platforms;android-36" "build-tools;36.0.0"

if ! grep -q "ANDROID_HOME" "$HOME/.bashrc" 2>/dev/null; then
  {
    echo ''
    echo '# Android SDK (добавлено настройкой RuVideoHub)'
    echo "export ANDROID_HOME=\"$ANDROID_HOME\""
    echo 'export ANDROID_SDK_ROOT="$ANDROID_HOME"'
    echo 'export PATH="$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools"'
  } >> "$HOME/.bashrc"
fi

if [ -f "debug.keystore.base64" ] && [ ! -f "debug.keystore" ]; then
  base64 -d debug.keystore.base64 > debug.keystore
  echo "==> debug.keystore восстановлен из debug.keystore.base64"
fi

chmod +x ./gradlew 2>/dev/null || true

echo ""
echo "==> Готово! Откройте новый терминал (или выполните: source ~/.bashrc), затем:"
echo "      ./gradlew assembleDebug --stacktrace"
echo "==> APK появится в: app/build/outputs/apk/debug/"
