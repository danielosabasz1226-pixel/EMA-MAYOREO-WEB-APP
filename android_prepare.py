from pathlib import Path
import re
import shutil
import subprocess
import zipfile

ROOT = Path.cwd()
ASSETS = ROOT / "app" / "src" / "main" / "assets" / "ema"
STAGE = ROOT / ".android-stage"

zip_candidates = sorted(ROOT.glob("*EMA-MAYOREO-WEB*.zip"))
if not zip_candidates:
    raise SystemExit("No encontré el ZIP de EMA.")

zip_path = zip_candidates[0]

if STAGE.exists():
    shutil.rmtree(STAGE)
if ASSETS.exists():
    shutil.rmtree(ASSETS)

STAGE.mkdir(parents=True, exist_ok=True)
ASSETS.mkdir(parents=True, exist_ok=True)

with zipfile.ZipFile(zip_path) as z:
    z.extractall(STAGE)

indexes = list(STAGE.rglob("index.html"))
if not indexes:
    raise SystemExit("No encontré index.html dentro de EMA.")

source_root = indexes[0].parent
shutil.copytree(source_root, ASSETS, dirs_exist_ok=True)

mobile_css = ROOT / "android-mobile.css"
mobile_js = ROOT / "android-mobile.js"
shutil.copy2(mobile_css, ASSETS / "android-mobile.css")
shutil.copy2(mobile_js, ASSETS / "android-mobile.js")

index = ASSETS / "index.html"
html = index.read_text(encoding="utf-8")

html = html.replace(
    '<meta name="viewport" content="width=device-width, initial-scale=1.0">',
    '<meta name="viewport" content="width=device-width, initial-scale=1.0, viewport-fit=cover">'
)

if 'name="mobile-web-app-capable"' not in html:
    html = html.replace(
        '<meta name="theme-color" content="#0B4EA2">',
        '<meta name="theme-color" content="#0B4EA2">\n  <meta name="mobile-web-app-capable" content="yes">'
    )

if 'class="ema-android"' not in html:
    html = html.replace("<body>", '<body class="ema-android">', 1)

if 'href="android-mobile.css"' not in html:
    html = html.replace(
        '<link rel="stylesheet" href="styles.css">',
        '<link rel="stylesheet" href="styles.css">\n  <link rel="stylesheet" href="android-mobile.css">'
    )

if 'src="android-mobile.js"' not in html:
    html = html.replace(
        '<script src="app.js" defer></script>',
        '<script src="app.js" defer></script>\n  <script src="android-mobile.js" defer></script>'
    )

index.write_text(html, encoding="utf-8")

app = ASSETS / "app.js"
s = app.read_text(encoding="utf-8")

s = re.sub(
    r'^\s*\$\{item\.codigo.*class="cod".*\}\s*$',
    '',
    s,
    count=1,
    flags=re.MULTILINE
)

s = s.replace(
    "codigo: item.code, descripcion: item.name,",
    "codigo: '', descripcion: item.name,"
)
s = s.replace(
    "codigo: cleanText(item.codigo),",
    "codigo: '',"
)

print_anchor = "  const printWindow = window.open('', '_blank', 'width=900,height=1000');"
print_bridge = """  if (window.AndroidBridge && typeof window.AndroidBridge.printHtml === 'function') {
    window.AndroidBridge.printHtml(documentHtml);
    return;
  }

"""
if print_anchor in s and "window.AndroidBridge.printHtml(documentHtml);" not in s:
    s = s.replace(print_anchor, print_bridge + print_anchor, 1)

bt = chr(96)
old_pdf = "  doc.save(" + bt + "Pedido_\${quote.numero}.pdf" + bt + ");"
new_pdf = """  const pdfFilename = """ + bt + """Pedido_\${quote.numero}.pdf""" + bt + """;
  if (window.AndroidBridge && typeof window.AndroidBridge.saveBase64 === 'function') {
    const dataUri = doc.output('datauristring');
    const base64 = dataUri.slice(dataUri.indexOf(',') + 1);
    window.AndroidBridge.saveBase64(pdfFilename, 'application/pdf', base64);
  } else {
    doc.save(pdfFilename);
  }"""
if old_pdf in s and "const pdfFilename" not in s:
    s = s.replace(old_pdf, new_pdf, 1)

app.write_text(s, encoding="utf-8")

(ASSETS / "ANDROID-VERSION.txt").write_text(
    "EMA Android 1.3 - modo local, móvil, sin 404",
    encoding="utf-8"
)

checks = [
    ASSETS / "index.html",
    ASSETS / "app.js",
    ASSETS / "styles.css",
    ASSETS / "android-mobile.css",
    ASSETS / "android-mobile.js",
]
for p in checks:
    if not p.exists() or p.stat().st_size == 0:
        raise SystemExit(f"Falta archivo Android: {p}")

subprocess.run(["node", "--check", str(app)], check=True)

print("EMA Android local preparado correctamente.")
print(f"Fuente: {zip_path.name}")
print(f"Archivos en assets: {len(list(ASSETS.rglob('*')))}")
