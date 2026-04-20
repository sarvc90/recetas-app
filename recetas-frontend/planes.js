// ================== CONFIG ==================
const API_BASE_URL =
  window.APP_CONFIG.API_BASE_URL || `${window.location.origin}/api`;

const PAGE_SIZE = 6;

// ================== STATE ==================
let todosLosPlanes = [];
let planesFiltrados = [];
let paginaActual = 0;
let filtroCreadorId = null;
let filtroNombre = '';

// ================== DOM REFS ==================
const planesGrid = document.getElementById('planesGrid');
const loadingEl = document.getElementById('loading');
const messageEl = document.getElementById('message');
const loadMoreBtn = document.getElementById('loadMore');
const searchInput = document.getElementById('searchInput');
const searchBtn = document.getElementById('searchBtn');
const creatorFilterBanner = document.getElementById('creatorFilterBanner');
const creatorFilterLabel = document.getElementById('creatorFilterLabel');
const clearFilterBtn = document.getElementById('clearFilterBtn');
const planDetailModal = document.getElementById('planDetailModal');
const closePlanDetailModal = document.getElementById('closePlanDetailModal');
const planDetailTitle = document.getElementById('planDetailTitle');
const planDetailBody = document.getElementById('planDetailBody');

// ================== HELPERS ==================
function getAuthHeaders() {
  const token = localStorage.getItem('token');
  return {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${token}`,
  };
}

function formatearPrecio(precio) {
  return new Intl.NumberFormat('es-CO', {
    style: 'currency',
    currency: 'USD',
  }).format(precio);
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
  element.setAttribute('role', 'status');
  element.setAttribute('aria-live', 'polite');
  element.classList.remove('hidden');
}

function leerCreadorUrl() {
  const params = new URLSearchParams(window.location.search);
  return params.get('creadorId');
}

// ================== FETCH PLANES ==================
async function cargarPlanes() {
  try {
    mostrarCargando(true);
    messageEl.classList.add('hidden');

    const response = await fetch(`${API_BASE_URL}/planes`, {
      headers: getAuthHeaders(),
    });

    if (!response.ok) throw new Error('Error al cargar planes');

    const data = await response.json();
    todosLosPlanes = Array.isArray(data) ? data : [];

    aplicarFiltros();
  } catch (error) {
    console.error('Error al cargar planes:', error);
    mostrarResultado(messageEl, 'Error al cargar los planes. Intenta de nuevo.', 'error');
  } finally {
    mostrarCargando(false);
  }
}

function aplicarFiltros() {
  let resultado = [...todosLosPlanes];

  if (filtroCreadorId) {
    resultado = resultado.filter((p) => String(p.creadorId) === String(filtroCreadorId));
  }

  if (filtroNombre.trim()) {
    const termino = filtroNombre.trim().toLowerCase();
    resultado = resultado.filter((p) =>
      (p.nombre || '').toLowerCase().includes(termino)
    );
  }

  planesFiltrados = resultado;
  paginaActual = 0;
  renderPlanesDesde(0, true);
}

function renderPlanesDesde(desde, limpiar) {
  if (limpiar) {
    planesGrid.replaceChildren();
  }

  const hasta = desde + PAGE_SIZE;
  const trozo = planesFiltrados.slice(desde, hasta);

  if (planesFiltrados.length === 0) {
    mostrarResultado(messageEl, 'No se encontraron planes.', 'error');
    loadMoreBtn.classList.add('hidden');
    return;
  }

  messageEl.classList.add('hidden');

  trozo.forEach((plan) => {
    planesGrid.appendChild(crearTarjetaPlan(plan));
  });

  const hayMas = hasta < planesFiltrados.length;
  loadMoreBtn.classList.toggle('hidden', !hayMas);
}

function crearTarjetaPlan(plan) {
  const card = document.createElement('article');
  card.className = 'card';
  card.setAttribute('aria-label', `Plan: ${plan.nombre}`);

  const header = document.createElement('div');
  header.className = 'plan-card-header';

  const nombre = document.createElement('h2');
  nombre.className = 'plan-name';
  nombre.textContent = plan.nombre || 'Plan sin nombre';

  const precio = document.createElement('span');
  precio.className = 'plan-price';
  precio.textContent = formatearPrecio(plan.precio || 0);

  header.append(nombre, precio);

  const descripcion = document.createElement('p');
  descripcion.className = 'plan-description';
  descripcion.textContent = plan.descripcion || 'Sin descripción';

  const meta = document.createElement('div');
  meta.className = 'plan-meta';

  const duracionRow = document.createElement('div');
  duracionRow.className = 'plan-meta-row';
  duracionRow.textContent = `⏱ Duración: ${plan.duracionDias || 0} días`;

  const recetasRow = document.createElement('div');
  recetasRow.className = 'plan-meta-row';
  const numRecetas = Array.isArray(plan.recetaIds) ? plan.recetaIds.length : 0;
  recetasRow.textContent = `📖 ${numRecetas} receta${numRecetas !== 1 ? 's' : ''} exclusiva${numRecetas !== 1 ? 's' : ''}`;

  const suscriptoresRow = document.createElement('div');
  suscriptoresRow.className = 'plan-meta-row';
  suscriptoresRow.textContent = `👥 ${plan.suscriptoresActivos || 0} suscriptor${plan.suscriptoresActivos !== 1 ? 'es' : ''} activo${plan.suscriptoresActivos !== 1 ? 's' : ''}`;

  const creadorRow = document.createElement('div');
  creadorRow.className = 'plan-meta-row';

  const creadorLabel = document.createElement('span');
  creadorLabel.textContent = 'Por: ';

  const creadorBtn = document.createElement('button');
  creadorBtn.className = 'plan-creator-link';
  creadorBtn.type = 'button';
  creadorBtn.textContent = plan.nombreCreador || 'Creador desconocido';
  creadorBtn.setAttribute('aria-label', `Filtrar por creador ${plan.nombreCreador}`);
  creadorBtn.addEventListener('click', (e) => {
    e.stopPropagation();
    aplicarFiltroCreador(plan.creadorId, plan.nombreCreador);
  });

  creadorRow.append(creadorLabel, creadorBtn);
  meta.append(duracionRow, recetasRow, suscriptoresRow, creadorRow);

  const acciones = document.createElement('div');
  acciones.className = 'card-actions';

  const verDetallesBtn = document.createElement('button');
  verDetallesBtn.className = 'ver-detalles-btn';
  verDetallesBtn.type = 'button';
  verDetallesBtn.textContent = 'Ver detalles';
  verDetallesBtn.addEventListener('click', () => abrirDetallePlan(plan));

  acciones.appendChild(verDetallesBtn);
  card.append(header, descripcion, meta, acciones);

  return card;
}

// ================== FILTRO POR CREADOR ==================
function aplicarFiltroCreador(creadorId, nombreCreador) {
  filtroCreadorId = creadorId;
  creatorFilterLabel.textContent = `Planes de ${nombreCreador}`;
  creatorFilterBanner.classList.remove('hidden');
  aplicarFiltros();
}

clearFilterBtn.addEventListener('click', () => {
  filtroCreadorId = null;
  creatorFilterBanner.classList.add('hidden');
  aplicarFiltros();
});

// ================== BÚSQUEDA ==================
searchBtn.addEventListener('click', () => {
  filtroNombre = searchInput.value;
  aplicarFiltros();
});

searchInput.addEventListener('keypress', (e) => {
  if (e.key === 'Enter') {
    filtroNombre = searchInput.value;
    aplicarFiltros();
  }
});

// ================== CARGAR MÁS ==================
loadMoreBtn.addEventListener('click', () => {
  paginaActual++;
  renderPlanesDesde(paginaActual * PAGE_SIZE, false);
});

// ================== MODAL DETALLE ==================
function abrirDetallePlan(plan) {
  planDetailTitle.textContent = plan.nombre || 'Plan sin nombre';

  planDetailBody.replaceChildren();

  const info = document.createElement('div');
  info.className = 'plan-detail-info';

  const precioEl = document.createElement('div');
  precioEl.className = 'plan-detail-price';
  precioEl.textContent = formatearPrecio(plan.precio || 0);

  const descripcionEl = document.createElement('div');
  descripcionEl.className = 'plan-detail-description';
  descripcionEl.textContent = plan.descripcion || 'Sin descripción';

  const duracionRow = crearDetailRow('Duración:', `${plan.duracionDias || 0} días`);

  const suscriptoresRow = crearDetailRow(
    'Suscriptores activos:',
    String(plan.suscriptoresActivos || 0)
  );

  const creadorRowEl = document.createElement('div');
  creadorRowEl.className = 'plan-detail-row';
  const creadorLabelEl = document.createElement('span');
  creadorLabelEl.className = 'plan-detail-label';
  creadorLabelEl.textContent = 'Creador:';
  const creadorLinkBtn = document.createElement('button');
  creadorLinkBtn.className = 'plan-detail-creator-link';
  creadorLinkBtn.type = 'button';
  creadorLinkBtn.textContent = plan.nombreCreador || 'Desconocido';
  creadorLinkBtn.setAttribute('aria-label', `Filtrar por ${plan.nombreCreador}`);
  creadorLinkBtn.addEventListener('click', () => {
    cerrarModal(planDetailModal);
    aplicarFiltroCreador(plan.creadorId, plan.nombreCreador);
  });
  creadorRowEl.append(creadorLabelEl, creadorLinkBtn);

  const fechaRow = crearDetailRow('Creado:', formatearFecha(plan.fechaCreacion));

  const recetasTitleEl = document.createElement('p');
  recetasTitleEl.className = 'plan-detail-recipes-title';
  const numRecetas = Array.isArray(plan.recetaIds) ? plan.recetaIds.length : 0;
  recetasTitleEl.textContent = `Recetas exclusivas (${numRecetas}):`;

  const recetasList = document.createElement('ul');
  recetasList.className = 'plan-recipes-list';
  recetasList.setAttribute('aria-label', 'Recetas del plan');

  if (numRecetas === 0) {
    const emptyEl = document.createElement('li');
    emptyEl.className = 'plan-recipes-empty';
    emptyEl.textContent = 'Este plan aún no tiene recetas asignadas.';
    recetasList.appendChild(emptyEl);
  } else {
    plan.recetaIds.forEach((rid) => {
      const li = document.createElement('li');
      li.textContent = `Receta #${rid}`;
      recetasList.appendChild(li);
    });
  }

  const suscribirseBtn = document.createElement('button');
  suscribirseBtn.className = 'suscribirse-btn';
  suscribirseBtn.type = 'button';
  suscribirseBtn.textContent = '💳 Suscribirse a este plan';
  suscribirseBtn.addEventListener('click', () => {
    window.location.href = `pago.html?planId=${plan.id}`;
  });

  info.append(
    precioEl,
    descripcionEl,
    duracionRow,
    suscriptoresRow,
    creadorRowEl,
    fechaRow,
    recetasTitleEl,
    recetasList,
    suscribirseBtn
  );

  planDetailBody.appendChild(info);
  planDetailModal.classList.remove('hidden');
  planDetailModal.querySelector('.modal-content').scrollTop = 0;
}

function crearDetailRow(label, valor) {
  const row = document.createElement('div');
  row.className = 'plan-detail-row';
  const labelEl = document.createElement('span');
  labelEl.className = 'plan-detail-label';
  labelEl.textContent = label;
  const valorEl = document.createElement('span');
  valorEl.textContent = valor;
  row.append(labelEl, valorEl);
  return row;
}

function cerrarModal(modal) {
  modal.classList.add('hidden');
}

closePlanDetailModal.addEventListener('click', () => cerrarModal(planDetailModal));

planDetailModal.addEventListener('click', (e) => {
  if (e.target === planDetailModal) cerrarModal(planDetailModal);
});

document.addEventListener('keydown', (e) => {
  if (e.key === 'Escape') {
    cerrarModal(planDetailModal);
  }
});

// ================== LOADING STATE ==================
function mostrarCargando(visible) {
  if (visible) {
    loadingEl.classList.remove('hidden');
  } else {
    loadingEl.classList.add('hidden');
  }
}

// ================== HEADER ==================
function construirHeader(container) {
  const usuario = JSON.parse(localStorage.getItem('usuario'));
  if (!usuario) {
    window.location.href = 'index.html';
    return;
  }

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
    { href: 'mis-recetas.html', label: 'Mis Recetas' },
    { href: 'favoritos.html', label: '❤️ Favoritos' },
    { href: 'editar-perfil.html', label: '✏️ Editar perfil' },
    { href: 'mis-planes.html', label: '📋 Mis Planes' },
    { href: 'mis-suscripciones.html', label: '📜 Mis Suscripciones' },
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

// ================== INICIALIZACIÓN ==================
async function initApp() {
  const token = localStorage.getItem('token');
  if (!token) {
    window.location.href = 'index.html';
    return;
  }

  const container = document.querySelector('.container');
  if (container) {
    construirHeader(container);
  }

  // Leer filtro de creador desde URL
  const creadorIdUrl = leerCreadorUrl();
  if (creadorIdUrl) {
    filtroCreadorId = creadorIdUrl;
    creatorFilterLabel.textContent = `Planes del creador seleccionado`;
    creatorFilterBanner.classList.remove('hidden');
  }

  await cargarPlanes();

  // Si había creadorId en URL y ya tenemos los planes, actualizar el label con el nombre
  if (creadorIdUrl && todosLosPlanes.length > 0) {
    const plan = todosLosPlanes.find((p) => String(p.creadorId) === String(creadorIdUrl));
    if (plan) {
      creatorFilterLabel.textContent = `Planes de ${plan.nombreCreador}`;
    }
  }
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', initApp);
} else {
  initApp();
}
