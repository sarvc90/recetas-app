const API_BASE_URL =
  window.APP_CONFIG.API_BASE_URL || 'http://localhost:8080/api';
const loading = document.getElementById('loading');
const message = document.getElementById('message');
const editarPerfilForm = document.getElementById('editarPerfilForm');
const nombreInput = document.getElementById('nombre');
const fotoPerfilInput = document.getElementById('fotoPerfil');
const fotoActual = document.getElementById('fotoActual');

let usuarioActual = null;

function getAuthHeaders() {
  const token = localStorage.getItem('token');
  return {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${token}`,
  };
}

function getAuthHeadersMultipart() {
  const token = localStorage.getItem('token');
  return { Authorization: `Bearer ${token}` };
}

async function initApp() {
  const usuario = localStorage.getItem('usuario');
  if (!usuario) {
    window.location.href = 'index.html';
    return;
  }
  usuarioActual = JSON.parse(usuario);
  await cargarDatosUsuario();
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', initApp);
} else {
  initApp();
}

async function cargarDatosUsuario() {
  try {
    const response = await fetch(
      `${API_BASE_URL}/usuarios/${usuarioActual.id}`,
      {
        headers: getAuthHeaders(),
      },
    );
    if (response.ok) {
      const data = await response.json();
      if (data.success && data.data) {
        usuarioActual = data.data;
        localStorage.setItem('usuario', JSON.stringify(usuarioActual));
      }
    }
  } catch (error) {
    console.log('Usando datos locales:', error);
  }
  nombreInput.value = usuarioActual.nombre || '';
  const fotoPerfil = usuarioActual.fotoPerfil;
  if (fotoPerfil && fotoPerfil !== '' && fotoPerfil !== 'icon.png') {
    fotoActual.src = fotoPerfil;
    fotoActual.style.width = '100%';
    fotoActual.style.height = '100%';
    fotoActual.style.objectFit = 'cover';
  }
  actualizarEstadoEmail();
}

function actualizarEstadoEmail() {
  const statusText = document.getElementById('email-status-text');
  const btnSolicitar = document.getElementById('solicitar-verificacion-btn');

  if (!statusText) return;

  if (usuarioActual.emailVerificado) {
    statusText.innerHTML = `<span style="color:#155724;">✅ Email verificado (${usuarioActual.email || ''})</span>`;
    btnSolicitar.style.display = 'none';
  } else {
    statusText.innerHTML = `<span style="color:#842029;">⚠️ Email no verificado (${usuarioActual.email || ''})</span>`;
    btnSolicitar.style.display = 'inline-block';
  }
}

// ─── VERIFICACIÓN DE EMAIL ──────────────────────────────────────────────────
async function solicitarVerificacionEmail() {
  try {
    mostrarMensaje('⏳ Enviando código de verificación...', 'loading');
    const response = await fetch(
      `${API_BASE_URL}/usuarios/${usuarioActual.id}/solicitar-verificacion-email`,
      { method: 'POST', headers: getAuthHeaders() },
    );
    const data = await response.json();

    if (data.success) {
      mostrarMensaje('📧 Código enviado. Ingrésalo abajo.', 'success');
      document.getElementById('codigo-verificacion-section').style.display =
        'block';
      document.getElementById('solicitar-verificacion-btn').style.display =
        'none';
      document.getElementById('codigo-verificacion').value = '';
      document.getElementById('codigo-verificacion').focus();
    } else {
      mostrarMensaje(
        '❌ ' + (data.message || 'No se pudo enviar el código'),
        'error',
      );
    }
  } catch (error) {
    mostrarMensaje('❌ Error de conexión: ' + error.message, 'error');
  }
}

async function confirmarVerificacionEmail() {
  const codigo = document.getElementById('codigo-verificacion').value.trim();

  if (!codigo || codigo.length !== 6) {
    mostrarMensaje('❌ El código debe tener exactamente 6 dígitos', 'error');
    return;
  }

  try {
    mostrarMensaje('⏳ Verificando código...', 'loading');
    const response = await fetch(
      `${API_BASE_URL}/usuarios/${usuarioActual.id}/verificar-email`,
      {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({ codigo }),
      },
    );
    const data = await response.json();

    if (data.success) {
      usuarioActual.emailVerificado = true;
      localStorage.setItem('usuario', JSON.stringify(usuarioActual));
      mostrarMensaje('✅ Email verificado correctamente', 'success');
      cancelarVerificacionEmail();
      actualizarEstadoEmail();
    } else {
      mostrarMensaje('❌ ' + (data.message || 'Código incorrecto'), 'error');
    }
  } catch (error) {
    mostrarMensaje('❌ Error de conexión: ' + error.message, 'error');
  }
}

function cancelarVerificacionEmail() {
  document.getElementById('codigo-verificacion-section').style.display = 'none';
  document.getElementById('codigo-verificacion').value = '';
  if (!usuarioActual.emailVerificado) {
    document.getElementById('solicitar-verificacion-btn').style.display =
      'inline-block';
  }
}

// ─── FOTO Y PERFIL ──────────────────────────────────────────────────────────

fotoPerfilInput.addEventListener('change', (e) => {
  const file = e.target.files[0];
  if (file) {
    if (file.size > 5 * 1024 * 1024) {
      mostrarMensaje('La imagen no debe superar los 5MB', 'error');
      fotoPerfilInput.value = '';
      return;
    }
    const reader = new FileReader();
    reader.onload = (e) => {
      fotoActual.src = e.target.result;
      fotoActual.style.width = '100%';
      fotoActual.style.height = '100%';
      fotoActual.style.objectFit = 'cover';
    };
    reader.readAsDataURL(file);
  }
});

editarPerfilForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  const nombre = nombreInput.value.trim();
  if (!nombre) {
    mostrarMensaje('El nombre es obligatorio', 'error');
    return;
  }
  try {
    loading.classList.remove('hidden');
    message.classList.add('hidden');
    let fotoUrl = usuarioActual.fotoPerfil || '';
    if (fotoPerfilInput.files[0]) {
      const formData = new FormData();
      formData.append('file', fotoPerfilInput.files[0]);
      const imageResponse = await fetch(
        `${API_BASE_URL}/usuarios/${usuarioActual.id}/foto`,
        {
          method: 'POST',
          headers: getAuthHeadersMultipart(),
          body: formData,
        },
      );
      if (!imageResponse.ok) throw new Error('Error al subir la imagen');
      const imageData = await imageResponse.json();
      fotoUrl = imageData.data;
    }
    const response = await fetch(
      `${API_BASE_URL}/usuarios/${usuarioActual.id}/perfil`,
      {
        method: 'PUT',
        headers: getAuthHeaders(),
        body: JSON.stringify({ nombre: nombre, fotoPerfil: fotoUrl }),
      },
    );
    if (!response.ok) throw new Error('Error al actualizar el perfil');
    const data = await response.json();
    if (data.success) {
      const usuarioActualizado = {
        id: usuarioActual.id,
        nombre: data.data.nombre,
        email: usuarioActual.email,
        fotoPerfil: data.data.fotoPerfil,
        emailVerificado:
          data.data.emailVerificado ?? usuarioActual.emailVerificado,
      };
      localStorage.setItem('usuario', JSON.stringify(usuarioActualizado));
      mostrarMensaje('✅ Perfil actualizado correctamente', 'success');
      setTimeout(() => {
        window.location.href = 'recetas.html';
      }, 1500);
    } else {
      throw new Error(data.message || 'Error al actualizar');
    }
  } catch (error) {
    console.error('Error:', error);
    mostrarMensaje(
      '❌ Error al actualizar el perfil: ' + error.message,
      'error',
    );
  } finally {
    loading.classList.add('hidden');
  }
});

function mostrarMensaje(texto, tipo) {
  message.textContent = texto;
  if (tipo === 'success') {
    message.className = 'message success';
  } else if (tipo === 'loading') {
    message.className = 'message loading';
  } else {
    message.className = 'message error';
  }
  message.classList.remove('hidden');
  if (tipo !== 'loading') {
    setTimeout(() => {
      message.classList.add('hidden');
    }, 3000);
  }
}
