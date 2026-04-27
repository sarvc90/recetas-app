// ================== CONFIG ==================
const API_BASE_URL =
  window.APP_CONFIG.API_BASE_URL || `${window.location.origin}/api`;

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

    const json = await response.json();
    const plan = json.data || json;

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

// ================== STRIPE KEY ==================
async function cargarStripeKey() {
  try {
    const response = await fetch(`${API_BASE_URL}/config/stripe`);
    if (!response.ok) throw new Error('No se pudo obtener la configuración de pago');
    const data = await response.json();
    return data.publishableKey;
  } catch (error) {
    console.error('Error al cargar Stripe config:', error);
    return null;
  }
}

// ================== STRIPE SETUP ==================
function inicializarStripe(publishableKey) {
  const stripe = Stripe(publishableKey);
  const elements = stripe.elements();

  const elementStyle = {
    base: {
      fontFamily: '"Playfair Display", serif',
      fontSize: '15px',
      color: '#333',
      '::placeholder': { color: '#aab7c4' },
    },
    invalid: {
      color: '#e74c3c',
      iconColor: '#e74c3c',
    },
  };

  const cardNumber = elements.create('cardNumber', { style: elementStyle });
  const cardExpiry = elements.create('cardExpiry', { style: elementStyle });
  const cardCvc   = elements.create('cardCvc',    { style: elementStyle });

  cardNumber.mount('#card-number-element');
  cardExpiry.mount('#card-expiry-element');
  cardCvc.mount('#card-cvc-element');

  let numberComplete = false;
  let expiryComplete = false;
  let cvcComplete    = false;

  function actualizarBoton() {
    submitPaymentBtn.disabled = !(numberComplete && expiryComplete && cvcComplete);
  }

  cardNumber.addEventListener('change', (e) => {
    cardErrorsEl.textContent = e.error ? e.error.message : '';
    numberComplete = e.complete;
    actualizarBoton();
  });
  cardExpiry.addEventListener('change', (e) => {
    cardErrorsEl.textContent = e.error ? e.error.message : '';
    expiryComplete = e.complete;
    actualizarBoton();
  });
  cardCvc.addEventListener('change', (e) => {
    cardErrorsEl.textContent = e.error ? e.error.message : '';
    cvcComplete = e.complete;
    actualizarBoton();
  });

  return { stripe, cardNumber };
}

// ================== PAYMENT FLOW ==================
async function procesarPago(stripe, cardNumber, planId) {
  submitPaymentBtn.disabled = true;
  submitPaymentBtn.textContent = '⏳ Procesando...';
  submitPaymentBtn.classList.add('processing');
  mostrarResultado(paymentResultEl, 'Iniciando proceso de pago...', 'loading');

  const zipInput = document.getElementById('card-zip');

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
    const pagoPayload = pagoData.data || pagoData;
    const { clientSecret, stripePaymentIntentId } = pagoPayload;

    if (!clientSecret) {
      throw new Error('No se recibió el client secret de Stripe');
    }

    mostrarResultado(paymentResultEl, 'Confirmando pago con Stripe...', 'loading');

    // Step 2: Confirm payment with Stripe
    const billingDetails = zipInput.value.trim()
      ? { address: { postal_code: zipInput.value.trim() } }
      : {};

    const { error: stripeError, paymentIntent } = await stripe.confirmCardPayment(
      clientSecret,
      {
        payment_method: { card: cardNumber, billing_details: billingDetails },
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

    const confirmarJson = await confirmarResponse.json();
    const suscripcion = confirmarJson.data || confirmarJson;

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

  const stripeKey = await cargarStripeKey();
  if (!stripeKey || stripeKey === 'pk_test_placeholder') {
    mostrarResultado(paymentResultEl, 'El sistema de pagos no está configurado. Contacta al administrador.', 'error');
    paymentResultEl.classList.remove('hidden');
    submitPaymentBtn.disabled = true;
    return;
  }

  const { stripe, cardNumber } = inicializarStripe(stripeKey);

  paymentForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    await procesarPago(stripe, cardNumber, planId);
  });
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', initApp);
} else {
  initApp();
}
