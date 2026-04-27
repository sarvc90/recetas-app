package com.recetas.app.dto;

import java.math.BigDecimal;
import java.util.List;

public class DashboardCreadorResponse {

    private Long creadorId;
    private String nombreCreador;
    private BigDecimal ingresosTotales;      // ventas brutas totales
    private BigDecimal ingresosCreador;      // 75% del total
    private BigDecimal comisionPlataforma;   // 25% del total
    private long suscriptoresTotalesActivos;
    private int totalPlanes;
    private List<PlanMetrica> metricasPorPlan;

    public static class PlanMetrica {
        private Long planId;
        private String nombrePlan;
        private BigDecimal ingresos;
        private long suscriptoresActivos;
        private int totalRecetas;

        public Long getPlanId() { return planId; }
        public void setPlanId(Long planId) { this.planId = planId; }

        public String getNombrePlan() { return nombrePlan; }
        public void setNombrePlan(String nombrePlan) { this.nombrePlan = nombrePlan; }

        public BigDecimal getIngresos() { return ingresos; }
        public void setIngresos(BigDecimal ingresos) { this.ingresos = ingresos; }

        public long getSuscriptoresActivos() { return suscriptoresActivos; }
        public void setSuscriptoresActivos(long suscriptoresActivos) { this.suscriptoresActivos = suscriptoresActivos; }

        public int getTotalRecetas() { return totalRecetas; }
        public void setTotalRecetas(int totalRecetas) { this.totalRecetas = totalRecetas; }
    }

    public Long getCreadorId() { return creadorId; }
    public void setCreadorId(Long creadorId) { this.creadorId = creadorId; }

    public String getNombreCreador() { return nombreCreador; }
    public void setNombreCreador(String nombreCreador) { this.nombreCreador = nombreCreador; }

    public BigDecimal getIngresosTotales() { return ingresosTotales; }
    public void setIngresosTotales(BigDecimal ingresosTotales) { this.ingresosTotales = ingresosTotales; }

    public BigDecimal getIngresosCreador() { return ingresosCreador; }
    public void setIngresosCreador(BigDecimal ingresosCreador) { this.ingresosCreador = ingresosCreador; }

    public BigDecimal getComisionPlataforma() { return comisionPlataforma; }
    public void setComisionPlataforma(BigDecimal comisionPlataforma) { this.comisionPlataforma = comisionPlataforma; }

    public long getSuscriptoresTotalesActivos() { return suscriptoresTotalesActivos; }
    public void setSuscriptoresTotalesActivos(long suscriptoresTotalesActivos) { this.suscriptoresTotalesActivos = suscriptoresTotalesActivos; }

    public int getTotalPlanes() { return totalPlanes; }
    public void setTotalPlanes(int totalPlanes) { this.totalPlanes = totalPlanes; }

    public List<PlanMetrica> getMetricasPorPlan() { return metricasPorPlan; }
    public void setMetricasPorPlan(List<PlanMetrica> metricasPorPlan) { this.metricasPorPlan = metricasPorPlan; }
}
