const API_BASE_URL = "http://localhost:8080/api";
const grid = document.getElementById("favoritosGrid");
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
const favBtn = document.getElementById("favBtn");

let recetaActual = null;

closeModal?.addEventListener("click", () => modal.classList.add("hidden"));

// ================== INICIALIZACIÓN ==================
document.addEventListener("DOMContentLoaded", async () => {
    const usuario = JSON.parse(localStorage.getItem("usuario"));
    if (!usuario) {
        window.location.href = "index.html";
        return;
    }

    await cargarFavoritos(usuario.id);
});

// ================== CARGAR FAVORITOS ==================
async function cargarFavoritos(usuarioId) {
    try {
        loading.classList.remove("hidden");
        message.classList.add("hidden");

        const response = await fetch(`${API_BASE_URL}/favoritos/usuario/${usuarioId}`);
        if (!response.ok) throw new Error("Error al cargar favoritos");

        const data = await response.json();
        grid.innerHTML = "";

        if (!data.success || !data.data || data.data.length === 0) {
            message.textContent = "Aún no has agregado recetas a favoritos ❤️";
            message.classList.remove("hidden");
            return;
        }

        renderFavoritos(data.data);

    } catch (error) {
        console.error(error);
        message.textContent = "❌ Error al cargar tus favoritos";
        message.classList.remove("hidden");
    } finally {
        loading.classList.add("hidden");
    }
}

// ================== RENDERIZAR ==================
function renderFavoritos(recetas) {
    recetas.forEach(receta => {
        const card = document.createElement("div");
        card.className = "card";
        card.dataset.id = receta.id;

        card.innerHTML = `
            <img src="${receta.imagenUrl || 'https://via.placeholder.com/300x200?text=Sin+Imagen'}" 
                 alt="${receta.nombre}">
            <h3>${receta.nombre}</h3>
            <p>${receta.descripcion || 'Sin descripción'}</p>
        `;

        card.addEventListener("click", () => abrirModal(receta));

        grid.appendChild(card);
    });
}

// ================== MODAL ==================
function abrirModal(receta) {
    recetaActual = receta;
    modalTitle.textContent = receta.nombre;
    modalDescription.textContent = receta.descripcion || "Sin descripción";
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

// ================== QUITAR DE FAVORITOS ==================
favBtn.addEventListener("click", async () => {
    const usuario = JSON.parse(localStorage.getItem("usuario"));
    if (!usuario || !recetaActual) return;

    try {
        const response = await fetch(
            `${API_BASE_URL}/favoritos/toggle?usuarioId=${usuario.id}&recetaId=${recetaActual.id}`,
            { method: "POST" }
        );

        const data = await response.json();

        if (data.success) {
            // Cerrar modal
            modal.classList.add("hidden");

            // Eliminar card del grid
            const card = document.querySelector(`.card[data-id="${recetaActual.id}"]`);
            if (card) card.remove();

            // Mostrar mensaje
            message.textContent = "✅ Receta eliminada de favoritos";
            message.classList.remove("hidden");
            setTimeout(() => message.classList.add("hidden"), 3000);

            // Verificar si no quedan favoritos
            if (grid.children.length === 0) {
                message.textContent = "Aún no has agregado recetas a favoritos ❤️";
                message.classList.remove("hidden");
            }
        }
    } catch (error) {
        console.error(error);
        message.textContent = "❌ Error al quitar de favoritos";
        message.classList.remove("hidden");
    }
});