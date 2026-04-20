// ================== CONFIG ==================
const API_BASE_URL =
  window.APP_CONFIG.API_BASE_URL || `${window.location.origin}/api`;

const STRIPE_KEY =
  window.STRIPE_PUBLISHABLE_KEY || 'pk_test_placeholder';

// ================== DOM REFS ==================
const planLoadingEl = document.getElementById('planLoading');
const planErrorEl = document.getElementById('planError');
const planInfoEl = document.getElementById('planInfo');
const planNombreEl = document.getElementById('planNombre');
const planPrecioEl = document.getElementById('planPrecio');
const planDuracionEl = document.getElementById('planDuracion');
const planDescripcionEl = document.getElementById('planDescripcion');
const paymentResultEl = document.getElementById('paymentResult');
const paymentForm = document.getElementById('paymentForm');
const submitPaymentBtn = document.getElementById('submitPaymentBtn');
const cardErrorsEl = document.getElementById('card-errors');
const successSection = document.getElementById('successSection');
const successMessage = document.getElementById('successMessage');
const paymentSection = document.querySelector('.payment-section');
const planSummarySection = document.querySelector('.plan-summary');

// ================== HELPERS ==================
function getAuthHeaders() {
  const token = localStorage.getItem('token');
  return {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${token}`,
  };
}

function formatearPrecio(precio) {
  return new Intl.NumberFormat('es-CO', {
    style: 'currency',
    currency: 'USD',
  }).format(precio);
}

function formatearFecha(fechaStr) {
  if (!fechaStr) return '—';
  return new Date(fechaStr).toLocaleDateString('es-ES', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });
}

function mostrarResultado(element, mensaje, tipo) {
  element.textContent = mensaje;
  element.className = `result ${tipo}`;
  element.setAttribute('role', tipo === 'error' ? 'alert' : 'status');
  element.setAttribute('aria-live', 'polite');
  element.classList.remove('hidden');
}

function obtenerPlanId() {
  const params = new URLSearchParams(window.location.search);
  return params.get('planId');
}

// ================== LOAD PLAN ==================
async function cargarPlan(planId) {
  try {
    planLoadingEl.classList.remove('hidden');

    const response = await fetch(`${API_BASE_URL}/planes/${planId}`, {
      headers: getAuthHeaders(),
    });

    if (!response.ok) throw new Error('No se encontró el plan');

    const plan = await response.json();

    planNombreEl.textContent = plan.nombre || 'Plan sin nombre';
    planPrecioEl.textContent = formatearPrecio(plan.precio || 0);
    planDuracionEl.textContent = `${plan.duracionDias || 0} días`;
    planDescripcionEl.textContent = plan.descripcion || 'Sin descripción';

    planLoadingEl.classList.add('hidden');
    planInfoEl.classList.remove('hidden');

    return plan;
  } catch (error) {
    console.error('Error al cargar plan:', error);
    planLoadingEl.classList.add('hidden');
    mostrarResultado(planErrorEl, `No se pudo cargar la información del plan: ${error.message}`, 'error');
    submitPaymentBtn.disabled = true;
    return null;
  }
}

// ================== STRIPE SETUP ==================
function inicializarStripe() {
  const stripe = Stripe(STRIPE_KEY);
  const elements = stripe.elements();

  const cardElement = elements.create('card', {
    style: {
      base: {
        fontFamily: '"Playfair Display", serif',
        fontSize: '16px',
        color: '#333',
        '::placeholder': {
          color: '#aab7c4',
        },
      },
      invalid: {
        color: '#e74c3c',
        iconColor: '#e74c3c',
      },
    },
  });

  cardElement.mount('#card-element');

  cardElement.addEventListener('change', (event) => {
    if (event.error) {
      cardErrorsEl.textContent = event.error.message;
    } else {
      cardErrorsEl.textContent = '';
    }

    // Enable submit only when card is complete and no error
    submitPaymentBtn.disabled = !event.complete;
  });

  return { stripe, cardElement };
}

// ================== PAYMENT FLOW ==================
async function procesarPago(stripe, cardElement, planId) {
  submitPaymentBtn.disabled = true;
  submitPaymentBtn.textContent = '⏳ Procesando...';
  submitPaymentBtn.classList.add('processing');
  mostrarResultado(paymentResultEl, 'Iniciando proceso de pago...', 'loading');

  try {
    // Step 1: Initiate payment
    const iniciarResponse = await fetch(`${API_BASE_URL}/suscripciones/iniciar-pago`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify({ planId: Number(planId) }),
    });

    if (!iniciarResponse.ok) {
      const err = await iniciarResponse.json().catch(() => ({}));
      throw new Error(err.message || 'Error al iniciar el pago');
    }

    const pagoData = await iniciarResponse.json();
    const { clientSecret, stripePaymentIntentId } = pagoData;

    if (!clientSecret) {
      throw new Error('No se recibió el client secret de Stripe');
    }

    mostrarResultado(paymentResultEl, 'Confirmando pago con Stripe...', 'loading');

    // Step 2: Confirm payment with Stripe
    const { error: stripeError, paymentIntent } = await stripe.confirmCardPayment(
      clientSecret,
      {
        payment_method: { card: cardElement },
      }
    );

    if (stripeError) {
      throw new Error(stripeError.message || 'Error en la confirmación del pago');
    }

    if (paymentIntent.status !== 'succeeded') {
      throw new Error(`Estado del pago inesperado: ${paymentIntent.status}`);
    }

    mostrarResultado(paymentResultEl, 'Activando suscripción...', 'loading');

    // Step 3: Confirm subscription on backend
    const confirmarResponse = await fetch(`${API_BASE_URL}/suscripciones/confirmar-pago`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify({ paymentIntentId: stripePaymentIntentId || paymentIntent.id }),
    });

    if (!confirmarResponse.ok) {
      const err = await confirmarResponse.json().catch(() => ({}));
      throw new Error(err.message || 'Error al confirmar la suscripción');
    }

    const suscripcion = await confirmarResponse.json();

    mostrarExito(suscripcion);
  } catch (error) {
    console.error('Error en el pago:', error);
    mostrarResultado(paymentResultEl, `❌ ${error.message}`, 'error');
    submitPaymentBtn.disabled = false;
    submitPaymentBtn.textContent = '💳 Pagar y suscribirse';
    submitPaymentBtn.classList.remove('processing');
  }
}

function mostrarExito(suscripcion) {
  paymentForm.classList.add('hidden');
  paymentSection.querySelector('h2').classList.add('hidden');
  paymentResultEl.classList.add('hidden');

  const nombrePlan = planNombreEl.textContent;
  const fechaFin = suscripcion.fechaFin
    ? formatearFecha(suscripcion.fechaFin)
    : 'según el plan';

  successMessage.textContent = `Te has suscrito exitosamente a "${nombrePlan}". Tu suscripción estará activa hasta el ${fechaFin}.`;
  successSection.classList.remove('hidden');

  setTimeout(() => {
    window.location.href = 'mis-suscripciones.html';
  }, 3000);
}

// ================== INICIALIZACIÓN ==================
async function initApp() {
  const token = localStorage.getItem('token');
  if (!token) {
    window.location.href = 'index.html';
    return;
  }

  const planId = obtenerPlanId();
  if (!planId) {
    mostrarResultado(planErrorEl, 'No se especificó un plan. Por favor vuelve a la página de planes.', 'error');
    planLoadingEl.classList.add('hidden');
    return;
  }

  const plan = await cargarPlan(planId);
  if (!plan) return;

  const { stripe, cardElement } = inicializarStripe();

  paymentForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    await procesarPago(stripe, cardElement, planId);
  });
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', initApp);
} else {
  initApp();
}
