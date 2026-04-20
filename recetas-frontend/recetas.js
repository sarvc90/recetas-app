// ================== VARIABLES GLOBALES ==================
const API_RECETAS_URL =
  window.APP_CONFIG.API_RECETAS_URL || `${window.location.origin}/api/recetas`;
const API_BASE_URL =
  window.APP_CONFIG.API_BASE_URL || `${window.location.origin}/api`;
const pageSize = 6;
let currentPage = 0;
let selectedRating = 0;
let currentRecetaId = null;

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

const recetasGrid = document.getElementById('recetasGrid');
const loading = document.getElementById('loading');
const message = document.getElementById('message');
const modal = document.getElementById('modal');
const closeModal = document.getElementById('closeModal');
const modalTitle = document.getElementById('modalTitle');
const modalDescription = document.getElementById('modalDescription');
const modalImage = document.getElementById('modalImage');
const modalIngredientes = document.getElementById('modalIngredientes');
const modalInstrucciones = document.getElementById('modalInstrucciones');
const addRecipeBtn = document.getElementById('addRecipeBtn');
const recipeForm = document.getElementById('recipeForm');
const newRecipeForm = document.getElementById('newRecipeForm');
const cancelRecipeBtn = document.getElementById('cancelRecipeBtn');
const imageInput = document.getElementById('recipeImage');
const imagePreview = document.getElementById('imagePreview');
const loadMoreBtn = document.getElementById('loadMore');

const favBtn = document.getElementById('favBtn');
let esFavorito = false;
let recetaActual = null;

const commentsList = document.getElementById('commentsList');
const commentText = document.getElementById('commentText');
const submitCommentBtn = document.getElementById('submitComment');
const starsInput = document.querySelectorAll('.star');
const selectedRatingText = document.getElementById('selectedRating');
const ratingStars = document.getElementById('ratingStars');
const ratingText = document.getElementById('ratingText');

function getStoredUsuario() {
  const rawUsuario = localStorage.getItem('usuario');
  if (!rawUsuario) return null;

  try {
    return JSON.parse(rawUsuario);
  } catch (error) {
    console.error('No se pudo leer el usuario guardado:', error);
    return null;
  }
}

// ================== EVENT LISTENERS ==================
closeModal?.addEventListener('click', () => {
  modal.classList.add('hidden');
  resetCommentForm();
});

loadMoreBtn?.addEventListener('click', () => {
  currentPage++;
  fetchRecetas(currentPage);
});

addRecipeBtn?.addEventListener('click', () =>
  recipeForm.classList.remove('hidden'),
);

cancelRecipeBtn?.addEventListener('click', () => {
  recipeForm.classList.add('hidden');
  newRecipeForm.reset();
  imagePreview.replaceChildren();
});

document.getElementById('searchBtn')?.addEventListener('click', buscarRecetas);
document.getElementById('searchInput')?.addEventListener('keypress', (e) => {
  if (e.key === 'Enter') buscarRecetas();
});

favBtn?.addEventListener('click', async () => {
  const usuario = JSON.parse(localStorage.getItem('usuario'));
  if (!usuario) return alert('Inicia sesión para usar favoritos');

  const res = await fetch(
    `${API_BASE_URL}/favoritos/toggle?usuarioId=${usuario.id}&recetaId=${recetaActual.id}`,
    { method: 'POST', headers: getAuthHeaders() },
  );

  const data = await res.json();
  alert(data.message);
  esFavorito = !esFavorito;
  actualizarIconoFav();
});

starsInput.forEach((star) => {
  star.addEventListener('click', () => {
    selectedRating = parseInt(star.dataset.rating);
    updateStarsInput();
  });
  star.addEventListener('keydown', (event) => {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      selectedRating = parseInt(star.dataset.rating);
      updateStarsInput();
    }
  });
  star.addEventListener('mouseenter', () => {
    highlightStars(parseInt(star.dataset.rating));
  });
});

document.querySelector('.stars-input')?.addEventListener('mouseleave', () => {
  updateStarsInput();
});

submitCommentBtn?.addEventListener('click', async () => {
  await submitComment();
});

// ================== FUNCIONES DE COMENTARIOS ==================
function updateStarsInput() {
  starsInput.forEach((star) => {
    const rating = parseInt(star.dataset.rating);
    star.textContent = rating <= selectedRating ? '★' : '☆';
    star.classList.toggle('active', rating <= selectedRating);
  });
  selectedRatingText.textContent =
    selectedRating > 0
      ? `${selectedRating} ${selectedRating === 1 ? 'estrella' : 'estrellas'}`
      : 'No seleccionada';
}

function highlightStars(rating) {
  starsInput.forEach((star) => {
    const starRating = parseInt(star.dataset.rating);
    star.textContent = starRating <= rating ? '★' : '☆';
  });
}

function resetCommentForm() {
  selectedRating = 0;
  if (commentText) commentText.value = '';
  updateStarsInput();
}

async function cargarComentarios(recetaId) {
  try {
    const response = await fetch(`${API_RECETAS_URL}/${recetaId}/comentarios`, {
      headers: getAuthHeaders(),
    });
    if (!response.ok) throw new Error('Error al cargar comentarios');

    const data = await response.json();

    if (data.success) {
      const { comentarios, promedioCalificacion, totalComentarios } = data.data;
      mostrarCalificacionPromedio(promedioCalificacion, totalComentarios);

      if (comentarios && comentarios.length > 0) {
        renderComentarios(comentarios);
      } else if (commentsList) {
        commentsList.replaceChildren();
        const emptyState = document.createElement('p');
        emptyState.className = 'no-comments';
        emptyState.textContent = 'Sé el primero en comentar esta receta';
        commentsList.appendChild(emptyState);
      }
    }
  } catch (error) {
    console.error('Error al cargar comentarios:', error);
    if (commentsList) {
      commentsList.replaceChildren();
      const errorState = document.createElement('p');
      errorState.className = 'no-comments';
      errorState.textContent = 'Error al cargar comentarios';
      commentsList.appendChild(errorState);
    }
  }
}

function mostrarCalificacionPromedio(promedio, total) {
  const estrellas = Math.round(promedio);
  let estrellasHTML = '';
  for (let i = 1; i <= 5; i++) {
    estrellasHTML += i <= estrellas ? '★' : '☆';
  }
  if (ratingStars) ratingStars.textContent = estrellasHTML;
  if (ratingText) {
    ratingText.textContent =
      total > 0
        ? `${promedio.toFixed(1)} / 5 (${total} ${total === 1 ? 'calificación' : 'calificaciones'})`
        : 'Sin calificaciones';
  }
}

function renderComentarios(comentarios) {
  if (!commentsList) return;

  commentsList.replaceChildren();

  if (!comentarios || comentarios.length === 0) {
    const emptyState = document.createElement('p');
    emptyState.className = 'no-comments';
    emptyState.textContent = 'Sé el primero en comentar esta receta';
    commentsList.appendChild(emptyState);
    return;
  }

  const fragment = document.createDocumentFragment();

  comentarios.forEach((comentario) => {
    const fecha = new Date(comentario.fechaCreacion);
    const fechaFormateada = fecha.toLocaleDateString('es-ES', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });

    let estrellasComentario = '';
    for (let i = 1; i <= 5; i++) {
      estrellasComentario += i <= comentario.calificacion ? '★' : '☆';
    }

    const commentItem = document.createElement('article');
    commentItem.className = 'comment-item';

    const commentHeader = document.createElement('div');
    commentHeader.className = 'comment-header';

    const commentAuthor = document.createElement('span');
    commentAuthor.className = 'comment-author';
    commentAuthor.textContent = comentario.nombreUsuario || 'Usuario';

    const commentRating = document.createElement('span');
    commentRating.className = 'comment-rating';
    commentRating.textContent = estrellasComentario;

    const commentTextElement = document.createElement('p');
    commentTextElement.className = 'comment-text';
    commentTextElement.textContent = comentario.texto || '';

    const commentDate = document.createElement('span');
    commentDate.className = 'comment-date';
    commentDate.textContent = fechaFormateada;

    commentHeader.append(commentAuthor, commentRating);
    commentItem.append(commentHeader, commentTextElement, commentDate);
    fragment.appendChild(commentItem);
  });

  commentsList.appendChild(fragment);
}

async function submitComment() {
  const usuario = JSON.parse(localStorage.getItem('usuario'));

  if (!usuario) {
    alert('Debes iniciar sesión para comentar');
    return;
  }

  if (selectedRating === 0) {
    alert('Por favor selecciona una calificación');
    return;
  }

  const texto = commentText ? commentText.value.trim() : '';
  if (!texto) {
    alert('Por favor escribe un comentario');
    return;
  }

  try {
    if (submitCommentBtn) {
      submitCommentBtn.disabled = true;
      submitCommentBtn.textContent = 'Publicando...';
    }

    const response = await fetch(
      `${API_RECETAS_URL}/${currentRecetaId}/comentarios`,
      {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({
          usuarioId: usuario.id,
          texto: texto,
          calificacion: selectedRating,
        }),
      },
    );

    const data = await response.json();

    if (data.success) {
      alert('✅ Comentario publicado correctamente');
      resetCommentForm();
      await cargarComentarios(currentRecetaId);
      const commentForm = document.getElementById('commentForm');
      if (commentForm) commentForm.style.display = 'none';
    } else {
      alert('❌ ' + data.message);
    }
  } catch (error) {
    console.error('Error al publicar comentario:', error);
    alert('❌ Error al publicar el comentario');
  } finally {
    if (submitCommentBtn) {
      submitCommentBtn.disabled = false;
      submitCommentBtn.textContent = 'Publicar Comentario';
    }
  }
}

async function verificarSiYaComento(recetaId, usuarioId) {
  try {
    const response = await fetch(
      `${API_RECETAS_URL}/${recetaId}/comentarios/verificar?usuarioId=${usuarioId}`,
      { headers: getAuthHeaders() },
    );
    const data = await response.json();
    const commentForm = document.getElementById('commentForm');
    if (commentForm) {
      commentForm.style.display = data.data === true ? 'none' : 'block';
    }
  } catch (error) {
    console.error('Error al verificar comentario:', error);
  }
}

// ================== VALIDAR FORMULARIO ==================
function validarFormularioReceta() {
  const nombre = document.getElementById('recipeName').value.trim();
  const descripcion = document.getElementById('recipeDescription').value.trim();
  const ingredientes = document
    .getElementById('recipeIngredients')
    .value.trim();
  const instrucciones = document
    .getElementById('recipeInstructions')
    .value.trim();

  if (!nombre || !descripcion || !ingredientes || !instrucciones) {
    throw new Error('Todos los campos son obligatorios');
  }

  return { nombre, descripcion, ingredientes, instrucciones };
}

// ================== FETCH RECETAS ==================
async function fetchRecetas(page = 0) {
  try {
    loading.classList.remove('hidden');
    message.classList.add('hidden');

    const response = await fetch(
      `${API_RECETAS_URL}?page=${page}&size=${pageSize}`,
    );
    if (!response.ok) throw new Error('Error al cargar recetas');

    const data = await response.json();
    const recetas = data.content || data;

    if (page === 0) recetasGrid.replaceChildren();

    if (!recetas || recetas.length === 0) {
      if (page === 0) {
        message.textContent = 'No hay recetas disponibles';
        message.classList.remove('hidden');
      }
      loadMoreBtn?.classList.add('hidden');
      return;
    }

    renderRecetas(recetas);

    if (data.last !== undefined) {
      loadMoreBtn.classList.toggle('hidden', data.last);
    }

    message.classList.add('hidden');
  } catch (error) {
    console.error('Error al cargar recetas:', error);
    if (page === 0 && recetasGrid.children.length === 0) {
      message.textContent =
        'Error al cargar recetas. Por favor, intenta de nuevo.';
      message.classList.remove('hidden');
    }
  } finally {
    loading.classList.add('hidden');
  }
}

// ================== RENDERIZAR RECETAS ==================
function renderRecetas(recetas) {
  recetas.forEach((receta) => {
    const card = document.createElement('article');
    card.className = 'card';
    card.dataset.id = receta.id;
    card.setAttribute('tabindex', '0');
    card.setAttribute('role', 'button');
    card.setAttribute(
      'aria-label',
      `Ver detalle de ${receta.nombre || 'receta'}`,
    );

    const imagen = document.createElement('img');
    imagen.src =
      receta.imagenUrl || 'https://via.placeholder.com/300x200?text=Sin+Imagen';
    imagen.alt = receta.nombre || 'Receta sin nombre';
    imagen.width = 300;
    imagen.height = 200;
    imagen.loading = 'lazy';

    const titulo = document.createElement('h3');
    titulo.textContent = receta.nombre || 'Receta sin nombre';

    const descripcion = document.createElement('p');
    descripcion.textContent = receta.descripcion || 'Sin descripción';

    card.append(imagen, titulo, descripcion);

    if (receta.esExclusiva === true) {
      const badge = document.createElement('span');
      badge.className = 'exclusive-badge';
      badge.textContent = '🔒 Exclusiva';
      card.appendChild(badge);
    }

    card.addEventListener('click', () => openModal(receta));
    card.addEventListener('keydown', (event) => {
      if (event.key === 'Enter' || event.key === ' ') {
        event.preventDefault();
        openModal(receta);
      }
    });
    recetasGrid.appendChild(card);
  });
}

// ================== MODAL ==================
async function openModal(receta) {
  const usuario = JSON.parse(localStorage.getItem('usuario'));

  if (receta.esExclusiva === true && usuario && receta.usuarioId !== usuario.id) {
    try {
      const token = localStorage.getItem('token');
      const accesoResponse = await fetch(
        `${API_BASE_URL}/suscripciones/acceso/receta/${receta.id}`,
        {
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${token}`,
          },
        }
      );
      const accesoData = await accesoResponse.json();
      if (!accesoData.data) {
        window.location.href = `acceso-denegado.html?recetaId=${receta.id}`;
        return;
      }
    } catch (error) {
      console.error('Error al verificar acceso a receta exclusiva:', error);
      window.location.href = `acceso-denegado.html?recetaId=${receta.id}`;
      return;
    }
  }

  modalTitle.textContent = receta.nombre || 'Receta sin nombre';
  modalDescription.textContent = receta.descripcion || 'Sin descripción';
  modalImage.src =
    receta.imagenUrl || 'https://via.placeholder.com/500x300?text=Sin+Imagen';

  modalIngredientes.replaceChildren();
  let ingredientes = [];
  if (typeof receta.ingredientes === 'string') {
    ingredientes = receta.ingredientes
      .split('\n')
      .filter((ing) => ing.trim() !== '');
  } else if (Array.isArray(receta.ingredientes)) {
    ingredientes = receta.ingredientes;
  }

  ingredientes.forEach((ing) => {
    const li = document.createElement('li');
    li.textContent = ing.trim();
    modalIngredientes.appendChild(li);
  });

  modalInstrucciones.textContent = receta.instrucciones || 'No disponibles';
  recetaActual = receta;
  currentRecetaId = receta.id;

  if (usuario) {
    verificarFavorito(usuario.id, receta.id);
    verificarSiYaComento(receta.id, usuario.id);
  } else {
    const commentForm = document.getElementById('commentForm');
    if (commentForm) commentForm.style.display = 'none';
  }

  cargarComentarios(receta.id);
  resetCommentForm();
  modal.classList.remove('hidden');
}

// ================== FAVORITOS ==================
async function verificarFavorito(usuarioId, recetaId) {
  try {
    const res = await fetch(
      `${API_BASE_URL}/favoritos/usuario/${usuarioId}/receta/${recetaId}`,
      { headers: getAuthHeaders() },
    );
    const data = await res.json();
    esFavorito = data.data;
    actualizarIconoFav();
  } catch (error) {
    console.error('Error al verificar favorito:', error);
  }
}

function actualizarIconoFav() {
  if (favBtn) {
    favBtn.textContent = esFavorito
      ? '❤️ Quitar de Favoritos'
      : '🤍 Agregar a Favoritos';
  }
}

// ================== GUARDAR NUEVA RECETA ==================
newRecipeForm?.addEventListener('submit', async (e) => {
  e.preventDefault();
  const usuario = JSON.parse(localStorage.getItem('usuario'));

  try {
    const datos = validarFormularioReceta();

    if (imageInput.files[0]) {
      const formData = new FormData();
      formData.append('file', imageInput.files[0]);

      const imageResponse = await fetch(
        `${API_BASE_URL}/recetas/upload-image`,
        {
          method: 'POST',
          headers: getAuthHeadersMultipart(),
          body: formData,
        },
      );

      if (!imageResponse.ok) throw new Error('Error al subir la imagen');
      const imageData = await imageResponse.json();
      datos.imagenUrl = imageData.url;
    } else {
      throw new Error('La imagen es obligatoria para nuevas recetas');
    }

    datos.usuarioId = usuario.id;

    const recetaResponse = await fetch(API_RECETAS_URL, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify(datos),
    });

    if (!recetaResponse.ok) throw new Error('Error al crear la receta');

    const recetaActualizada = await recetaResponse.json();
    renderRecetas([recetaActualizada]);

    recipeForm.classList.add('hidden');
    newRecipeForm.reset();
    imagePreview.replaceChildren();

    message.textContent = '✅ Receta creada correctamente';
    message.classList.remove('hidden');
    setTimeout(() => message.classList.add('hidden'), 3000);
  } catch (error) {
    console.error(error);
    message.textContent = '❌ Error: ' + error.message;
    message.classList.remove('hidden');
  }
});

// ================== BÚSQUEDA ==================
async function buscarRecetas() {
  const searchInput = document.getElementById('searchInput');
  const loading = document.getElementById('loading');
  const message = document.getElementById('message');
  const recetasGrid = document.getElementById('recetasGrid');
  const loadMoreBtn = document.getElementById('loadMore');

  if (!searchInput || !loading || !message || !recetasGrid) {
    console.error('Elementos DOM no encontrados');
    return;
  }

  const termino = searchInput.value.trim();

  if (!termino) {
    currentPage = 0;
    await fetchRecetas();
    return;
  }

  try {
    loading.classList.remove('hidden');
    message.classList.add('hidden');
    recetasGrid.replaceChildren();

    const response = await fetch(
      `${API_RECETAS_URL}/buscar?termino=${encodeURIComponent(termino)}`,
      { method: 'GET', headers: { Accept: 'application/json' } },
    );

    if (!response.ok) throw new Error('Error al buscar recetas');

    const data = await response.json();
    const recetas = data.data || [];

    if (!recetas || recetas.length === 0) {
      message.textContent =
        'No se encontraron recetas que coincidan con tu búsqueda';
      message.classList.remove('hidden');
      loadMoreBtn?.classList.add('hidden');
      return;
    }

    renderRecetas(recetas);
    loadMoreBtn?.classList.add('hidden');
  } catch (error) {
    console.error('Error en la búsqueda:', error);
    message.textContent =
      'Error al realizar la búsqueda. Por favor, intenta de nuevo.';
    message.classList.remove('hidden');
  } finally {
    loading.classList.add('hidden');
  }
}

// ================== INICIALIZACIÓN ==================
async function initApp() {
  console.log('Iniciando aplicación...');

  const usuarioData = getStoredUsuario();
  if (!usuarioData) {
    window.location.href = 'index.html';
    return;
  }

  const fotoPerfil =
    usuarioData.fotoPerfil || localStorage.getItem('usuarioFoto');

  const container = document.querySelector('.container');
  if (container) {
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
      userPhoto.width = 56;
      userPhoto.height = 56;
      userProfile.appendChild(userPhoto);
    } else {
      const placeholder = document.createElement('div');
      placeholder.className = 'user-photo-placeholder';
      const fallback = document.createElement('img');
      fallback.src = 'icon.png';
      fallback.alt = 'Usuario';
      fallback.width = 56;
      fallback.height = 56;
      fallback.style.width = '70%';
      fallback.style.height = '70%';
      fallback.style.objectFit = 'cover';
      placeholder.appendChild(fallback);
      userProfile.appendChild(placeholder);
    }

    const userName = document.createElement('span');
    userName.className = 'user-name';
    userName.textContent = `Bienvenido, ${usuarioData.nombre || 'Usuario'}`;
    userProfile.appendChild(userName);

    const navButtons = document.createElement('nav');
    navButtons.className = 'nav-buttons';
    navButtons.setAttribute('aria-label', 'Acciones de cuenta');

    [
      { href: 'mis-recetas.html', label: 'Mis Recetas' },
      { href: 'favoritos.html', label: '❤️ Favoritos' },
      { href: 'editar-perfil.html', label: '✏️ Editar perfil' },
      { href: 'planes.html', label: '🍽️ Planes' },
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
    navButtons.appendChild(logoutButton);

    userInfo.append(userProfile, navButtons);
    header.appendChild(userInfo);
    container.insertBefore(header, container.firstChild);

    logoutButton.addEventListener('click', () => {
      localStorage.removeItem('usuario');
      localStorage.removeItem('token');
      window.location.href = 'index.html';
    });
  }

  await fetchRecetas();
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', initApp);
} else {
  initApp();
}
