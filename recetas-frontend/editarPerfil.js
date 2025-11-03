const API_URL = "http://localhost:8080/api";

// Obtener ID del usuario del localStorage
const usuarioId = localStorage.getItem("usuarioId");

if (!usuarioId) {
  window.location.href = "login.html";
}

// Cargar perfil al inicio
document.addEventListener("DOMContentLoaded", cargarPerfil);

async function cargarPerfil() {
  mostrarLoading(true);

  try {
    const response = await fetch(`${API_URL}/usuarios/${usuarioId}`);
    const data = await response.json();

    if (data.success) {
      const usuario = data.data;
      document.getElementById("nombre").value = usuario.nombre;
      document.getElementById("email").value = usuario.email;

      // Mostrar foto de perfil
      const fotoContainer = document.getElementById("fotoContainer");
      if (usuario.fotoPerfil) {
        fotoContainer.innerHTML = `<img src="${usuario.fotoPerfil}" alt="Foto de perfil" class="foto-perfil">`;
      } else {
        fotoContainer.innerHTML = `<div class="foto-placeholder">👤</div>`;
      }
    } else {
      mostrarMensaje(data.message, "error");
    }
  } catch (error) {
    console.error("Error al cargar perfil:", error);
    mostrarMensaje("Error al cargar el perfil", "error");
  } finally {
    mostrarLoading(false);
  }
}

// Preview de imagen al seleccionar
document.getElementById("fotoPerfil").addEventListener("change", function (e) {
  const file = e.target.files[0];
  if (file) {
    // Validar tamaño (5MB)
    if (file.size > 5 * 1024 * 1024) {
      mostrarMensaje("La imagen no debe superar los 5MB", "error");
      this.value = "";
      return;
    }

    // Validar tipo
    if (!file.type.startsWith("image/")) {
      mostrarMensaje("Por favor selecciona una imagen válida", "error");
      this.value = "";
      return;
    }

    const reader = new FileReader();
    reader.onload = function (e) {
      const fotoContainer = document.getElementById("fotoContainer");
      fotoContainer.innerHTML = `<img src="${e.target.result}" alt="Vista previa" class="foto-perfil">`;
    };
    reader.readAsDataURL(file);
  }
});

// Enviar formulario
document
  .getElementById("editarPerfilForm")
  .addEventListener("submit", async function (e) {
    e.preventDefault();
    mostrarLoading(true);

    try {
      const nombre = document.getElementById("nombre").value.trim();
      const fotoFile = document.getElementById("fotoPerfil").files[0];

      // Validar nombre
      if (nombre.length < 2) {
        mostrarMensaje("El nombre debe tener al menos 2 caracteres", "error");
        mostrarLoading(false);
        return;
      }

      let fotoUrl = null;

      // Si hay una foto nueva, subirla primero
      if (fotoFile) {
        const formData = new FormData();
        formData.append("file", fotoFile);

        const fotoResponse = await fetch(
          `${API_URL}/usuarios/${usuarioId}/foto`,
          {
            method: "POST",
            body: formData,
          },
        );

        const fotoData = await fotoResponse.json();

        if (fotoData.success) {
          fotoUrl = fotoData.data;
        } else {
          mostrarMensaje(fotoData.message, "error");
          mostrarLoading(false);
          return;
        }
      }

      // Actualizar perfil
      const updateData = {
        nombre: nombre,
        fotoPerfil: fotoUrl,
      };

      const response = await fetch(`${API_URL}/usuarios/${usuarioId}/perfil`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(updateData),
      });

      const data = await response.json();

      if (data.success) {
        mostrarMensaje("Perfil actualizado exitosamente", "success");

        // Actualizar localStorage
        localStorage.setItem("usuarioNombre", data.data.nombre);
        if (data.data.fotoPerfil) {
          localStorage.setItem("usuarioFoto", data.data.fotoPerfil);
        }

        // Redirigir después de 2 segundos
        setTimeout(() => {
          window.location.href = "recetas.html";
        }, 2000);
      } else {
        mostrarMensaje(data.message, "error");
      }
    } catch (error) {
      console.error("Error:", error);
      mostrarMensaje(
        "Error al actualizar el perfil. Por favor intenta de nuevo.",
        "error",
      );
    } finally {
      mostrarLoading(false);
    }
  });

function mostrarMensaje(mensaje, tipo) {
  const messageDiv = document.getElementById("message");
  messageDiv.textContent = mensaje;
  messageDiv.className = tipo;
  messageDiv.classList.remove("hidden");

  // Scroll al mensaje
  messageDiv.scrollIntoView({ behavior: "smooth", block: "nearest" });

  // Ocultar después de 5 segundos si es de éxito
  if (tipo === "success") {
    setTimeout(() => {
      messageDiv.classList.add("hidden");
    }, 5000);
  }
}

function mostrarLoading(mostrar) {
  const loadingDiv = document.getElementById("loading");
  const formDiv = document.getElementById("editarPerfilForm");

  if (mostrar) {
    loadingDiv.classList.remove("hidden");
    formDiv.style.opacity = "0.5";
    formDiv.style.pointerEvents = "none";
  } else {
    loadingDiv.classList.add("hidden");
    formDiv.style.opacity = "1";
    formDiv.style.pointerEvents = "auto";
  }
}
