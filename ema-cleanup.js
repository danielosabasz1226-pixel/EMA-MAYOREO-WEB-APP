(() => {
  'use strict';

  const cleanProductText = (value) => {
    if (typeof value !== 'string') return value;
    let text = value.replace(/\u00A0/g, ' ').replace(/\s+/g, ' ').trim();

    text = text.replace(/(?:^|\s)C[oó]d\.?\s*CBC\s*[-–—:]?\s*[^|;\n]*?\.[A-Z]{1,8}\d{4,}\s*$/i, '');
    text = text.replace(/\s*[-–—]\s*[^|;\n]*?\.[A-Z]{1,8}\d{4,}\s*$/i, '');
    text = text.replace(/\s*\(\s*[A-Z]{1,8}\d{4,}\s*\)\s*$/i, '');
    text = text.replace(/\s*\(?[A-Z]{1,8}\d{4,}\)?\s*$/i, '');

    return text.replace(/\s{2,}/g, ' ').trim();
  };

  const cleanQuote = (quote) => {
    if (!quote || !Array.isArray(quote.items)) return quote;
    quote.items.forEach(item => {
      item.codigo = '';
      item.code = '';
      item.codigoProducto = '';
      item.descripcion = cleanProductText(item.descripcion || item.name || '');
      if ('name' in item) item.name = item.descripcion;
    });
    return quote;
  };

  const cleanState = () => {
    try {
      if (typeof state === 'undefined') return;
      if (Array.isArray(state.quotes)) state.quotes.forEach(cleanQuote);

      if (Array.isArray(state.importHistory)) {
        state.importHistory.forEach(entry => {
          if (Array.isArray(entry.preview)) entry.preview.forEach(cleanQuote);
          if (Array.isArray(entry.quotes)) entry.quotes.forEach(cleanQuote);
        });
      }

      if (typeof saveState === 'function') saveState();
    } catch (e) {
      console.warn('EMA: limpieza de códigos no disponible todavía.', e);
    }
  };

  const hideCodeElements = () => {
    document.querySelectorAll('.cod,[class*="codigo"],[class*="code"]').forEach(el => {
      el.remove();
    });
  };

  const cleanVisibleText = () => {
    document.querySelectorAll('.items td:first-child, .items td:first-child *').forEach(el => {
      if (!el.children.length && el.textContent) {
        el.textContent = cleanProductText(el.textContent);
      }
    });
    hideCodeElements();
  };

  const patchExcel = () => {
    try {
      if (!window.XLSX || typeof window.XLSX.read !== 'function' || window.XLSX.read.__emaSafe) return;

      const original = window.XLSX.read;
      const safeRead = function(data, opts) {
        const wb = original.call(this, data, opts);
        try {
          for (const sheetName of (wb.SheetNames || [])) {
            const ws = wb.Sheets?.[sheetName];
            if (!ws) continue;

            for (const key of Object.keys(ws)) {
              if (key.startsWith('!')) continue;
              const cell = ws[key];
              if (!cell || typeof cell !== 'object') continue;
              if (typeof cell.v === 'string') cell.v = cleanProductText(cell.v);
              if (typeof cell.w === 'string') cell.w = cleanProductText(cell.w);
            }
          }
        } catch (e) {
          console.warn('EMA: no se pudo limpiar el Excel.', e);
        }
        return wb;
      };

      safeRead.__emaSafe = true;
      window.XLSX.read = safeRead;
    } catch (e) {
      console.warn('EMA: XLSX todavía no está listo.', e);
    }
  };

  const protectApp = () => {
    patchExcel();
    cleanState();

    try {
      if (typeof renderQuote === 'function' && !renderQuote.__emaSafe) {
        const originalRender = renderQuote;
        const safeRender = function() {
          cleanState();
          const result = originalRender.apply(this, arguments);
          setTimeout(cleanVisibleText, 0);
          return result;
        };
        safeRender.__emaSafe = true;
        renderQuote = safeRender;
      }

      if (typeof exportPdf === 'function' && !exportPdf.__emaSafe) {
        const originalPdf = exportPdf;
        const safePdf = function(quote) {
          cleanState();
          return originalPdf.call(this, cleanQuote(quote));
        };
        safePdf.__emaSafe = true;
        exportPdf = safePdf;
      }
    } catch (e) {
      console.warn('EMA: no se pudieron proteger las facturas.', e);
    }

    cleanVisibleText();
  };

  const start = () => {
    protectApp();

    const observer = new MutationObserver(() => protectApp());
    observer.observe(document.body, {
      childList: true,
      subtree: true,
      characterData: true
    });

    setInterval(protectApp, 1500);
  };

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', start, { once: true });
  } else {
    start();
  }
})();