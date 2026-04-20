// ================== CONFIG ==================
const API_BASE_URL =
  window.APP_CONFIG.API_BASE_URL || `${window.location.origin}/api`;

// ================== STATE ==================
let planEnGestion = null; // plan whose recipes are being managed

// ================== DOM REFS ==================
const messageEl = document.getElementById('message');
const loadingEl = document.getElementById('loading');
const misPlanesGrid = document.getElementById('misPlanesGrid');
const dashboardLoadingEl = document.getElementById('dashboardLoading');
const dashboardErrorEl = document.getElementById('dashboardError');
const dashboardMetricsEl = document.getElementById('dashboardMetrics');
const metricsTableEl = document.getElementById('metricsTable');

// Create plan modal
const crearPlanModal = document.getElementById('crearPlanModal');
const closeCrearPlanModal = document.getElementById('closeCrearPlanModal');
const cancelCrearPlanBtn = document.getElementById('cancelCrearPlanBtn');
const crearPlanForm = document.getElementById('crearPlanForm');
const crearPlanResult = document.getElementById('crearPlanResult');
const crearPlanBtn = document.getElementById('crearPlanBtn');

// Edit plan modal
const editarPlanModal = document.getElementById('editarPlanModal');
const closeEditarPlanModal = document.getElementById('closeEditarPlanModal');
const cancelEditarPlanBtn = document.getElementById('cancelEditarPlanBtn');
const editarPlanForm = document.getElementById('editarPlanForm');
const editarPlanResult = document.getElementById('editarPlanResult');

// Manage recipes modal
const gestionarRecetasModal = document.getElementById('gestionarRecetasModal');
const closeGestionarRecetasModal = document.getElementById('closeGestionarRecetasModal');
const gestionarRecetasResult = document.getElementById('gestionarRecetasResult');
const recetasEnPlanLoading = document.getElementById('recetasEnPlanLoading');
const recetasEnPlanList = document.getElementById('recetasEnPlanList');
const recetasDisponiblesLoading = document.getElementById('recetasDisponiblesLoading');
const recetasDisponiblesList = document.getElementById('recetasDisponiblesList');
const asignarSeleccionadasContainer = document.getElementById('asignarSeleccionadasContainer');
const asignarSeleccionadasBtn = document.getElementById('asignarSeleccionadasBtn');

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

function mostrarResultado(element, mensaje, tipo) {
  element.textContent = mensaje;
  element.className = `result ${tipo}`;
  element.setAttribute('role', tipo === 'error' ? 'alert' : 'status');
  element.setAttribute('aria-live', 'polite');
  element.classList.remove('hidden');
}

function ocultarResultado(element) {
  element.classList.add('hidden');
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

// ================== DASHBOARD ==================
async function cargarDashboard(creadorId) {
  try {
    dashboardLoadingEl.classList.remove('hidden');
    dashboardErrorEl.classList.add('hidden');
    dashboardMetricsEl.replaceChildren();
    metricsTableEl.classList.add('hidden');

    const response = await fetch(`${API_BASE_URL}/planes/dashboard`, {
      headers: getAuthHeaders(),
    });

    if (!response.ok) throw new Error('Error al cargar dashboard');

    const data = await response.json();

    renderDashboardMetrics(data);
    if (data.metricasPorPlan && data.metricasPorPlan.length > 0) {
      renderMetricasTabla(data.metricasPorPlan);
    }
  } catch (error) {
    console.error('Error al cargar dashboard:', error);
    mostrarResultado(dashboardErrorEl, 'No se pudieron cargar las métricas del dashboard.', 'error');
  } finally {
    dashboardLoadingEl.classList.add('hidden');
  }
}

function renderDashboardMetrics(data) {
  const metricas = [
    {
      icon: '💰',
      label: 'Ingresos totales',
      value: formatearPrecio(data.ingresosTotales || 0),
    },
    {
      icon: '👥',
      label: 'Suscriptores activos',
      value: String(data.suscriptoresTotalesActivos || 0),
    },
    {
      icon: '📋',
      label: 'Planes activos',
      value: String(data.totalPlanes || 0),
    },
  ];

  dashboardMetricsEl.replaceChildren();
  metricas.forEach(({ icon, label, value }) => {
    const card = document.createElement('div');
    card.className = 'metric-card';

    const iconEl = document.createElement('span');
    iconEl.className = 'metric-icon';
    iconEl.setAttribute('aria-hidden', 'true');
    iconEl.textContent = icon;

    const valueEl = document.createElement('span');
    valueEl.className = 'metric-value';
    valueEl.textContent = value;

    const labelEl = document.createElement('span');
    labelEl.className = 'metric-label';
    labelEl.textContent = label;

    card.append(iconEl, valueEl, labelEl);
    dashboardMetricsEl.appendChild(card);
  });
}

function renderMetricasTabla(metricas) {
  metricsTableEl.replaceChildren();

  const title = document.createElement('h3');
  title.textContent = 'Métricas por plan';
  title.style.marginBottom = '12px';

  const table = document.createElement('table');
  table.className = 'metrics-table';
  table.setAttribute('role', 'table');

  const thead = document.createElement('thead');
  const headerRow = document.createElement('tr');
  ['Plan', 'Suscriptores', 'Ingresos'].forEach((col) => {
    const th = document.createElement('th');
    th.scope = 'col';
    th.textContent = col;
    headerRow.appendChild(th);
  });
  thead.appendChild(headerRow);

  const tbody = document.createElement('tbody');
  metricas.forEach((m) => {
    const tr = document.createElement('tr');

    const tdNombre = document.createElement('td');
    tdNombre.textContent = m.nombrePlan || `Plan #${m.planId}`;

    const tdSubs = document.createElement('td');
    tdSubs.textContent = String(m.suscriptoresActivos || 0);

    const tdIngresos = document.createElement('td');
    tdIngresos.textContent = formatearPrecio(m.ingresos || 0);

    tr.append(tdNombre, tdSubs, tdIngresos);
    tbody.appendChild(tr);
  });

  table.append(thead, tbody);
  metricsTableEl.append(title, table);
  metricsTableEl.classList.remove('hidden');
}

// ================== MIS PLANES ==================
async function cargarMisPlanes(creadorId) {
  try {
    loadingEl.classList.remove('hidden');
    misPlanesGrid.replaceChildren();
    messageEl.classList.add('hidden');

    const response = await fetch(`${API_BASE_URL}/planes/creador/${creadorId}`, {
      headers: getAuthHeaders(),
    });

    if (!response.ok) throw new Error('Error al cargar tus planes');

    const planes = await response.json();

    if (!planes || planes.length === 0) {
      mostrarResultado(messageEl, 'Aún no has creado ningún plan. ¡Crea tu primer plan!', 'loading');
      return;
    }

    planes.forEach((plan) => {
      misPlanesGrid.appendChild(crearItemPlan(plan));
    });
  } catch (error) {
    console.error('Error al cargar planes:', error);
    mostrarResultado(messageEl, 'Error al cargar tus planes. Intenta de nuevo.', 'error');
  } finally {
    loadingEl.classList.add('hidden');
  }
}

function crearItemPlan(plan) {
  const item = document.createElement('article');
  item.className = 'plan-item';
  item.dataset.planId = plan.id;

  const infoDiv = document.createElement('div');
  infoDiv.className = 'plan-item-info';

  const nombreEl = document.createElement('div');
  nombreEl.className = 'plan-item-name';
  nombreEl.textContent = plan.nombre || 'Plan sin nombre';

  const estadoBadge = document.createElement('span');
  estadoBadge.className = `plan-status-badge ${plan.estado === 'ACTIVO' ? 'badge-activo' : 'badge-inactivo'}`;
  estadoBadge.textContent = plan.estado || 'ACTIVO';
  nombreEl.appendChild(estadoBadge);

  const precioEl = document.createElement('div');
  precioEl.className = 'plan-item-price';
  precioEl.textContent = formatearPrecio(plan.precio || 0);

  const metaEl = document.createElement('div');
  metaEl.className = 'plan-item-meta';

  const numRecetas = Array.isArray(plan.recetaIds) ? plan.recetaIds.length : 0;
  metaEl.innerHTML = `
    <span>⏱ Duración: ${plan.duracionDias || 0} días</span>
    <span>📖 ${numRecetas} receta${numRecetas !== 1 ? 's' : ''}</span>
    <span>👥 ${plan.suscriptoresActivos || 0} suscriptor${(plan.suscriptoresActivos || 0) !== 1 ? 'es' : ''} activo${(plan.suscriptoresActivos || 0) !== 1 ? 's' : ''}</span>
  `;

  infoDiv.append(nombreEl, precioEl, metaEl);

  const accionesDiv = document.createElement('div');
  accionesDiv.className = 'plan-item-actions';

  const editarBtn = document.createElement('button');
  editarBtn.className = 'edit-btn';
  editarBtn.type = 'button';
  editarBtn.textContent = '✏️ Editar';
  editarBtn.setAttribute('aria-label', `Editar plan ${plan.nombre}`);
  editarBtn.addEventListener('click', () => abrirModalEditar(plan));

  const desactivarBtn = document.createElement('button');
  desactivarBtn.className = 'delete-btn';
  desactivarBtn.type = 'button';
  desactivarBtn.textContent = '🗑️ Desactivar';
  desactivarBtn.setAttribute('aria-label', `Desactivar plan ${plan.nombre}`);
  desactivarBtn.addEventListener('click', () => desactivarPlan(plan.id, item));

  const gestionarBtn = document.createElement('button');
  gestionarBtn.className = 'gestionar-btn';
  gestionarBtn.type = 'button';
  gestionarBtn.textContent = '📝 Gestionar recetas';
  gestionarBtn.setAttribute('aria-label', `Gestionar recetas del plan ${plan.nombre}`);
  gestionarBtn.addEventListener('click', () => abrirModalGestionarRecetas(plan));

  accionesDiv.append(editarBtn, desactivarBtn, gestionarBtn);
  item.append(infoDiv, accionesDiv);

  return item;
}

// ================== CREAR PLAN ==================
crearPlanBtn.addEventListener('click', () => {
  crearPlanForm.reset();
  ocultarResultado(crearPlanResult);
  crearPlanModal.classList.remove('hidden');
});

closeCrearPlanModal.addEventListener('click', () => cerrarModal(crearPlanModal));
cancelCrearPlanBtn.addEventListener('click', () => cerrarModal(crearPlanModal));

crearPlanForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  const submitBtn = crearPlanForm.querySelector('button[type="submit"]');

  const nombre = document.getElementById('crearNombre').value.trim();
  const descripcion = document.getElementById('crearDescripcion').value.trim();
  const precio = parseFloat(document.getElementById('crearPrecio').value);
  const duracionDias = parseInt(document.getElementById('crearDuracion').value, 10);

  if (!nombre || !descripcion || isNaN(precio) || isNaN(duracionDias)) {
    mostrarResultado(crearPlanResult, 'Todos los campos son obligatorios.', 'error');
    return;
  }

  try {
    submitBtn.disabled = true;
    mostrarResultado(crearPlanResult, 'Creando plan...', 'loading');

    const response = await fetch(`${API_BASE_URL}/planes`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify({ nombre, descripcion, precio, duracionDias }),
    });

    if (!response.ok) {
      const err = await response.json().catch(() => ({}));
      throw new Error(err.message || 'Error al crear el plan');
    }

    mostrarResultado(crearPlanResult, '✅ Plan creado correctamente.', 'success');

    setTimeout(async () => {
      cerrarModal(crearPlanModal);
      const usuario = JSON.parse(localStorage.getItem('usuario'));
      await Promise.all([cargarMisPlanes(usuario.id), cargarDashboard(usuario.id)]);
    }, 1200);
  } catch (error) {
    console.error('Error al crear plan:', error);
    mostrarResultado(crearPlanResult, `❌ ${error.message}`, 'error');
  } finally {
    submitBtn.disabled = false;
  }
});

// ================== EDITAR PLAN ==================
function abrirModalEditar(plan) {
  document.getElementById('editarPlanId').value = plan.id;
  document.getElementById('editarNombre').value = plan.nombre || '';
  document.getElementById('editarDescripcion').value = plan.descripcion || '';
  document.getElementById('editarPrecio').value = plan.precio || '';
  document.getElementById('editarDuracion').value = plan.duracionDias || '';
  ocultarResultado(editarPlanResult);
  editarPlanModal.classList.remove('hidden');
}

closeEditarPlanModal.addEventListener('click', () => cerrarModal(editarPlanModal));
cancelEditarPlanBtn.addEventListener('click', () => cerrarModal(editarPlanModal));

editarPlanForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  const submitBtn = editarPlanForm.querySelector('button[type="submit"]');
  const planId = document.getElementById('editarPlanId').value;

  const nombre = document.getElementById('editarNombre').value.trim();
  const descripcion = document.getElementById('editarDescripcion').value.trim();
  const precio = parseFloat(document.getElementById('editarPrecio').value);
  const duracionDias = parseInt(document.getElementById('editarDuracion').value, 10);

  if (!nombre || !descripcion || isNaN(precio) || isNaN(duracionDias)) {
    mostrarResultado(editarPlanResult, 'Todos los campos son obligatorios.', 'error');
    return;
  }

  try {
    submitBtn.disabled = true;
    mostrarResultado(editarPlanResult, 'Guardando cambios...', 'loading');

    const response = await fetch(`${API_BASE_URL}/planes/${planId}`, {
      method: 'PUT',
      headers: getAuthHeaders(),
      body: JSON.stringify({ nombre, descripcion, precio, duracionDias }),
    });

    if (!response.ok) {
      const err = await response.json().catch(() => ({}));
      throw new Error(err.message || 'Error al editar el plan');
    }

    mostrarResultado(editarPlanResult, '✅ Plan actualizado correctamente.', 'success');

    setTimeout(async () => {
      cerrarModal(editarPlanModal);
      const usuario = JSON.parse(localStorage.getItem('usuario'));
      await Promise.all([cargarMisPlanes(usuario.id), cargarDashboard(usuario.id)]);
    }, 1200);
  } catch (error) {
    console.error('Error al editar plan:', error);
    mostrarResultado(editarPlanResult, `❌ ${error.message}`, 'error');
  } finally {
    submitBtn.disabled = false;
  }
});

// ================== DESACTIVAR PLAN ==================
async function desactivarPlan(planId, itemEl) {
  if (!confirm('¿Seguro que deseas desactivar este plan? Los suscriptores actuales no podrán renovar.')) {
    return;
  }

  try {
    mostrarResultado(messageEl, 'Desactivando plan...', 'loading');

    const response = await fetch(`${API_BASE_URL}/planes/${planId}`, {
      method: 'DELETE',
      headers: getAuthHeaders(),
    });

    if (!response.ok) {
      const err = await response.json().catch(() => ({}));
      throw new Error(err.message || 'Error al desactivar el plan');
    }

    mostrarResultado(messageEl, '✅ Plan desactivado correctamente.', 'success');

    // Update badge in place instead of full reload
    const badge = itemEl.querySelector('.plan-status-badge');
    if (badge) {
      badge.textContent = 'INACTIVO';
      badge.className = 'plan-status-badge badge-inactivo';
    }

    // Reload to get fresh data
    const usuario = JSON.parse(localStorage.getItem('usuario'));
    await Promise.all([cargarMisPlanes(usuario.id), cargarDashboard(usuario.id)]);
  } catch (error) {
    console.error('Error al desactivar plan:', error);
    mostrarResultado(messageEl, `❌ ${error.message}`, 'error');
  }
}

// ================== GESTIONAR RECETAS ==================
async function abrirModalGestionarRecetas(plan) {
  planEnGestion = plan;
  document.getElementById('gestionarRecetasTitle').textContent = `Gestionar recetas: ${plan.nombre}`;
  ocultarResultado(gestionarRecetasResult);
  gestionarRecetasModal.classList.remove('hidden');

  await Promise.all([
    cargarRecetasEnPlan(plan),
    cargarRecetasDisponibles(plan),
  ]);
}

async function cargarRecetasEnPlan(plan) {
  recetasEnPlanLoading.classList.remove('hidden');
  recetasEnPlanList.replaceChildren();

  const recetaIds = Array.isArray(plan.recetaIds) ? plan.recetaIds : [];

  recetasEnPlanLoading.classList.add('hidden');

  if (recetaIds.length === 0) {
    const empty = document.createElement('p');
    empty.className = 'recetas-gestionar-empty';
    empty.textContent = 'Este plan no tiene recetas asignadas.';
    recetasEnPlanList.appendChild(empty);
    return;
  }

  recetaIds.forEach((recetaId) => {
    const item = crearItemRecetaEnPlan(plan.id, recetaId);
    recetasEnPlanList.appendChild(item);
  });
}

function crearItemRecetaEnPlan(planId, recetaId) {
  const item = document.createElement('div');
  item.className = 'receta-gestionar-item';
  item.dataset.recetaId = recetaId;

  const info = document.createElement('div');
  info.className = 'receta-gestionar-item-info';
  const nameEl = document.createElement('span');
  nameEl.className = 'receta-gestionar-item-name';
  nameEl.textContent = `Receta #${recetaId}`;
  info.appendChild(nameEl);

  const removeBtn = document.createElement('button');
  removeBtn.className = 'remove-receta-btn';
  removeBtn.type = 'button';
  removeBtn.textContent = '✕ Quitar';
  removeBtn.setAttribute('aria-label', `Quitar receta ${recetaId} del plan`);
  removeBtn.addEventListener('click', () => quitarRecetaDePlan(planId, recetaId, item));

  item.append(info, removeBtn);
  return item;
}

async function quitarRecetaDePlan(planId, recetaId, itemEl) {
  if (!confirm('¿Quitar esta receta del plan?')) return;

  try {
    mostrarResultado(gestionarRecetasResult, 'Quitando receta...', 'loading');

    const response = await fetch(`${API_BASE_URL}/planes/${planId}/recetas/${recetaId}`, {
      method: 'DELETE',
      headers: getAuthHeaders(),
    });

    if (!response.ok) {
      const err = await response.json().catch(() => ({}));
      throw new Error(err.message || 'Error al quitar la receta');
    }

    itemEl.remove();

    // Update planEnGestion state
    if (planEnGestion) {
      planEnGestion.recetaIds = (planEnGestion.recetaIds || []).filter(
        (id) => String(id) !== String(recetaId)
      );
    }

    mostrarResultado(gestionarRecetasResult, '✅ Receta quitada del plan.', 'success');

    // Refresh available list
    await cargarRecetasDisponibles(planEnGestion);
  } catch (error) {
    console.error('Error al quitar receta:', error);
    mostrarResultado(gestionarRecetasResult, `❌ ${error.message}`, 'error');
  }
}

async function cargarRecetasDisponibles(plan) {
  recetasDisponiblesLoading.classList.remove('hidden');
  recetasDisponiblesList.replaceChildren();
  asignarSeleccionadasContainer.classList.add('hidden');

  try {
    const usuario = JSON.parse(localStorage.getItem('usuario'));
    const response = await fetch(
      `${API_BASE_URL}/recetas/usuario/${usuario.id}`,
      { headers: getAuthHeaders() }
    );

    if (!response.ok) throw new Error('Error al cargar recetas');

    const recetas = await response.json();
    const recetasData = Array.isArray(recetas) ? recetas : (recetas.content || []);

    const idsEnPlan = new Set((plan.recetaIds || []).map(String));
    const disponibles = recetasData.filter((r) => !idsEnPlan.has(String(r.id)));

    recetasDisponiblesLoading.classList.add('hidden');

    if (disponibles.length === 0) {
      const empty = document.createElement('p');
      empty.className = 'recetas-gestionar-empty';
      empty.textContent = 'No tienes más recetas disponibles para agregar.';
      recetasDisponiblesList.appendChild(empty);
      return;
    }

    asignarSeleccionadasContainer.classList.remove('hidden');

    disponibles.forEach((receta) => {
      const item = crearItemRecetaDisponible(receta);
      recetasDisponiblesList.appendChild(item);
    });
  } catch (error) {
    console.error('Error al cargar recetas disponibles:', error);
    recetasDisponiblesLoading.classList.add('hidden');
    const errorEl = document.createElement('p');
    errorEl.className = 'recetas-gestionar-empty';
    errorEl.textContent = 'Error al cargar tus recetas.';
    recetasDisponiblesList.appendChild(errorEl);
  }
}

function crearItemRecetaDisponible(receta) {
  const item = document.createElement('div');
  item.className = 'receta-gestionar-item';
  item.dataset.recetaId = receta.id;

  const checkbox = document.createElement('input');
  checkbox.type = 'checkbox';
  checkbox.id = `receta-check-${receta.id}`;
  checkbox.value = receta.id;
  checkbox.setAttribute('aria-label', `Seleccionar ${receta.nombre}`);

  const info = document.createElement('label');
  info.className = 'receta-gestionar-item-info';
  info.htmlFor = `receta-check-${receta.id}`;

  const nameEl = document.createElement('span');
  nameEl.className = 'receta-gestionar-item-name';
  nameEl.textContent = receta.nombre || `Receta #${receta.id}`;

  const descEl = document.createElement('span');
  descEl.className = 'receta-gestionar-item-desc';
  descEl.textContent = receta.descripcion || '';

  info.append(nameEl, descEl);

  const agregarBtn = document.createElement('button');
  agregarBtn.className = 'agregar-receta-btn';
  agregarBtn.type = 'button';
  agregarBtn.textContent = '+ Agregar';
  agregarBtn.setAttribute('aria-label', `Agregar ${receta.nombre} al plan`);
  agregarBtn.addEventListener('click', () => agregarRecetaAlPlan([receta.id]));

  item.append(checkbox, info, agregarBtn);
  return item;
}

asignarSeleccionadasBtn.addEventListener('click', async () => {
  const checkboxes = recetasDisponiblesList.querySelectorAll(
    'input[type="checkbox"]:checked'
  );
  const ids = Array.from(checkboxes).map((cb) => Number(cb.value));

  if (ids.length === 0) {
    mostrarResultado(gestionarRecetasResult, 'Selecciona al menos una receta.', 'error');
    return;
  }

  await agregarRecetaAlPlan(ids);
});

async function agregarRecetaAlPlan(recetaIds) {
  if (!planEnGestion) return;

  try {
    mostrarResultado(gestionarRecetasResult, 'Asignando recetas...', 'loading');

    const response = await fetch(`${API_BASE_URL}/planes/${planEnGestion.id}/recetas`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify({ recetaIds }),
    });

    if (!response.ok) {
      const err = await response.json().catch(() => ({}));
      throw new Error(err.message || 'Error al asignar recetas');
    }

    // Update local state
    if (!planEnGestion.recetaIds) planEnGestion.recetaIds = [];
    recetaIds.forEach((id) => {
      if (!planEnGestion.recetaIds.includes(id)) {
        planEnGestion.recetaIds.push(id);
      }
    });

    mostrarResultado(gestionarRecetasResult, '✅ Recetas asignadas correctamente.', 'success');

    // Refresh both lists
    await Promise.all([
      cargarRecetasEnPlan(planEnGestion),
      cargarRecetasDisponibles(planEnGestion),
    ]);
  } catch (error) {
    console.error('Error al asignar recetas:', error);
    mostrarResultado(gestionarRecetasResult, `❌ ${error.message}`, 'error');
  }
}

closeGestionarRecetasModal.addEventListener('click', () => {
  cerrarModal(gestionarRecetasModal);
  planEnGestion = null;
  const usuario = JSON.parse(localStorage.getItem('usuario'));
  cargarMisPlanes(usuario.id);
});

// ================== MODAL HELPERS ==================
function cerrarModal(modal) {
  modal.classList.add('hidden');
}

// Close on backdrop click
[crearPlanModal, editarPlanModal, gestionarRecetasModal].forEach((modal) => {
  modal.addEventListener('click', (e) => {
    if (e.target === modal) cerrarModal(modal);
  });
});

document.addEventListener('keydown', (e) => {
  if (e.key === 'Escape') {
    [crearPlanModal, editarPlanModal, gestionarRecetasModal].forEach((m) => {
      if (!m.classList.contains('hidden')) cerrarModal(m);
    });
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

  await Promise.all([
    cargarDashboard(usuario.id),
    cargarMisPlanes(usuario.id),
  ]);
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', initApp);
} else {
  initApp();
}
