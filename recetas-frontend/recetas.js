// ================== VARIABLES GLOBALES ==================
const API_URL = "http://localhost:8080/api/recetas"; // Ajusta a tu backend
const API_BASE_URL = "http://localhost:8080/api";
const pageSize = 6;
let currentPage = 0;

const recetasGrid = document.getElementById("recetasGrid");
const loading = document.getElementById("loading");
const message = document.getElementById("message");
const modal = document.getElementById("modal");
const closeModal = document.getElementById("closeModal");
const modalTitle = document.getElementById("modalTitle");
const modalImage = document.getElementById("modalImage");
const modalIngredientes = document.getElementById("modalIngredientes");
const modalInstrucciones = document.getElementById("modalInstrucciones");
const addRecipeBtn = document.getElementById('addRecipeBtn');
const recipeForm = document.getElementById('recipeForm');
const newRecipeForm = document.getElementById('newRecipeForm');
const cancelRecipeBtn = document.getElementById('cancelRecipeBtn');
const imageInput = document.getElementById('recipeImage');
const imagePreview = document.getElementById('imagePreview');
const loadMoreBtn = document.getElementById("loadMoreBtn");

// ================== EVENT LISTENERS ==================
closeModal?.addEventListener("click", () => modal.classList.add("hidden"));
loadMoreBtn?.addEventListener("click", () => {
    currentPage++;
    fetchRecetas(currentPage);
});
addRecipeBtn?.addEventListener('click', () => recipeForm.classList.remove('hidden'));
cancelRecipeBtn?.addEventListener('click', () => {
    recipeForm.classList.add('hidden');
    newRecipeForm.reset();
    imagePreview.innerHTML = '';
});

// ================== VALIDAR FORMULARIO ==================
function validarFormularioReceta() {
    const nombre = document.getElementById('recipeName').value.trim();
    const descripcion = document.getElementById('recipeDescription').value.trim();
    const ingredientes = document.getElementById('recipeIngredients').value.trim();
    const instrucciones = document.getElementById('recipeInstructions').value.trim();

    if (!nombre || !descripcion || !ingredientes || !instrucciones) {
        throw new Error('Todos los campos son obligatorios');
    }

    return { nombre, descripcion, ingredientes, instrucciones };
}

// ================== FETCH RECETAS ==================
async function fetchRecetas(page = 0) {
    try {
        loading.classList.remove("hidden");
        message.classList.add("hidden");

        const response = await fetch(`${API_URL}?page=${page}&size=${pageSize}`);
        if (!response.ok) throw new Error("Error al cargar recetas");

        const data = await response.json();

        // Soporte para Page<Receta> (Spring Boot) o lista simple
        const recetas = data.content || data;

        if (page === 0) recetasGrid.innerHTML = ''; // Limpiar solo en primera página

        if (!recetas || recetas.length === 0) {
            if (page === 0) {
                message.textContent = "No hay recetas disponibles";
                message.classList.remove("hidden");
            }
            loadMoreBtn?.classList.add("hidden");
            return;
        }

        renderRecetas(recetas);
        if (data.last !== undefined) {
            loadMoreBtn.classList.toggle("hidden", data.last);
        }

    } catch (error) {
        console.error('Error al cargar recetas:', error);
        message.textContent = "Error al cargar recetas";
        message.classList.remove("hidden");
    } finally {
        loading.classList.add("hidden");
    }
}

// ================== RENDERIZAR RECETAS ==================
function renderRecetas(recetas) {
    recetas.forEach(receta => {
        const card = document.createElement('div');
        card.className = "card";
        card.dataset.id = receta.id;

        card.innerHTML = `
            <img src="${receta.imagenUrl || 'https://via.placeholder.com/300x200?text=Sin+Imagen'}" 
                 alt="${receta.nombre || 'Receta sin nombre'}">
            <h3>${receta.nombre || 'Receta sin nombre'}</h3>
            <p>${receta.descripcion || 'Sin descripción'}</p>
            <div class="card-actions">
                <button class="edit-btn">✏️ Editar</button>
            </div>
        `;

        // Eventos
        card.querySelector('.edit-btn').addEventListener('click', (e) => {
            e.preventDefault();
            e.stopPropagation();
            prepararFormularioEdicion(receta);
        });

        card.addEventListener("click", (e) => {
            if (!e.target.classList.contains('edit-btn')) {
                openModal(receta);
            }
        });

        recetasGrid.appendChild(card);
    });
}

// ================== PREPARAR FORMULARIO EDICIÓN ==================
function prepararFormularioEdicion(receta) {
    recipeForm.classList.remove('hidden');
    document.getElementById('recipeName').value = receta.nombre || '';
    document.getElementById('recipeDescription').value = receta.descripcion || '';
    document.getElementById('recipeIngredients').value = Array.isArray(receta.ingredientes)
        ? receta.ingredientes.join('\n')
        : receta.ingredientes || '';
    document.getElementById('recipeInstructions').value = receta.instrucciones || '';

    if (receta.imagenUrl) {
        imagePreview.innerHTML = `<img src="${receta.imagenUrl}" alt="Preview">`;
    }

    const submitBtn = newRecipeForm.querySelector('.submit-btn');
    submitBtn.textContent = 'Actualizar Receta';
    newRecipeForm.dataset.editingId = receta.id;
    recipeForm.scrollIntoView({ behavior: 'smooth' });
}

// ================== MODAL ==================
function openModal(receta) {
    modalTitle.textContent = receta.nombre || "Receta sin nombre";
    modalImage.src = receta.imagenUrl || "https://via.placeholder.com/500x300?text=Sin+Imagen";

    modalIngredientes.innerHTML = "";
    let ingredientes = [];
    if (typeof receta.ingredientes === "string") {
        ingredientes = receta.ingredientes.split('\n').filter(ing => ing.trim() !== '');
    } else if (Array.isArray(receta.ingredientes)) {
        ingredientes = receta.ingredientes;
    }

    ingredientes.forEach(ing => {
        const li = document.createElement("li");
        li.textContent = ing.trim();
        modalIngredientes.appendChild(li);
    });

    modalInstrucciones.textContent = receta.instrucciones || "No disponibles";
    modal.classList.remove("hidden");
}

// ================== GUARDAR / ACTUALIZAR RECETA ==================
newRecipeForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const usuario = JSON.parse(localStorage.getItem('usuario'));

    try {
        const datos = validarFormularioReceta();
        const editingId = newRecipeForm.dataset.editingId;

        // Manejar imagen
        if (imageInput.files[0]) {
            const formData = new FormData();
            formData.append('file', imageInput.files[0]);

            const imageResponse = await fetch(`${API_BASE_URL}/recetas/upload-image`, {
                method: 'POST',
                body: formData
            });

            if (!imageResponse.ok) throw new Error("Error al subir la imagen");
            const imageData = await imageResponse.json();
            datos.imagenUrl = imageData.url;
        } else if (editingId) {
            const recetaExistente = document.querySelector(`.card[data-id="${editingId}"] img`);
            if (recetaExistente) datos.imagenUrl = recetaExistente.src;
        }

        if (!editingId && !imageInput.files[0]) {
            throw new Error('La imagen es obligatoria para nuevas recetas');
        }

        datos.usuarioId = usuario.id;
        const endpoint = editingId ? `${API_URL}/${editingId}` : API_URL;
        const method = editingId ? 'PUT' : 'POST';

        const recetaResponse = await fetch(endpoint, {
            method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(datos)
        });

        if (!recetaResponse.ok) throw new Error(`Error al ${editingId ? 'actualizar' : 'crear'} la receta`);

        const recetaActualizada = await recetaResponse.json();

        if (editingId) {
            const card = document.querySelector(`.card[data-id="${editingId}"]`);
            if (card) {
                card.querySelector('h3').textContent = datos.nombre;
                card.querySelector('p').textContent = datos.descripcion;
                if (datos.imagenUrl) card.querySelector('img').src = datos.imagenUrl;

                const recetaModal = { id: editingId, ...datos };

                // Actualizar evento click
                card.onclick = (e) => {
                    if (!e.target.classList.contains('edit-btn')) {
                        openModal(recetaModal);
                    }
                };
            }
        } else {
            renderRecetas([recetaActualizada]);
        }

        // Limpiar formulario
        recipeForm.classList.add('hidden');
        newRecipeForm.reset();
        imagePreview.innerHTML = '';
        delete newRecipeForm.dataset.editingId;
        const submitBtn = newRecipeForm.querySelector('.submit-btn');
        submitBtn.textContent = 'Guardar Receta';

        message.textContent = `✅ Receta ${editingId ? 'actualizada' : 'creada'} correctamente`;
        message.classList.remove('hidden');
        setTimeout(() => message.classList.add('hidden'), 3000);

    } catch (error) {
        console.error(error);
        message.textContent = '❌ Error: ' + error.message;
        message.classList.remove('hidden');
    }
});

// ================== INICIALIZACIÓN ==================
document.addEventListener('DOMContentLoaded', async () => {
    console.log('Iniciando aplicación...');

    const usuario = localStorage.getItem('usuario');
    if (!usuario) {
        window.location.href = 'index.html';
        return;
    }

    const container = document.querySelector('.container');
    if (container) {
        const header = document.createElement('div');
        header.className = 'header';
        header.innerHTML = `
            <div class="user-info">
                <span>Bienvenido, ${JSON.parse(usuario).nombre}</span>
                <button id="logout" class="logout-btn">Cerrar Sesión</button>
            </div>
        `;
        container.insertBefore(header, container.firstChild);

        document.getElementById('logout')?.addEventListener('click', () => {
            localStorage.removeItem('usuario');
            window.location.href = 'index.html';
        });
    }

    await fetchRecetas();
});
