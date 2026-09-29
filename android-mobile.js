/* =========================================================
   EMA ANDROID · comportamiento móvil
========================================================= */
(() => {
  'use strict';

  document.documentElement.classList.add('ema-android');
  document.body?.classList.add('ema-android');

  const refreshMobileState = () => {
    document.documentElement.classList.add('ema-android');
    document.body?.classList.add('ema-android');
  };

  const hideSecondaryCodes = () => {
    document.querySelectorAll('.cod').forEach((node) => node.remove());
  };

  const waitForApp = (attempt = 0) => {
    refreshMobileState();
    hideSecondaryCodes();

    if (typeof window.printHtml === 'function' && !window.printHtml.__emaAndroid) {
      const originalPrintHtml = window.printHtml;

      const safePrintHtml = function(quotes, title = 'EMA · Pedidos') {
        if (!window.AndroidBridge?.printHtml || typeof window.invoice !== 'function') {
          return originalPrintHtml.apply(this, arguments);
        }

        const safeQuotes = Array.isArray(quotes) ? quotes : [];
        const documentHtml = '<!doctype html>' +
          '<html lang="es-GT"><head>' +
          '<meta charset="utf-8">' +
          '<meta name="viewport" content="width=device-width,initial-scale=1">' +
          '<title>' + String(title).replace(/[<>]/g, '') + '</title>' +
          '<style>' +
          '@page{size:auto;margin:.12in}' +
          '*{box-sizing:border-box}' +
          'html,body{margin:0;padding:0;background:#fff;color:#111;font-family:Arial,Helvetica,sans-serif}' +
          '.page{width:100%;padding:0;margin:0 auto;page-break-after:always}' +
          '.page:last-child{page-break-after:auto}' +
          '.cod{display:none!important}' +
          '.invoice{width:100%;margin:0 auto;border:1px solid #111;padding:.14in;background:#fff}' +
          '.inv-head{text-align:center;border-bottom:2px solid #111;padding:.03in .03in .09in}' +
          '.company{font-size:15px;font-weight:900;letter-spacing:.07em}' +
          '.address{margin-top:3px;font-size:8px}' +
          '.title{margin-top:4px;font-size:9px;font-weight:900;letter-spacing:.18em}' +
          '.invoice table{width:100%;border-collapse:collapse}' +
          '.meta{margin-top:8px}.meta th,.meta td{border:1px solid #444;padding:5px;font-size:7.5px;vertical-align:top}' +
          '.meta th{text-align:left;font-weight:900;white-space:nowrap}' +
          '.items{margin-top:8px}.items th,.items td{padding:5px;font-size:7.5px;vertical-align:top}' +
          '.items thead tr{border-top:2px solid #111;border-bottom:2px solid #111}' +
          '.items tbody tr{border-bottom:1px solid #777}' +
          '.items th{font-weight:900}.items td{border-left:1px solid #555;border-right:1px solid #555;word-break:break-word}' +
          '.items th.num,.items td.num{text-align:right;white-space:nowrap}' +
          '.total{margin-top:5px}.total td{padding:6px;border-top:2px solid #111;border-bottom:2px solid #111;font-weight:900}' +
          '.total .label{text-align:right}.total .value{text-align:right;white-space:nowrap}' +
          '.inv-footer{margin-top:8px;text-align:center;font-size:8px;font-style:italic}' +
          '.inv-cond{margin-top:5px;padding-top:4px;border-top:1px dashed #777;text-align:center;color:#555;font-size:6.5px}' +
          '</style></head><body>' +
          safeQuotes.map((quote) => '<div class="page">' + window.invoice(quote) + '</div>').join('') +
          '</body></html>';

        window.AndroidBridge.printHtml(documentHtml);
      };

      safePrintHtml.__emaAndroid = true;
      window.printHtml = safePrintHtml;
    }

    if (typeof window.downloadBlob === 'function' && !window.downloadBlob.__emaAndroid) {
      const originalDownloadBlob = window.downloadBlob;

      const safeDownloadBlob = function(blob, filename) {
        if (!window.AndroidBridge?.saveBlob || !blob) {
          return originalDownloadBlob.apply(this, arguments);
        }

        blob.arrayBuffer().then((buffer) => {
          const bytes = new Uint8Array(buffer);
          let binary = '';
          const chunk = 0x8000;
          for (let i = 0; i < bytes.length; i += chunk) {
            binary += String.fromCharCode(...bytes.subarray(i, i + chunk));
          }
          window.AndroidBridge.saveBase64(
            String(filename || 'EMA-archivo'),
            String(blob.type || 'application/octet-stream'),
            btoa(binary)
          );
        }).catch((error) => {
          console.error('EMA Android saveBlob:', error);
          originalDownloadBlob.apply(this, arguments);
        });
      };

      safeDownloadBlob.__emaAndroid = true;
      window.downloadBlob = safeDownloadBlob;
    }

    hideSecondaryCodes();

    if (attempt < 30) {
      setTimeout(() => waitForApp(attempt + 1), 250);
    }
  };

  const observer = new MutationObserver(() => {
    refreshMobileState();
    hideSecondaryCodes();
  });

  const start = () => {
    refreshMobileState();
    hideSecondaryCodes();
    observer.observe(document.body, { childList: true, subtree: true, characterData: true });
    waitForApp();
  };

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', start, { once: true });
  } else {
    start();
  }
})();
