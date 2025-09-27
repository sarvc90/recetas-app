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

// Cerrar modal
closeModal.addEventListener("click", () => modal.classList.add("hidden"));

// Cargar más
loadMoreBtn.addEventListener("click", () => {
    currentPage++;
    fetchRecetas(currentPage);
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
