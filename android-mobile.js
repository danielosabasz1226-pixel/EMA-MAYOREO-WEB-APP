/* EMA Android UI v2 · navegación y acciones móviles */
(() => {
  'use strict';

  document.documentElement.classList.add('ema-android');
  document.body?.classList.add('ema-android');

  const normalize = (value) => String(value || '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/\s+/g, ' ')
    .trim()
    .toLowerCase();

  const clickAction = (keys) => {
    const wanted = (Array.isArray(keys) ? keys : [keys]).map(normalize);
    const nodes = Array.from(document.querySelectorAll(
      'button, a, [role="button"], .btn, .tab, label'
    ));

    const exact = nodes.find((node) => {
      const text = normalize(node.innerText || node.textContent);
      return wanted.some((key) => text === key);
    });

    const partial = nodes.find((node) => {
      const text = normalize(node.innerText || node.textContent);
      return wanted.some((key) => key && text.includes(key));
    });

    const target = exact || partial;
    if (target) {
      target.click();
      target.scrollIntoView({ behavior: 'smooth', block: 'center' });
      return true;
    }

    return false;
  };

  const openImport = () => {
    const input = document.querySelector('input[type="file"]');
    if (input) {
      input.click();
      return true;
    }
    return clickAction([
      'importar excel',
      'importar',
      'cargar excel',
      'cargar archivo',
      'excel'
    ]);
  };

  const scrollTop = () => {
    window.scrollTo({ top: 0, behavior: 'smooth' });
    document.querySelector('.shell')?.scrollTo?.({ top: 0, behavior: 'smooth' });
    return true;
  };

  window.EMAAndroid = {
    clickAction,
    openImport,
    scrollTop,
    newOrder: () => clickAction(['nuevo pedido', 'nuevo pedido / cotización', 'nuevo', 'crear pedido']),
    history: () => clickAction(['historial', 'pedidos', 'mis pedidos']),
    sellers: () => clickAction(['vendedores', 'vendedor']),
    customers: () => clickAction(['clientes', 'cliente']),
    print: () => clickAction(['imprimir', 'imprimir pedido', 'pdf', 'generar pdf']),
    more: () => scrollTop()
  };

  const refresh = () => {
    document.documentElement.classList.add('ema-android');
    document.body?.classList.add('ema-android');
    document.querySelectorAll('.cod').forEach((node) => node.remove());
  };

  const observer = new MutationObserver(refresh);

  const start = () => {
    refresh();
    observer.observe(document.body, {
      childList: true,
      subtree: true,
      characterData: true
    });
  };

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', start, { once: true });
  } else {
    start();
  }
})();
