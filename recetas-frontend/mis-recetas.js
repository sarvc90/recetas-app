const API_URL = "http://localhost:8080/api/recetas";
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

closeModal?.addEventListener("click", () => modal.classList.add("hidden"));

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
                <button class="edit-btn">✏️ Editar</button>
                <button class="delete-btn">🗑 Eliminar</button>
            </div>
        `;

        card.querySelector(".edit-btn").addEventListener("click", (e) => {
            e.stopPropagation();
            window.location.href = `recetas.html?edit=${receta.id}`;
        });

        card.querySelector(".delete-btn").addEventListener("click", (e) => {
            e.stopPropagation();
            eliminarReceta(receta.id, card);
        });

        card.addEventListener("click", () => abrirModal(receta));

        grid.appendChild(card);
    });
}

// ================== MODAL ==================
function abrirModal(receta) {
    modalTitle.textContent = receta.nombre;
    modalDescription.textContent = receta.descripcion;
    modalImage.src = receta.imagenUrl || "https://via.placeholder.com/500x300?text=Sin+Imagen";

    modalIngredientes.innerHTML = "";
    const ingredientes = typeof receta.ingredientes === "string"
        ? receta.ingredientes.split("\n")
        : receta.ingredientes;

    ingredientes.forEach(ing => {
        const li = document.createElement("li");
        li.textContent = ing;
        modalIngredientes.appendChild(li);
    });

    modalInstrucciones.textContent = receta.instrucciones || "Sin instrucciones";
    modal.classList.remove("hidden");
}

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
