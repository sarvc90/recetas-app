// script.js
const API_BASE_URL =
  window.APP_CONFIG.API_BASE_URL || 'http://localhost:8080/api';

// ─── Estado de la sesión de login (2FA) ────────────────────────────────────
let loginPending = false;
let loginPendingEmail = '';
let loginPendingPassword = '';

// ─── Estado de la sesión de registro ──────────────────────────────────────
let registroPending = false;
let registroPendingData = null; // { nombre, email, password }

// ─── Utilidades UI ──────────────────────────────────────────────────────────
function mostrarResultado(elementId, mensaje, tipo) {
  const elemento = document.getElementById(elementId);
  elemento.innerHTML = mensaje;
  elemento.className = `result ${tipo}`;
}

function limpiarFormularios() {
  document.getElementById('nombre').value = '';
  document.getElementById('email').value = '';
  document.getElementById('password').value = '';
  document.getElementById('login-email').value = '';
  document.getElementById('login-password').value = '';
}

async function postJson(url, payload) {
  const response = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });

  let data = null;
  try {
    data = await response.json();
  } catch (e) {
    // Sin body JSON
  }
  return { response, data };
}

// ─── LOGIN ──────────────────────────────────────────────────────────────────
function mostrarAuthCodeSection() {
  document.getElementById('auth-code-section').style.display = 'block';
  document.getElementById('login-btn').style.display = 'none';
  const input = document.getElementById('auth-code');
  input.value = '';
  input.focus();
}

function ocultarAuthCodeSection() {
  document.getElementById('auth-code-section').style.display = 'none';
  document.getElementById('login-btn').style.display = 'block';
}

function cancelarLogin() {
  loginPending = false;
  loginPendingEmail = '';
  loginPendingPassword = '';
  ocultarAuthCodeSection();
  mostrarResultado('resultado-login', '', '');
}

async function loginUsuario() {
  const email = document.getElementById('login-email').value.trim();
  const password = document.getElementById('login-password').value;

  if (!email || !password) {
    mostrarResultado(
      'resultado-login',
      '❌ Por favor, completa todos los campos',
      'error',
    );
    return;
  }

  try {
    mostrarResultado('resultado-login', '⏳ Iniciando sesión...', 'loading');

    const inicioLogin = await postJson(`${API_BASE_URL}/auth/login`, {
      email,
      password,
    });

    if (!inicioLogin.response.ok || !inicioLogin.data?.success) {
      mostrarResultado(
        'resultado-login',
        `❌ ${inicioLogin.data?.message || 'Error en el login'}`,
        'error',
      );
      return;
    }

    // Paso 1 exitoso: guardar estado y mostrar input del código
    loginPending = true;
    loginPendingEmail = email;
    loginPendingPassword = password;

    mostrarResultado(
      'resultado-login',
      '📧 Código enviado a tu correo. Ingrésalo abajo.',
      'success',
    );
    mostrarAuthCodeSection();
  } catch (error) {
    mostrarResultado(
      'resultado-login',
      `❌ Error de conexión: ${error.message}`,
      'error',
    );
  }
}

async function verificarAuthCode() {
  if (!loginPending) return;

  const authCode = document.getElementById('auth-code').value.trim();

  if (!authCode || authCode.length !== 6) {
    mostrarResultado(
      'resultado-login',
      '❌ El código debe tener exactamente 6 dígitos',
      'error',
    );
    return;
  }

  try {
    mostrarResultado('resultado-login', '⏳ Verificando código...', 'loading');

    const confirmacionLogin = await postJson(`${API_BASE_URL}/auth/login`, {
      email: loginPendingEmail,
      password: loginPendingPassword,
      authCode,
    });

    if (confirmacionLogin.response.ok && confirmacionLogin.data?.success) {
      const usuario = confirmacionLogin.data.data;
      mostrarResultado(
        'resultado-login',
        `✅ ¡Bienvenido de nuevo, ${usuario.nombre}!`,
        'success',
      );

      document.getElementById('auth-code').value = '';
      localStorage.setItem('usuario', JSON.stringify(usuario));
      localStorage.setItem('token', usuario.token || '');
      loginPending = false;

      setTimeout(() => {
        window.location.href = 'recetas.html';
      }, 1500);
      return;
    }

    mostrarResultado(
      'resultado-login',
      `❌ ${confirmacionLogin.data?.message || 'Código incorrecto'}`,
      'error',
    );
  } catch (error) {
    mostrarResultado(
      'resultado-login',
      `❌ Error de conexión: ${error.message}`,
      'error',
    );
  }
}

// ─── REGISTRO ───────────────────────────────────────────────────────────────
function mostrarVerificationCodeSection() {
  document.getElementById('verification-code-section').style.display = 'block';
  document.getElementById('registro-btn').style.display = 'none';
  const input = document.getElementById('verification-code');
  input.value = '';
  input.focus();
}

function ocultarVerificationCodeSection() {
  document.getElementById('verification-code-section').style.display = 'none';
  document.getElementById('registro-btn').style.display = 'block';
}

function cancelarRegistro() {
  registroPending = false;
  registroPendingData = null;
  ocultarVerificationCodeSection();
  mostrarResultado('resultado-registro', '', '');
}

async function registrarUsuario() {
  const nombre = document.getElementById('nombre').value.trim();
  const email = document.getElementById('email').value.trim();
  const password = document.getElementById('password').value;

  if (!nombre || !email || !password) {
    mostrarResultado(
      'resultado-registro',
      '❌ Por favor, completa todos los campos',
      'error',
    );
    return;
  }

  if (password.length < 6) {
    mostrarResultado(
      'resultado-registro',
      '❌ La contraseña debe tener al menos 6 caracteres',
      'error',
    );
    return;
  }

  try {
    mostrarResultado(
      'resultado-registro',
      '⏳ Enviando código de verificación...',
      'loading',
    );

    const inicioRegistro = await postJson(`${API_BASE_URL}/auth/registro`, {
      nombre,
      email,
      password,
    });

    if (!inicioRegistro.response.ok || !inicioRegistro.data?.success) {
      mostrarResultado(
        'resultado-registro',
        `❌ ${inicioRegistro.data?.message || 'No fue posible iniciar el registro'}`,
        'error',
      );
      return;
    }

    // Paso 1 exitoso: guardar estado y mostrar input del código
    registroPending = true;
    registroPendingData = { nombre, email, password };

    mostrarResultado(
      'resultado-registro',
      '📧 Código de verificación enviado a tu correo. Ingrésalo abajo.',
      'success',
    );
    mostrarVerificationCodeSection();
  } catch (error) {
    mostrarResultado(
      'resultado-registro',
      `❌ Error de conexión: ${error.message}`,
      'error',
    );
  }
}

async function verificarRegistroCode() {
  if (!registroPending || !registroPendingData) return;

  const verificationCode = document
    .getElementById('verification-code')
    .value.trim();

  if (!verificationCode || verificationCode.length !== 6) {
    mostrarResultado(
      'resultado-registro',
      '❌ El código debe tener exactamente 6 dígitos',
      'error',
    );
    return;
  }

  try {
    mostrarResultado(
      'resultado-registro',
      '⏳ Verificando código y completando registro...',
      'loading',
    );

    const { nombre, email, password } = registroPendingData;

    const confirmacionRegistro = await postJson(
      `${API_BASE_URL}/auth/registro`,
      {
        nombre,
        email,
        password,
        verificationCode,
      },
    );

    if (
      confirmacionRegistro.response.ok &&
      confirmacionRegistro.data?.success
    ) {
      const usuario = confirmacionRegistro.data.data;
      mostrarResultado(
        'resultado-registro',
        `✅ ¡Registro exitoso! Bienvenido, ${usuario.nombre}.`,
        'success',
      );

      localStorage.setItem('usuario', JSON.stringify(usuario));
      localStorage.setItem('token', usuario.token || '');
      registroPending = false;
      registroPendingData = null;
      limpiarFormularios();
      ocultarVerificationCodeSection();
      return;
    }

    mostrarResultado(
      'resultado-registro',
      `❌ ${confirmacionRegistro.data?.message || 'No fue posible confirmar el registro'}`,
      'error',
    );
  } catch (error) {
    mostrarResultado(
      'resultado-registro',
      `❌ Error de conexión: ${error.message}`,
      'error',
    );
  }
}

// ─── Soporte tecla Enter ─────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', function () {
  document
    .getElementById('password')
    .addEventListener('keypress', function (e) {
      if (e.key === 'Enter') registrarUsuario();
    });

  document
    .getElementById('login-password')
    .addEventListener('keypress', function (e) {
      if (e.key === 'Enter') loginUsuario();
    });

  document
    .getElementById('auth-code')
    .addEventListener('keypress', function (e) {
      if (e.key === 'Enter') verificarAuthCode();
    });

  document
    .getElementById('verification-code')
    .addEventListener('keypress', function (e) {
      if (e.key === 'Enter') verificarRegistroCode();
    });
});
