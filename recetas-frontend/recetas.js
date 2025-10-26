// ================== VARIABLES GLOBALES ==================
const API_URL = "http://localhost:8080/api/recetas";
const API_BASE_URL = "http://localhost:8080/api";
const pageSize = 6;
let currentPage = 0;
let selectedRating = 0;
let currentRecetaId = null;

// Botones y elementos principales
const recetasGrid = document.getElementById("recetasGrid");
const loading = document.getElementById("loading");
const message = document.getElementById("message");
const modal = document.getElementById("modal");
const closeModal = document.getElementById("closeModal");
const modalTitle = document.getElementById("modalTitle");
const modalDescription = document.getElementById("modalDescription");
const modalImage = document.getElementById("modalImage");
const modalIngredientes = document.getElementById("modalIngredientes");
const modalInstrucciones = document.getElementById("modalInstrucciones");
const addRecipeBtn = document.getElementById('addRecipeBtn');
const recipeForm = document.getElementById('recipeForm');
const newRecipeForm = document.getElementById('newRecipeForm');
const cancelRecipeBtn = document.getElementById('cancelRecipeBtn');
const imageInput = document.getElementById('recipeImage');
const imagePreview = document.getElementById('imagePreview');
const loadMoreBtn = document.getElementById("loadMore");

// Variables de favoritos
const favBtn = document.getElementById("favBtn");
let esFavorito = false;
let recetaActual = null;

// Variables de comentarios
const commentsList = document.getElementById("commentsList");
const commentText = document.getElementById("commentText");
const submitCommentBtn = document.getElementById("submitComment");
const starsInput = document.querySelectorAll(".star");
const selectedRatingText = document.getElementById("selectedRating");
const ratingStars = document.getElementById("ratingStars");
const ratingText = document.getElementById("ratingText");

// ================== EVENT LISTENERS ==================
closeModal?.addEventListener("click", () => {
    modal.classList.add("hidden");
    resetCommentForm();
});

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

// Event listeners para la búsqueda
document.getElementById('searchBtn')?.addEventListener('click', buscarRecetas);
document.getElementById('searchInput')?.addEventListener('keypress', (e) => {
    if (e.key === 'Enter') {
        buscarRecetas();
    }
});

// Event listener para favoritos
favBtn?.addEventListener("click", async () => {
    const usuario = JSON.parse(localStorage.getItem("usuario"));
    if (!usuario) return alert("Inicia sesión para usar favoritos");

    const res = await fetch(`${API_BASE_URL}/favoritos/toggle?usuarioId=${usuario.id}&recetaId=${recetaActual.id}`, {
        method: "POST"
    });

    const data = await res.json();
    alert(data.message);
    esFavorito = !esFavorito;
    actualizarIconoFav();
});

// Event listeners para las estrellas de calificación
starsInput.forEach(star => {
    star.addEventListener("click", () => {
        selectedRating = parseInt(star.dataset.rating);
        updateStarsInput();
    });

    star.addEventListener("mouseenter", () => {
        const rating = parseInt(star.dataset.rating);
        highlightStars(rating);
    });
});

document.querySelector(".stars-input")?.addEventListener("mouseleave", () => {
    updateStarsInput();
});

// Event listener para enviar comentario
submitCommentBtn?.addEventListener("click", async () => {
    await submitComment();
});

// ================== FUNCIONES DE COMENTARIOS ==================

function updateStarsInput() {
    starsInput.forEach(star => {
        const rating = parseInt(star.dataset.rating);
        star.textContent = rating <= selectedRating ? "★" : "☆";
        star.classList.toggle("active", rating <= selectedRating);
    });
    selectedRatingText.textContent = selectedRating > 0
        ? `${selectedRating} ${selectedRating === 1 ? 'estrella' : 'estrellas'}`
        : "No seleccionada";
}

function highlightStars(rating) {
    starsInput.forEach(star => {
        const starRating = parseInt(star.dataset.rating);
        star.textContent = starRating <= rating ? "★" : "☆";
    });
}

function resetCommentForm() {
    selectedRating = 0;
    if (commentText) commentText.value = "";
    updateStarsInput();
}

async function cargarComentarios(recetaId) {
    try {
        const response = await fetch(`${API_URL}/${recetaId}/comentarios`);
        if (!response.ok) throw new Error("Error al cargar comentarios");

        const data = await response.json();

        if (data.success) {
            const { comentarios, promedioCalificacion, totalComentarios } = data.data;

            // Actualizar display de calificación
            mostrarCalificacionPromedio(promedioCalificacion, totalComentarios);

            // Mostrar comentarios
            if (comentarios && comentarios.length > 0) {
                renderComentarios(comentarios);
            } else {
                commentsList.innerHTML = '<p class="no-comments">Sé el primero en comentar esta receta</p>';
            }
        }
    } catch (error) {
        console.error("Error al cargar comentarios:", error);
        if (commentsList) {
            commentsList.innerHTML = '<p class="no-comments">Error al cargar comentarios</p>';
        }
    }
}

function mostrarCalificacionPromedio(promedio, total) {
    const estrellas = Math.round(promedio);
    let estrellasHTML = "";

    for (let i = 1; i <= 5; i++) {
        estrellasHTML += i <= estrellas ? "★" : "☆";
    }

    if (ratingStars) ratingStars.textContent = estrellasHTML;
    if (ratingText) {
        ratingText.textContent = total > 0
            ? `${promedio.toFixed(1)} / 5 (${total} ${total === 1 ? 'calificación' : 'calificaciones'})`
            : "Sin calificaciones";
    }
}

function renderComentarios(comentarios) {
    if (!comentarios || comentarios.length === 0) {
        if (commentsList) {
            commentsList.innerHTML = '<p class="no-comments">Sé el primero en comentar esta receta</p>';
        }
        return;
    }

    if (commentsList) {
        commentsList.innerHTML = comentarios.map(comentario => {
            const fecha = new Date(comentario.fechaCreacion);
            const fechaFormateada = fecha.toLocaleDateString('es-ES', {
                year: 'numeric',
                month: 'long',
                day: 'numeric',
                hour: '2-digit',
                minute: '2-digit'
            });

            let estrellasComentario = "";
            for (let i = 1; i <= 5; i++) {
                estrellasComentario += i <= comentario.calificacion ? "★" : "☆";
            }

            return `
                <div class="comment-item">
                    <div class="comment-header">
                        <span class="comment-author">${comentario.nombreUsuario}</span>
                        <span class="comment-rating">${estrellasComentario}</span>
                    </div>
                    <p class="comment-text">${comentario.texto}</p>
                    <span class="comment-date">${fechaFormateada}</span>
                </div>
            `;
        }).join('');
    }
}

async function submitComment() {
    const usuario = JSON.parse(localStorage.getItem("usuario"));

    if (!usuario) {
        alert("Debes iniciar sesión para comentar");
        return;
    }

    if (selectedRating === 0) {
        alert("Por favor selecciona una calificación");
        return;
    }

    const texto = commentText ? commentText.value.trim() : "";
    if (!texto) {
        alert("Por favor escribe un comentario");
        return;
    }

    try {
        if (submitCommentBtn) {
            submitCommentBtn.disabled = true;
            submitCommentBtn.textContent = "Publicando...";
        }

        const response = await fetch(`${API_URL}/${currentRecetaId}/comentarios`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                usuarioId: usuario.id,
                texto: texto,
                calificacion: selectedRating
            })
        });

        const data = await response.json();

        if (data.success) {
            alert("✅ Comentario publicado correctamente");
            resetCommentForm();
            await cargarComentarios(currentRecetaId);

            // Ocultar formulario después de comentar
            const commentForm = document.getElementById("commentForm");
            if (commentForm) commentForm.style.display = "none";
        } else {
            alert("❌ " + data.message);
        }

    } catch (error) {
        console.error("Error al publicar comentario:", error);
        alert("❌ Error al publicar el comentario");
    } finally {
        if (submitCommentBtn) {
            submitCommentBtn.disabled = false;
            submitCommentBtn.textContent = "Publicar Comentario";
        }
    }
}

async function verificarSiYaComento(recetaId, usuarioId) {
    try {
        const response = await fetch(`${API_URL}/${recetaId}/comentarios/verificar?usuarioId=${usuarioId}`);
        const data = await response.json();

        const commentForm = document.getElementById("commentForm");
        if (commentForm) {
            if (data.data === true) {
                // Ya comentó - ocultar formulario
                commentForm.style.display = "none";
            } else {
                // No ha comentado - mostrar formulario
                commentForm.style.display = "block";
            }
        }
    } catch (error) {
        console.error("Error al verificar comentario:", error);
    }
}

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
        const recetas = data.content || data;

        if (page === 0) recetasGrid.innerHTML = '';

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

        message.classList.add("hidden");

    } catch (error) {
        console.error('Error al cargar recetas:', error);

        if (page === 0 && recetasGrid.children.length === 0) {
            message.textContent = "Error al cargar recetas. Por favor, intenta de nuevo.";
            message.classList.remove("hidden");
        }
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
        `;

        card.addEventListener("click", () => openModal(receta));

        recetasGrid.appendChild(card);
    });
}

// ================== MODAL ==================
function openModal(receta) {
    modalTitle.textContent = receta.nombre || "Receta sin nombre";
    modalDescription.textContent = receta.descripcion || "Sin descripción";
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
    recetaActual = receta;
    currentRecetaId = receta.id;

    const usuario = JSON.parse(localStorage.getItem("usuario"));
    if (usuario) {
        verificarFavorito(usuario.id, receta.id);
        verificarSiYaComento(receta.id, usuario.id);
    } else {
        const commentForm = document.getElementById("commentForm");
        if (commentForm) commentForm.style.display = "none";
    }

    // Cargar comentarios
    cargarComentarios(receta.id);

    // Resetear formulario de comentarios
    resetCommentForm();

    modal.classList.remove("hidden");
}

// ================== FAVORITOS ==================
async function verificarFavorito(usuarioId, recetaId) {
    try {
        const res = await fetch(`${API_BASE_URL}/favoritos/usuario/${usuarioId}/receta/${recetaId}`);
        const data = await res.json();
        esFavorito = data.data;
        actualizarIconoFav();
    } catch (error) {
        console.error('Error al verificar favorito:', error);
    }
}

function actualizarIconoFav() {
    if (favBtn) {
        favBtn.textContent = esFavorito ? "❤️ Quitar de Favoritos" : "🤍 Agregar a Favoritos";
    }
}

// ================== GUARDAR NUEVA RECETA ==================
newRecipeForm?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const usuario = JSON.parse(localStorage.getItem('usuario'));

    try {
        const datos = validarFormularioReceta();

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
        } else {
            throw new Error('La imagen es obligatoria para nuevas recetas');
        }

        datos.usuarioId = usuario.id;

        const recetaResponse = await fetch(API_URL, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(datos)
        });

        if (!recetaResponse.ok) throw new Error('Error al crear la receta');

        const recetaActualizada = await recetaResponse.json();
        renderRecetas([recetaActualizada]);

        // Limpiar formulario
        recipeForm.classList.add('hidden');
        newRecipeForm.reset();
        imagePreview.innerHTML = '';

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
        recetasGrid.innerHTML = '';

        const response = await fetch(`${API_URL}/buscar?termino=${encodeURIComponent(termino)}`, {
            method: 'GET',
            headers: {
                'Accept': 'application/json'
            }
        });

        if (!response.ok) throw new Error('Error al buscar recetas');

        const data = await response.json();
        const recetas = data.data || [];

        if (!recetas || recetas.length === 0) {
            message.textContent = 'No se encontraron recetas que coincidan con tu búsqueda';
            message.classList.remove('hidden');
            loadMoreBtn?.classList.add('hidden');
            return;
        }

        renderRecetas(recetas);
        loadMoreBtn?.classList.add('hidden');

    } catch (error) {
        console.error('Error en la búsqueda:', error);
        message.textContent = 'Error al realizar la búsqueda. Por favor, intenta de nuevo.';
        message.classList.remove('hidden');
    } finally {
        loading.classList.add('hidden');
    }
}

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
                <div class="nav-buttons">
                    <a href="mis-recetas.html" class="nav-btn">Mis Recetas</a>
                    <a href="favoritos.html" class="nav-btn">❤️ Favoritos</a>
                    <button id="logout" class="logout-btn">Cerrar Sesión</button>
                </div>
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