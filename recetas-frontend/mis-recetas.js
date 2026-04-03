const API_RECETAS_URL =
  window.APP_CONFIG.API_RECETAS_URL || `${window.location.origin}/api/recetas`;
const API_BASE_URL =
  window.APP_CONFIG.API_BASE_URL || `${window.location.origin}/api`;
const grid = document.getElementById('misRecetasGrid');
const loading = document.getElementById('loading');
const message = document.getElementById('message');

const modal = document.getElementById('modal');
const closeModal = document.getElementById('closeModal');
const modalTitle = document.getElementById('modalTitle');
const modalDescription = document.getElementById('modalDescription');
const modalImage = document.getElementById('modalImage');
const modalIngredientes = document.getElementById('modalIngredientes');
const modalInstrucciones = document.getElementById('modalInstrucciones');

const editModal = document.getElementById('editModal');
const closeEditModal = document.getElementById('closeEditModal');
const editForm = document.getElementById('editForm');
const editImageInput = document.getElementById('editRecipeImage');
const editImagePreview = document.getElementById('editImagePreview');

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

closeModal?.addEventListener('click', () => modal.classList.add('hidden'));
closeEditModal?.addEventListener('click', () =>
  editModal.classList.add('hidden'),
);

async function initApp() {
  const usuario = JSON.parse(localStorage.getItem('usuario'));
  if (!usuario) {
    window.location.href = 'index.html';
    return;
  }
  await cargarMisRecetas(usuario.id);
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', initApp);
} else {
  initApp();
}

async function cargarMisRecetas(usuarioId) {
  try {
    loading.classList.remove('hidden');
    const response = await fetch(`${API_RECETAS_URL}/usuario/${usuarioId}`, {
      headers: getAuthHeaders(),
    });
    if (!response.ok) throw new Error('Error al cargar tus recetas');
    const recetas = await response.json();
    grid.replaceChildren();
    if (!recetas || recetas.length === 0) {
      message.textContent = 'Aún no has creado recetas 🍰';
      message.classList.remove('hidden');
      return;
    }
    renderMisRecetas(recetas);
  } catch (error) {
    console.error(error);
    message.textContent = '❌ Error al cargar tus recetas';
    message.classList.remove('hidden');
  } finally {
    loading.classList.add('hidden');
  }
}

function renderMisRecetas(recetas) {
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

    const acciones = document.createElement('div');
    acciones.className = 'card-actions';

    const editButton = document.createElement('button');
    editButton.className = 'edit-btn';
    editButton.type = 'button';
    editButton.dataset.id = receta.id;
    editButton.textContent = '✏️ Editar';

    const deleteButton = document.createElement('button');
    deleteButton.className = 'delete-btn';
    deleteButton.type = 'button';
    deleteButton.dataset.id = receta.id;
    deleteButton.textContent = '🗑 Eliminar';

    editButton.addEventListener('click', (e) => {
      e.stopPropagation();
      abrirModalEdicion(receta);
    });

    deleteButton.addEventListener('click', (e) => {
      e.stopPropagation();
      eliminarReceta(receta.id, card);
    });

    acciones.append(editButton, deleteButton);
    card.append(imagen, titulo, acciones);

    card.addEventListener('click', (e) => {
      if (
        !e.target.classList.contains('edit-btn') &&
        !e.target.classList.contains('delete-btn')
      ) {
        abrirModal(receta);
      }
    });

    card.addEventListener('keydown', (event) => {
      if (event.key === 'Enter' || event.key === ' ') {
        event.preventDefault();
        abrirModal(receta);
      }
    });

    grid.appendChild(card);
  });
}

function abrirModal(receta) {
  modalTitle.textContent = receta.nombre;
  modalDescription.textContent = receta.descripcion;
  modalImage.src =
    receta.imagenUrl || 'https://via.placeholder.com/500x300?text=Sin+Imagen';
  modalIngredientes.replaceChildren();
  const ingredientes =
    typeof receta.ingredientes === 'string'
      ? receta.ingredientes.split('\n').filter((ing) => ing.trim() !== '')
      : receta.ingredientes;
  ingredientes.forEach((ing) => {
    const li = document.createElement('li');
    li.textContent = ing.trim();
    modalIngredientes.appendChild(li);
  });
  modalInstrucciones.textContent = receta.instrucciones || 'Sin instrucciones';
  modal.classList.remove('hidden');
}

function abrirModalEdicion(receta) {
  document.getElementById('editRecipeName').value = receta.nombre;
  document.getElementById('editRecipeDescription').value = receta.descripcion;
  document.getElementById('editRecipeIngredients').value =
    typeof receta.ingredientes === 'string'
      ? receta.ingredientes
      : receta.ingredientes.join('\n');
  document.getElementById('editRecipeInstructions').value =
    receta.instrucciones;
  editImagePreview.replaceChildren();
  if (receta.imagenUrl) {
    const previewImage = document.createElement('img');
    previewImage.src = receta.imagenUrl;
    previewImage.alt = 'Preview';
    previewImage.width = 300;
    previewImage.height = 200;
    previewImage.loading = 'lazy';
    editImagePreview.appendChild(previewImage);
  }
  editForm.dataset.recetaId = receta.id;
  editForm.dataset.imagenActual = receta.imagenUrl || '';
  editModal.classList.remove('hidden');
}

editForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  const recetaId = editForm.dataset.recetaId;
  const usuario = JSON.parse(localStorage.getItem('usuario'));
  try {
    const datos = {
      nombre: document.getElementById('editRecipeName').value.trim(),
      descripcion: document
        .getElementById('editRecipeDescription')
        .value.trim(),
      ingredientes: document
        .getElementById('editRecipeIngredients')
        .value.trim(),
      instrucciones: document
        .getElementById('editRecipeInstructions')
        .value.trim(),
      usuarioId: usuario.id,
    };
    if (
      !datos.nombre ||
      !datos.descripcion ||
      !datos.ingredientes ||
      !datos.instrucciones
    ) {
      throw new Error('Todos los campos son obligatorios');
    }
    if (editImageInput.files[0]) {
      const formData = new FormData();
      formData.append('file', editImageInput.files[0]);
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
      datos.imagenUrl = editForm.dataset.imagenActual;
    }
    const response = await fetch(`${API_RECETAS_URL}/${recetaId}`, {
      method: 'PUT',
      headers: getAuthHeaders(),
      body: JSON.stringify(datos),
    });
    if (!response.ok) throw new Error('Error al actualizar la receta');
    editModal.classList.add('hidden');
    editForm.reset();
    editImagePreview.replaceChildren();
    await cargarMisRecetas(usuario.id);
    message.textContent = '✅ Receta actualizada correctamente';
    message.classList.remove('hidden');
    setTimeout(() => message.classList.add('hidden'), 3000);
  } catch (error) {
    console.error(error);
    message.textContent = '❌ Error: ' + error.message;
    message.classList.remove('hidden');
  }
});

editImageInput?.addEventListener('change', (e) => {
  const file = e.target.files[0];
  if (file) {
    const reader = new FileReader();
    reader.onload = (e) => {
      editImagePreview.replaceChildren();
      const previewImage = document.createElement('img');
      previewImage.src = e.target.result;
      previewImage.alt = 'Preview';
      previewImage.width = 300;
      previewImage.height = 200;
      previewImage.loading = 'lazy';
      editImagePreview.appendChild(previewImage);
    };
    reader.readAsDataURL(file);
  }
});

async function eliminarReceta(id, card) {
  if (!confirm('¿Seguro que deseas eliminar esta receta?')) return;
  try {
    const response = await fetch(`${API_RECETAS_URL}/${id}`, {
      method: 'DELETE',
      headers: getAuthHeaders(),
    });
    if (!response.ok) throw new Error('Error al eliminar receta');
    card.remove();
    message.textContent = '✅ Receta eliminada correctamente';
    message.classList.remove('hidden');
    setTimeout(() => message.classList.add('hidden'), 3000);
    if (grid.children.length === 0) {
      message.textContent = 'Aún no has creado recetas 🍰';
      message.classList.remove('hidden');
    }
  } catch (error) {
    console.error(error);
    message.textContent = '❌ No se pudo eliminar la receta';
    message.classList.remove('hidden');
  }
}
