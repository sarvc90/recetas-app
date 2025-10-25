const API_URL = "http://localhost:8080/api/recetas";
const API_BASE_URL = "http://localhost:8080/api";
const grid = document.getElementById("misRecetasGrid");
const loading = document.getElementById("loading");
const message = document.getElementById("message");

// Modal
const modal = document.getElementById("modal");
const closeModal = document.getElementById("closeModal");
const modalTitle = document.getElementById("modalTitle");
const modalDescription = document.getElementById("modalDescription");
const modalImage = document.getElementById("modalImage");
const modalIngredientes = document.getElementById("modalIngredientes");
const modalInstrucciones = document.getElementById("modalInstrucciones");

// Modal de edición
const editModal = document.getElementById("editModal");
const closeEditModal = document.getElementById("closeEditModal");
const editForm = document.getElementById("editForm");
const editImageInput = document.getElementById("editRecipeImage");
const editImagePreview = document.getElementById("editImagePreview");

closeModal?.addEventListener("click", () => modal.classList.add("hidden"));
closeEditModal?.addEventListener("click", () => editModal.classList.add("hidden"));

// ================== INICIALIZACIÓN ==================
document.addEventListener("DOMContentLoaded", async () => {
    const usuario = JSON.parse(localStorage.getItem("usuario"));
    if (!usuario) {
        window.location.href = "index.html";
        return;
    }

    await cargarMisRecetas(usuario.id);
});

// ================== CARGAR RECETAS ==================
async function cargarMisRecetas(usuarioId) {
    try {
        loading.classList.remove("hidden");
        const response = await fetch(`${API_URL}/usuario/${usuarioId}`);
        if (!response.ok) throw new Error("Error al cargar tus recetas");

        const recetas = await response.json();
        grid.innerHTML = "";

        if (!recetas || recetas.length === 0) {
            message.textContent = "Aún no has creado recetas 🍰";
            message.classList.remove("hidden");
            return;
        }

        renderMisRecetas(recetas);

    } catch (error) {
        console.error(error);
        message.textContent = "❌ Error al cargar tus recetas";
        message.classList.remove("hidden");
    } finally {
        loading.classList.add("hidden");
    }
}

// ================== RENDERIZAR ==================
function renderMisRecetas(recetas) {
    recetas.forEach(receta => {
        const card = document.createElement("div");
        card.className = "card";
        card.dataset.id = receta.id;

        card.innerHTML = `
            <img src="${receta.imagenUrl || 'https://via.placeholder.com/300x200?text=Sin+Imagen'}" 
                 alt="${receta.nombre}">
            <h3>${receta.nombre}</h3>
            <div class="card-actions">
                <button class="edit-btn" data-id="${receta.id}">✏️ Editar</button>
                <button class="delete-btn" data-id="${receta.id}">🗑 Eliminar</button>
            </div>
        `;

        // Botón editar
        card.querySelector(".edit-btn").addEventListener("click", (e) => {
            e.stopPropagation();
            abrirModalEdicion(receta);
        });

        // Botón eliminar
        card.querySelector(".delete-btn").addEventListener("click", (e) => {
            e.stopPropagation();
            eliminarReceta(receta.id, card);
        });

        // Click en la tarjeta para ver detalles
        card.addEventListener("click", (e) => {
            if (!e.target.classList.contains('edit-btn') && !e.target.classList.contains('delete-btn')) {
                abrirModal(receta);
            }
        });

        grid.appendChild(card);
    });
}

// ================== MODAL DE DETALLE ==================
function abrirModal(receta) {
    modalTitle.textContent = receta.nombre;
    modalDescription.textContent = receta.descripcion;
    modalImage.src = receta.imagenUrl || "https://via.placeholder.com/500x300?text=Sin+Imagen";

    modalIngredientes.innerHTML = "";
    const ingredientes = typeof receta.ingredientes === "string"
        ? receta.ingredientes.split("\n").filter(ing => ing.trim() !== '')
        : receta.ingredientes;

    ingredientes.forEach(ing => {
        const li = document.createElement("li");
        li.textContent = ing.trim();
        modalIngredientes.appendChild(li);
    });

    modalInstrucciones.textContent = receta.instrucciones || "Sin instrucciones";
    modal.classList.remove("hidden");
}

// ================== MODAL DE EDICIÓN ==================
function abrirModalEdicion(receta) {
    document.getElementById('editRecipeName').value = receta.nombre;
    document.getElementById('editRecipeDescription').value = receta.descripcion;
    document.getElementById('editRecipeIngredients').value = typeof receta.ingredientes === "string"
        ? receta.ingredientes
        : receta.ingredientes.join('\n');
    document.getElementById('editRecipeInstructions').value = receta.instrucciones;

    // Mostrar imagen actual
    if (receta.imagenUrl) {
        editImagePreview.innerHTML = `<img src="${receta.imagenUrl}" alt="Preview">`;
    } else {
        editImagePreview.innerHTML = '';
    }

    // Guardar ID de la receta
    editForm.dataset.recetaId = receta.id;
    editForm.dataset.imagenActual = receta.imagenUrl || '';

    editModal.classList.remove("hidden");
}

// ================== GUARDAR EDICIÓN ==================
editForm.addEventListener('submit', async (e) => {
    e.preventDefault();

    const recetaId = editForm.dataset.recetaId;
    const usuario = JSON.parse(localStorage.getItem('usuario'));

    try {
        const datos = {
            nombre: document.getElementById('editRecipeName').value.trim(),
            descripcion: document.getElementById('editRecipeDescription').value.trim(),
            ingredientes: document.getElementById('editRecipeIngredients').value.trim(),
            instrucciones: document.getElementById('editRecipeInstructions').value.trim(),
            usuarioId: usuario.id
        };

        // Validar campos
        if (!datos.nombre || !datos.descripcion || !datos.ingredientes || !datos.instrucciones) {
            throw new Error('Todos los campos son obligatorios');
        }

        // Subir nueva imagen si se seleccionó
        if (editImageInput.files[0]) {
            const formData = new FormData();
            formData.append('file', editImageInput.files[0]);

            const imageResponse = await fetch(`${API_BASE_URL}/recetas/upload-image`, {
                method: 'POST',
                body: formData
            });

            if (!imageResponse.ok) throw new Error("Error al subir la imagen");
            const imageData = await imageResponse.json();
            datos.imagenUrl = imageData.url;
        } else {
            // Mantener imagen actual
            datos.imagenUrl = editForm.dataset.imagenActual;
        }

        // Actualizar receta
        const response = await fetch(`${API_URL}/${recetaId}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(datos)
        });

        if (!response.ok) throw new Error('Error al actualizar la receta');

        // Cerrar modal
        editModal.classList.add('hidden');
        editForm.reset();
        editImagePreview.innerHTML = '';

        // Recargar recetas
        await cargarMisRecetas(usuario.id);

        // Mostrar mensaje de éxito
        message.textContent = '✅ Receta actualizada correctamente';
        message.classList.remove('hidden');
        setTimeout(() => message.classList.add('hidden'), 3000);

    } catch (error) {
        console.error(error);
        message.textContent = '❌ Error: ' + error.message;
        message.classList.remove('hidden');
    }
});

// Preview de imagen al editar
editImageInput?.addEventListener('change', (e) => {
    const file = e.target.files[0];
    if (file) {
        const reader = new FileReader();
        reader.onload = (e) => {
            editImagePreview.innerHTML = `<img src="${e.target.result}" alt="Preview">`;
        };
        reader.readAsDataURL(file);
    }
});

// ================== ELIMINAR RECETA ==================
async function eliminarReceta(id, card) {
    if (!confirm("¿Seguro que deseas eliminar esta receta?")) return;

    try {
        const response = await fetch(`${API_URL}/${id}`, { method: "DELETE" });
        if (!response.ok) throw new Error("Error al eliminar receta");

        card.remove();
        message.textContent = "✅ Receta eliminada correctamente";
        message.classList.remove("hidden");
        setTimeout(() => message.classList.add("hidden"), 3000);

        if (grid.children.length === 0) {
            message.textContent = "Aún no has creado recetas 🍰";
            message.classList.remove("hidden");
        }
    } catch (error) {
        console.error(error);
        message.textContent = "❌ No se pudo eliminar la receta";
        message.classList.remove("hidden");
    }
}