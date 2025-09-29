const API_URL = "http://localhost:8080/api/recetas";
let currentPage = 0;
const pageSize = 9;

const recetasGrid = document.getElementById("recetasGrid");
const loadMoreBtn = document.getElementById("loadMore");
const loading = document.getElementById("loading");
const message = document.getElementById("message");

// Modal
const modal = document.getElementById("modal");
const closeModal = document.getElementById("closeModal");
const modalTitle = document.getElementById("modalTitle");
const modalImage = document.getElementById("modalImage");
const modalIngredientes = document.getElementById("modalIngredientes");
const modalInstrucciones = document.getElementById("modalInstrucciones");

// Variables para el formulario de recetas
const addRecipeBtn = document.getElementById('addRecipeBtn');
const recipeForm = document.getElementById('recipeForm');
const newRecipeForm = document.getElementById('newRecipeForm');
const cancelRecipeBtn = document.getElementById('cancelRecipeBtn');
const imageInput = document.getElementById('recipeImage');
const imagePreview = document.getElementById('imagePreview');

// Cerrar modal
closeModal.addEventListener("click", () => modal.classList.add("hidden"));

// Cargar más
loadMoreBtn.addEventListener("click", () => {
    currentPage++;
    fetchRecetas(currentPage);
});

// Mostrar/ocultar formulario
addRecipeBtn.addEventListener('click', () => {
    recipeForm.classList.remove('hidden');
});

cancelRecipeBtn.addEventListener('click', () => {
    recipeForm.classList.add('hidden');
    newRecipeForm.reset();
    imagePreview.innerHTML = '';
});

// Preview de imagen
imageInput.addEventListener('change', (e) => {
    const file = e.target.files[0];
    if (file) {
        const reader = new FileReader();
        reader.onload = (e) => {
            imagePreview.innerHTML = `<img src="${e.target.result}" alt="Preview">`;
        };
        reader.readAsDataURL(file);
    }
});

// Manejar envío del formulario
newRecipeForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const usuario = JSON.parse(localStorage.getItem('usuario'));

    try {
        // Primero subir la imagen
        const formData = new FormData();
        formData.append('file', imageInput.files[0]);

        const imageResponse = await fetch(`${API_URL}/upload-image`, {
            method: 'POST',
            body: formData
        });

        if (!imageResponse.ok) throw new Error('Error al subir la imagen');
        const imageData = await imageResponse.json();

        // Crear la receta con la URL de la imagen
        const recetaData = {
            nombre: document.getElementById('recipeName').value,
            descripcion: document.getElementById('recipeDescription').value,
            ingredientes: document.getElementById('recipeIngredients').value.split('\n').filter(i => i.trim()),
            instrucciones: document.getElementById('recipeInstructions').value,
            imagenUrl: imageData.data, // URL de la imagen subida
            usuarioId: usuario.id
        };

        const recetaResponse = await fetch(API_URL, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(recetaData)
        });

        if (!recetaResponse.ok) throw new Error('Error al crear la receta');

        // Limpiar formulario y actualizar lista
        recipeForm.classList.add('hidden');
        newRecipeForm.reset();
        imagePreview.innerHTML = '';
        recetasGrid.innerHTML = ''; // Limpiar grid actual
        currentPage = 0;
        fetchRecetas(); // Recargar recetas

    } catch (error) {
        console.error(error);
        message.textContent = 'Error al crear la receta: ' + error.message;
        message.classList.remove('hidden');
    }
});

// Verificar autenticación al cargar la página
document.addEventListener('DOMContentLoaded', () => {
    const usuario = localStorage.getItem('usuario');
    if (!usuario) {
        window.location.href = 'index.html';
        return;
    }

    // Agregar botón de cerrar sesión
    const container = document.querySelector('.container');
    const header = document.createElement('div');
    header.className = 'header';
    header.innerHTML = `
        <div class="user-info">
            <span>Bienvenido, ${JSON.parse(usuario).nombre}</span>
            <button id="logout" class="logout-btn">Cerrar Sesión</button>
        </div>
    `;
    container.insertBefore(header, container.firstChild);

    // Evento para cerrar sesión
    document.getElementById('logout').addEventListener('click', () => {
        localStorage.removeItem('usuario');
        window.location.href = 'index.html';
    });
});

// Fetch recetas
async function fetchRecetas(page = 0) {
    loading.classList.remove("hidden");
    message.classList.add("hidden");

    try {
        const res = await fetch(`${API_URL}?page=${page}&size=${pageSize}`);
        if (!res.ok) throw new Error("Error al cargar recetas");

        const data = await res.json();
        const recetas = data.content || [];

        if (recetas.length === 0 && page === 0) {
            message.textContent = "No hay recetas disponibles";
            message.classList.remove("hidden");
            loadMoreBtn.classList.add("hidden");
            return;
        }

        renderRecetas(recetas);

        if (data.last) loadMoreBtn.classList.add("hidden");
        else loadMoreBtn.classList.remove("hidden");

    } catch (err) {
        console.error(err);
        message.textContent = "Error al cargar recetas";
        message.classList.remove("hidden");
    } finally {
        loading.classList.add("hidden");
    }
}

// Renderizar recetas
function renderRecetas(recetas) {
    recetas.forEach(r => {
        const card = document.createElement("div");
        card.className = "card";

        const nombre = r.nombre || "Receta sin nombre";
        const descripcion = r.descripcion || "Sin descripción";
        const imagen = r.imagenUrl || "https://via.placeholder.com/300x200?text=Sin+Imagen";

        card.innerHTML = `
            <img src="${imagen}" alt="${nombre}">
            <h3>${nombre}</h3>
            <p>${descripcion}</p>
        `;

        card.addEventListener("click", () => openModal(r));
        recetasGrid.appendChild(card);
    });
}

// Modal detalle
function openModal(r) {
    modalTitle.textContent = r.nombre || "Receta sin nombre";
    modalImage.src = r.imagenUrl || "https://via.placeholder.com/500x300?text=Sin+Imagen";

    // Ingredientes
    modalIngredientes.innerHTML = "";
    let ingredientes = [];
    if (Array.isArray(r.ingredientes)) ingredientes = r.ingredientes;
    else if (typeof r.ingredientes === "string") ingredientes = r.ingredientes.split(",").map(i => i.trim());

    if (ingredientes.length === 0) ingredientes = ["No especificados"];
    ingredientes.forEach(ing => {
        const li = document.createElement("li");
        li.textContent = ing;
        modalIngredientes.appendChild(li);
    });

    modalInstrucciones.textContent = r.instrucciones || "No disponibles";

    modal.classList.remove("hidden");
}

fetchRecetas();
