(function () {
  const currentScript = document.currentScript;
  const mainScriptPath = currentScript?.dataset?.main;

  if (!mainScriptPath) {
    console.error('[env-loader] Falta data-main en el script loader.');
    return;
  }

  const hostname = window.location.hostname;
  const isDevelopment =
    hostname === 'localhost' ||
    hostname === '127.0.0.1' ||
    hostname === '::1' ||
    hostname.endsWith('.local') ||
    hostname === '';

  const configFilename = isDevelopment ? 'config.dev.js' : 'config.prod.js';
  const configPath = new URL(configFilename, currentScript.src).toString();

  const configScript = document.createElement('script');
  configScript.src = configPath;

  configScript.onload = () => {
    if (!window.APP_CONFIG) {
      console.error('[env-loader] APP_CONFIG no fue definido en', configPath);
      window.APP_CONFIG = {};
    }

    const mainScript = document.createElement('script');
    mainScript.src = mainScriptPath;
    document.body.appendChild(mainScript);
  };

  configScript.onerror = () => {
    console.error(`[env-loader] No se pudo cargar ${configPath}.`);
    window.APP_CONFIG = window.APP_CONFIG || {};

    const mainScript = document.createElement('script');
    mainScript.src = mainScriptPath;
    document.body.appendChild(mainScript);
  };

  document.head.appendChild(configScript);
})();
