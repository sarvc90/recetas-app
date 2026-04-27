// ================== CONFIG ==================
const API_BASE_URL =
  window.APP_CONFIG.API_BASE_URL || `${window.location.origin}/api`;

// ================== DOM REFS ==================
const messageEl = document.getElementById('message');
const loadingEl = document.getElementById('loading');
const suscripcionesActivasGrid = document.getElementById('suscripcionesActivasGrid');
const suscripcionesInactivasGrid = document.getElementById('suscripcionesInactivasGrid');
const activasEmpty = document.getElementById('activasEmpty');
const inactivasEmpty = document.getElementById('inactivasEmpty');

const recetasExclusivasModal = document.getElementById('recetasExclusivasModal');
const closeRecetasExclusivasModal = document.getElementById('closeRecetasExclusivasModal');
const recetasExclusivasTitle = document.getElementById('recetasExclusivasTitle');
const recetasExclusivasLoading = document.getElementById('recetasExclusivasLoading');
const recetasExclusivasError = document.getElementById('recetasExclusivasError');
const recetasExclusivasList = document.getElementById('recetasExclusivasList');

const recetaDetalleModal = document.getElementById('recetaDetalleModal');
const closeRecetaDetalleModal = document.getElementById('closeRecetaDetalleModal');
const recetaDetalleTitle = document.getElementById('recetaDetalleTitle');
const recetaDetalleDesc = document.getElementById('recetaDetalleDesc');
const recetaDetalleImg = document.getElementById('recetaDetalleImg');
const recetaDetalleIngredientes = document.getElementById('recetaDetalleIngredientes');
const recetaDetalleInstrucciones = document.getElementById('recetaDetalleInstrucciones');

// ================== HELPERS ==================
function getAuthHeaders() {
  const token = localStorage.getItem('token');
  return {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${token}`,
  };
}

function formatearFecha(fechaStr) {
  if (!fechaStr) return '—';
  return new Date(fechaStr).toLocaleDateString('es-ES', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });
}

function mostrarResultado(element, mensaje, tipo) {
  element.textContent = mensaje;
  element.className = `result ${tipo}`;
  element.setAttribute('role', tipo === 'error' ? 'alert' : 'status');
  element.setAttribute('aria-live', 'polite');
  element.classList.remove('hidden');
}

function calcularTiempoRestante(fechaFin) {
  if (!fechaFin) return null;
  const ahora = new Date();
  const fin = new Date(fechaFin);
  const diffMs = fin - ahora;

  if (diffMs <= 0) return null;

  const diffDias = Math.ceil(diffMs / (1000 * 60 * 60 * 24));

  if (diffDias === 1) return { texto: '1 día restante', proximoVencer: true };
  if (diffDias <= 7) return { texto: `${diffDias} días restantes`, proximoVencer: true };
  return { texto: `${diffDias} días restantes`, proximoVencer: false };
}

function esSuscripcionActiva(s) {
  return s.vigente === true;
}

// ================== HEADER ==================
function construirHeader(container, usuario) {
  const fotoPerfil = usuario.fotoPerfil || localStorage.getItem('usuarioFoto');

  const header = document.createElement('header');
  header.className = 'header';

  const userInfo = document.createElement('div');
  userInfo.className = 'user-info';

  const userProfile = document.createElement('div');
  userProfile.className = 'user-profile';

  if (fotoPerfil) {
    const userPhoto = document.createElement('img');
    userPhoto.src = fotoPerfil;
    userPhoto.alt = 'Foto de perfil';
    userPhoto.className = 'user-photo';
    userPhoto.width = 50;
    userPhoto.height = 50;
    userProfile.appendChild(userPhoto);
  } else {
    const placeholder = document.createElement('div');
    placeholder.className = 'user-photo-placeholder';
    const fallback = document.createElement('img');
    fallback.src = 'icon.png';
    fallback.alt = 'Usuario';
    fallback.width = 50;
    fallback.height = 50;
    fallback.style.width = '70%';
    fallback.style.height = '70%';
    fallback.style.objectFit = 'cover';
    placeholder.appendChild(fallback);
    userProfile.appendChild(placeholder);
  }

  const userName = document.createElement('span');
  userName.className = 'user-name';
  userName.textContent = `Bienvenido, ${usuario.nombre || 'Usuario'}`;
  userProfile.appendChild(userName);

  const navButtons = document.createElement('nav');
  navButtons.className = 'nav-buttons';
  navButtons.setAttribute('aria-label', 'Acciones de cuenta');

  [
    { href: 'recetas.html', label: '⬅ Volver a recetas' },
    { href: 'mis-recetas.html', label: 'Mis Recetas' },
    { href: 'favoritos.html', label: '❤️ Favoritos' },
    { href: 'editar-perfil.html', label: '✏️ Editar perfil' },
    { href: 'planes.html', label: '🍽️ Planes' },
    { href: 'mis-planes.html', label: '📋 Mis Planes' },
  ].forEach(({ href, label }) => {
    const link = document.createElement('a');
    link.href = href;
    link.className = 'nav-btn';
    link.textContent = label;
    navButtons.appendChild(link);
  });

  const logoutButton = document.createElement('button');
  logoutButton.id = 'logout';
  logoutButton.className = 'logout-btn';
  logoutButton.type = 'button';
  logoutButton.textContent = 'Cerrar Sesión';
  logoutButton.addEventListener('click', () => {
    localStorage.removeItem('usuario');
    localStorage.removeItem('token');
    window.location.href = 'index.html';
  });
  navButtons.appendChild(logoutButton);

  userInfo.append(userProfile, navButtons);
  header.appendChild(userInfo);
  container.insertBefore(header, container.firstChild);
}

// ================== LOAD SUBSCRIPTIONS ==================
async function cargarMisSuscripciones() {
  try {
    loadingEl.classList.remove('hidden');
    suscripcionesActivasGrid.replaceChildren();
    suscripcionesInactivasGrid.replaceChildren();
    activasEmpty.classList.add('hidden');
    inactivasEmpty.classList.add('hidden');

    const response = await fetch(`${API_BASE_URL}/suscripciones/mis-suscripciones`, {
      headers: getAuthHeaders(),
    });

    if (!response.ok) throw new Error('Error al cargar las suscripciones');

    const suscripciones = await response.json();
    const lista = Array.isArray(suscripciones) ? suscripciones : (suscripciones.data || []);

    const activas = lista.filter((s) => esSuscripcionActiva(s));
    const inactivas = lista.filter((s) => !esSuscripcionActiva(s));

    if (activas.length === 0) {
      activasEmpty.classList.remove('hidden');
    } else {
      activas.forEach((s) => {
        suscripcionesActivasGrid.appendChild(crearCardSuscripcionActiva(s));
      });
    }

    if (inactivas.length === 0) {
      inactivasEmpty.classList.remove('hidden');
    } else {
      inactivas.forEach((s) => {
        suscripcionesInactivasGrid.appendChild(crearCardSuscripcionInactiva(s));
      });
    }
  } catch (error) {
    console.error('Error al cargar suscripciones:', error);
    mostrarResultado(messageEl, `❌ Error al cargar tus suscripciones: ${error.message}`, 'error');
  } finally {
    loadingEl.classList.add('hidden');
  }
}

// ================== ACTIVE SUBSCRIPTION CARD ==================
function crearCardSuscripcionActiva(s) {
  const card = document.createElement('article');
  card.className = 'suscripcion-card';
  card.dataset.suscripcionId = s.id;

  const header = document.createElement('div');
  header.className = 'suscripcion-card-header';

  const nombreEl = document.createElement('h3');
  nombreEl.className = 'suscripcion-plan-name';
  nombreEl.textContent = s.nombrePlan || `Plan #${s.planId}`;

  const badgeEl = document.createElement('span');
  badgeEl.className = 'suscripcion-status-badge badge-activa';
  badgeEl.textContent = 'ACTIVA';

  header.append(nombreEl, badgeEl);

  const datesEl = document.createElement('div');
  datesEl.className = 'suscripcion-card-dates';

  const inicioItem = crearDateItem('Inicio', formatearFecha(s.fechaInicio));
  const finItem = crearDateItem('Vencimiento', formatearFecha(s.fechaFin));
  datesEl.append(inicioItem, finItem);

  const tiempoRestante = calcularTiempoRestante(s.fechaFin);
  let tiempoEl = null;
  if (tiempoRestante) {
    tiempoEl = document.createElement('div');
    tiempoEl.className = `suscripcion-time-remaining${tiempoRestante.proximoVencer ? ' expiring-soon' : ''}`;
    tiempoEl.textContent = `⏳ ${tiempoRestante.texto}`;
  }

  const accionesEl = document.createElement('div');
  accionesEl.className = 'suscripcion-card-actions';

  const verRecetasBtn = document.createElement('button');
  verRecetasBtn.className = 'ver-recetas-btn';
  verRecetasBtn.type = 'button';
  verRecetasBtn.textContent = '📖 Ver recetas exclusivas';
  verRecetasBtn.setAttribute('aria-label', `Ver recetas exclusivas de ${s.nombrePlan}`);
  verRecetasBtn.addEventListener('click', () => abrirRecetasExclusivas(s));

  const cancelarBtn = document.createElement('button');
  cancelarBtn.className = 'cancel-sub-btn';
  cancelarBtn.type = 'button';
  cancelarBtn.textContent = '✕ Cancelar suscripción';
  cancelarBtn.setAttribute('aria-label', `Cancelar suscripción a ${s.nombrePlan}`);
  cancelarBtn.addEventListener('click', () => cancelarSuscripcion(s.id, card));

  const reembolsarBtn = document.createElement('button');
  reembolsarBtn.className = 'refund-btn';
  reembolsarBtn.type = 'button';
  reembolsarBtn.textContent = '💰 Solicitar reembolso';
  reembolsarBtn.setAttribute('aria-label', `Solicitar reembolso de ${s.nombrePlan}`);
  reembolsarBtn.addEventListener('click', () => solicitarReembolso(s.id));

  accionesEl.append(verRecetasBtn, cancelarBtn, reembolsarBtn);

  card.append(header, datesEl);
  if (tiempoEl) card.appendChild(tiempoEl);
  card.appendChild(accionesEl);

  return card;
}

// ================== INACTIVE SUBSCRIPTION CARD ==================
function crearCardSuscripcionInactiva(s) {
  const card = document.createElement('article');

  const estadoLower = (s.estado || '').toLowerCase();
  card.className = `suscripcion-card ${estadoLower === 'cancelada' ? 'cancelada' : 'expirada'}`;

  const header = document.createElement('div');
  header.className = 'suscripcion-card-header';

  const nombreEl = document.createElement('h3');
  nombreEl.className = 'suscripcion-plan-name';
  nombreEl.textContent = s.nombrePlan || `Plan #${s.planId}`;

  const badgeEl = document.createElement('span');
  const esCancelada = estadoLower === 'cancelada' || estadoLower === 'cancelled';
  badgeEl.className = `suscripcion-status-badge ${esCancelada ? 'badge-cancelada' : 'badge-expirada'}`;
  badgeEl.textContent = esCancelada ? 'CANCELADA' : 'EXPIRADA';

  header.append(nombreEl, badgeEl);

  const datesEl = document.createElement('div');
  datesEl.className = 'suscripcion-card-dates';

  const inicioItem = crearDateItem('Inicio', formatearFecha(s.fechaInicio));
  const finItem = crearDateItem('Finalizó', formatearFecha(s.fechaFin));
  datesEl.append(inicioItem, finItem);

  card.append(header, datesEl);

  return card;
}

function crearDateItem(label, valor) {
  const item = document.createElement('div');
  item.className = 'suscripcion-date-item';

  const labelEl = document.createElement('span');
  labelEl.className = 'suscripcion-date-label';
  labelEl.textContent = label;

  const valueEl = document.createElement('span');
  valueEl.className = 'suscripcion-date-value';
  valueEl.textContent = valor;

  item.append(labelEl, valueEl);
  return item;
}

// ================== CANCEL SUBSCRIPTION ==================
async function cancelarSuscripcion(suscripcionId, cardEl) {
  if (!confirm('¿Estás seguro de que quieres cancelar esta suscripción? Esta acción no se puede deshacer.')) {
    return;
  }

  try {
    mostrarResultado(messageEl, 'Cancelando suscripción...', 'loading');

    const response = await fetch(`${API_BASE_URL}/suscripciones/${suscripcionId}`, {
      method: 'DELETE',
      headers: getAuthHeaders(),
    });

    if (!response.ok) {
      const err = await response.json().catch(() => ({}));
      throw new Error(err.message || 'Error al cancelar la suscripción');
    }

    mostrarResultado(messageEl, '✅ Suscripción cancelada correctamente.', 'success');
    await cargarMisSuscripciones();
  } catch (error) {
    console.error('Error al cancelar suscripción:', error);
    mostrarResultado(messageEl, `❌ ${error.message}`, 'error');
  }
}

// ================== REFUND ==================
async function solicitarReembolso(suscripcionId) {
  if (!confirm('¿Deseas solicitar el reembolso de esta suscripción? La suscripción será cancelada y se procesará el reembolso.')) {
    return;
  }

  try {
    mostrarResultado(messageEl, 'Procesando solicitud de reembolso...', 'loading');

    const response = await fetch(`${API_BASE_URL}/suscripciones/${suscripcionId}/reembolsar`, {
      method: 'POST',
      headers: getAuthHeaders(),
    });

    const data = await response.json().catch(() => ({}));

    if (!response.ok) {
      throw new Error(data.message || 'Error al procesar el reembolso');
    }

    mostrarResultado(messageEl, '✅ Reembolso solicitado correctamente. El monto será acreditado en tu cuenta.', 'success');
    await cargarMisSuscripciones();
  } catch (error) {
    console.error('Error al solicitar reembolso:', error);
    mostrarResultado(messageEl, `❌ ${error.message}`, 'error');
  }
}

// ================== EXCLUSIVE RECIPES MODAL ==================
async function abrirRecetasExclusivas(suscripcion) {
  recetasExclusivasTitle.textContent = `Recetas exclusivas: ${suscripcion.nombrePlan || 'Plan'}`;
  recetasExclusivasLoading.classList.remove('hidden');
  recetasExclusivasError.classList.add('hidden');
  recetasExclusivasList.replaceChildren();
  recetasExclusivasModal.classList.remove('hidden');

  try {
    // Fetch plan details to get recetaIds and creadorId
    const planResponse = await fetch(`${API_BASE_URL}/planes/${suscripcion.planId}`, {
      headers: getAuthHeaders(),
    });

    if (!planResponse.ok) throw new Error('No se pudo cargar el plan');

    const planJson = await planResponse.json();
    const plan = planJson.data || planJson;
    const recetaIds = Array.isArray(plan.recetaIds) ? plan.recetaIds : [];
    const creadorId = plan.creadorId;

    if (recetaIds.length === 0) {
      recetasExclusivasLoading.classList.add('hidden');
      renderRecetasExclusivasVacio('Este plan no tiene recetas asignadas.');
      return;
    }

    // Fetch recipes by creator and filter to those in the plan
    const recetasResponse = await fetch(
      `${API_BASE_URL}/recetas/usuario/${creadorId}`,
      { headers: getAuthHeaders() }
    );

    if (!recetasResponse.ok) throw new Error('No se pudieron cargar las recetas');

    const recetasData = await recetasResponse.json();
    const todasRecetas = Array.isArray(recetasData)
      ? recetasData
      : (recetasData.content || []);

    const idsEnPlan = new Set(recetaIds.map(String));
    const recetasDelPlan = todasRecetas.filter((r) => idsEnPlan.has(String(r.id)));

    recetasExclusivasLoading.classList.add('hidden');

    if (recetasDelPlan.length === 0) {
      renderRecetasExclusivasVacio('No se encontraron las recetas de este plan.');
      return;
    }

    recetasDelPlan.forEach((receta) => {
      recetasExclusivasList.appendChild(crearCardRecetaExclusiva(receta));
    });
  } catch (error) {
    console.error('Error al cargar recetas exclusivas:', error);
    recetasExclusivasLoading.classList.add('hidden');
    recetasExclusivasError.textContent = `❌ ${error.message}`;
    recetasExclusivasError.classList.remove('hidden');
  }
}

function renderRecetasExclusivasVacio(mensaje) {
  const empty = document.createElement('p');
  empty.className = 'recetas-exclusivas-empty';
  empty.textContent = mensaje;
  recetasExclusivasList.appendChild(empty);
}

function crearCardRecetaExclusiva(receta) {
  const card = document.createElement('article');
  card.className = 'receta-exclusiva-card';

  const img = document.createElement('img');
  img.src = receta.imagenUrl || 'https://via.placeholder.com/300x130?text=Receta';
  img.alt = receta.nombre || 'Receta';
  img.width = 300;
  img.height = 130;
  img.loading = 'lazy';

  const body = document.createElement('div');
  body.className = 'receta-exclusiva-card-body';

  const nameEl = document.createElement('span');
  nameEl.className = 'receta-exclusiva-name';
  nameEl.textContent = receta.nombre || 'Receta sin nombre';

  const descEl = document.createElement('p');
  descEl.className = 'receta-exclusiva-desc';
  descEl.textContent = receta.descripcion || '';

  const verBtn = document.createElement('button');
  verBtn.className = 'ver-receta-detalle-btn';
  verBtn.type = 'button';
  verBtn.textContent = '👁 Ver receta';
  verBtn.setAttribute('aria-label', `Ver detalle de ${receta.nombre || 'receta'}`);
  verBtn.addEventListener('click', () => abrirDetalleReceta(receta));

  body.append(nameEl, descEl, verBtn);
  card.append(img, body);

  return card;
}

async function abrirDetalleReceta(receta) {
  try {
    const accesoResponse = await fetch(
      `${API_BASE_URL}/suscripciones/acceso/receta/${receta.id}`,
      { headers: getAuthHeaders() },
    );
    const accesoData = await accesoResponse.json();

    if (accesoData.data === false) {
      recetasExclusivasError.textContent =
        'Tu suscripción no está activa. No puedes ver esta receta.';
      recetasExclusivasError.classList.remove('hidden');
      return;
    }

    recetasExclusivasError.classList.add('hidden');
    abrirModalDetalleReceta(receta);
  } catch (error) {
    console.error('Error al verificar acceso a receta:', error);
    recetasExclusivasError.textContent = '❌ Error al verificar el acceso a la receta.';
    recetasExclusivasError.classList.remove('hidden');
  }
}

function abrirModalDetalleReceta(receta) {
  recetaDetalleTitle.textContent = receta.nombre || 'Receta sin nombre';
  recetaDetalleDesc.textContent = receta.descripcion || '';
  recetaDetalleImg.src =
    receta.imagenUrl || 'https://via.placeholder.com/500x300?text=Sin+Imagen';

  recetaDetalleIngredientes.replaceChildren();
  const ingredientesRaw = receta.ingredientes || '';
  const ingredientes =
    typeof ingredientesRaw === 'string'
      ? ingredientesRaw.split('\n').filter((i) => i.trim() !== '')
      : ingredientesRaw;
  ingredientes.forEach((ing) => {
    const li = document.createElement('li');
    li.textContent = ing.trim();
    recetaDetalleIngredientes.appendChild(li);
  });

  recetaDetalleInstrucciones.textContent = receta.instrucciones || 'Sin instrucciones';
  recetaDetalleModal.classList.remove('hidden');
}

// ================== MODAL CLOSE ==================
closeRecetasExclusivasModal.addEventListener('click', () => {
  recetasExclusivasModal.classList.add('hidden');
});

recetasExclusivasModal.addEventListener('click', (e) => {
  if (e.target === recetasExclusivasModal) {
    recetasExclusivasModal.classList.add('hidden');
  }
});

closeRecetaDetalleModal?.addEventListener('click', () => {
  recetaDetalleModal.classList.add('hidden');
});

recetaDetalleModal?.addEventListener('click', (e) => {
  if (e.target === recetaDetalleModal) {
    recetaDetalleModal.classList.add('hidden');
  }
});

document.addEventListener('keydown', (e) => {
  if (e.key === 'Escape') {
    if (!recetaDetalleModal?.classList.contains('hidden')) {
      recetaDetalleModal.classList.add('hidden');
    } else if (!recetasExclusivasModal.classList.contains('hidden')) {
      recetasExclusivasModal.classList.add('hidden');
    }
  }
});

// ================== INICIALIZACIÓN ==================
async function initApp() {
  const token = localStorage.getItem('token');
  const usuarioRaw = localStorage.getItem('usuario');

  if (!token || !usuarioRaw) {
    window.location.href = 'index.html';
    return;
  }

  const usuario = JSON.parse(usuarioRaw);

  const container = document.querySelector('.container');
  if (container) {
    construirHeader(container, usuario);
  }

  await cargarMisSuscripciones();
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', initApp);
} else {
  initApp();
}
