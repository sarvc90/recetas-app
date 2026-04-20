// recuperar-password
const API_BASE_URL = window.APP_CONFIG.API_BASE_URL || `${window.location.origin}/api`;

let emailRecuperacion = '';

// Mostrar resultados en la página
function mostrarResultado(elementId, mensaje, tipo) {
  const elemento = document.getElementById(elementId);
  if (!elemento) return;

  elemento.textContent = mensaje;
  elemento.className = `result ${tipo}`.trim();
  elemento.setAttribute('role', tipo === 'error' ? 'alert' : 'status');
  elemento.setAttribute('aria-live', 'polite');
}

// Paso 1: Solicitar código de recuperación
async function solicitarCodigo() {
  const email = document.getElementById('email').value.trim();

  if (!email) {
    mostrarResultado(
      'resultado-email',
      '❌ Por favor, ingresa tu email',
      'error',
    );
    return;
  }

  try {
    mostrarResultado(
      'resultado-email',
      '⏳ Enviando código de recuperación...',
      'loading',
    );

    const response = await fetch(`${API_BASE_URL}/auth/recuperar-password`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ email }),
    });

    const data = await response.json();

    if (response.ok && data.success) {
      emailRecuperacion = email;
      mostrarResultado(
        'resultado-email',
        '✅ Si el email está registrado, recibirás un código de recuperación. Revisa tu bandeja de entrada.',
        'success',
      );

      // Mostrar paso 2 después de un breve momento
      setTimeout(() => {
        document.getElementById('paso-email').style.display = 'none';
        document.getElementById('paso-reset').style.display = 'block';
      }, 1500);
    } else {
      mostrarResultado(
        'resultado-email',
        `❌ ${data.message || 'Error al solicitar el código de recuperación'}`,
        'error',
      );
    }
  } catch (error) {
    mostrarResultado(
      'resultado-email',
      `❌ Error de conexión: ${error.message}`,
      'error',
    );
  }
}

// Paso 2: Resetear contraseña con código
async function resetPassword() {
  const codigo = document.getElementById('codigo').value.trim();
  const nuevaPassword = document.getElementById('nueva-password').value;
  const confirmarPassword = document.getElementById('confirmar-password').value;

  // Validaciones
  if (!codigo || !nuevaPassword || !confirmarPassword) {
    mostrarResultado(
      'resultado-reset',
      '❌ Por favor, completa todos los campos',
      'error',
    );
    return;
  }

  if (nuevaPassword.length < 6) {
    mostrarResultado(
      'resultado-reset',
      '❌ La contraseña debe tener al menos 6 caracteres',
      'error',
    );
    return;
  }

  if (nuevaPassword !== confirmarPassword) {
    mostrarResultado(
      'resultado-reset',
      '❌ Las contraseñas no coinciden',
      'error',
    );
    return;
  }

  try {
    mostrarResultado(
      'resultado-reset',
      '⏳ Cambiando contraseña...',
      'loading',
    );

    const response = await fetch(`${API_BASE_URL}/auth/reset-password`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        email: emailRecuperacion,
        codigo: codigo,
        nuevaPassword: nuevaPassword,
      }),
    });

    const data = await response.json();

    if (response.ok && data.success) {
      mostrarResultado(
        'resultado-reset',
        '✅ ¡Contraseña actualizada exitosamente! Redirigiendo al inicio de sesión...',
        'success',
      );

      setTimeout(() => {
        window.location.href = 'index.html';
      }, 2000);
    } else {
      let errorMessage = `❌ ${data.message || 'Error al cambiar la contraseña'}`;

      if (data.message && data.message.includes('expirado')) {
        errorMessage = '❌ El código ha expirado. Solicita uno nuevo.';
      } else if (data.message && data.message.includes('inválido')) {
        errorMessage = '❌ El código ingresado es incorrecto.';
      }

      mostrarResultado('resultado-reset', errorMessage, 'error');
    }
  } catch (error) {
    mostrarResultado(
      'resultado-reset',
      `❌ Error de conexión: ${error.message}`,
      'error',
    );
  }
}

// Volver al paso 1
function volverAPaso1() {
  document.getElementById('paso-reset').style.display = 'none';
  document.getElementById('paso-email').style.display = 'block';
  document.getElementById('resultado-email').textContent = '';
  document.getElementById('resultado-email').className = 'result';
}

// Inicialización de eventos
function setupEventListeners() {
  // Botones (reemplaza onclicks inline eliminados del HTML)
  document.getElementById('solicitar-codigo-btn').addEventListener('click', solicitarCodigo);
  document.getElementById('reset-password-btn').addEventListener('click', resetPassword);
  document.getElementById('volver-paso1-btn').addEventListener('click', volverAPaso1);

  // Soporte tecla Enter
  document.getElementById('email').addEventListener('keypress', function (e) {
    if (e.key === 'Enter') {
      solicitarCodigo();
    }
  });

  document.getElementById('codigo').addEventListener('keypress', function (e) {
    if (e.key === 'Enter') {
      document.getElementById('nueva-password').focus();
    }
  });

  document
    .getElementById('confirmar-password')
    .addEventListener('keypress', function (e) {
      if (e.key === 'Enter') {
        resetPassword();
      }
    });
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', setupEventListeners);
} else {
  setupEventListeners();
}
