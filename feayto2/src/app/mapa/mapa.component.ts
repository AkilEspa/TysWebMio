import { AfterViewInit, ChangeDetectorRef, Component, OnDestroy, inject } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import * as L from 'leaflet';
import { HttpErrorResponse } from '@angular/common/http';
import { SimuladorService } from '../services/simulador.service';
import { VehiculoDto } from '../model/VehiculoDto';
import { MunicipioDto } from '../model/MunicipioDto';

@Component({
  selector: 'app-mapa',
  standalone: true,
  imports: [DecimalPipe, FormsModule],
  templateUrl: './mapa.component.html',
  styleUrl: './mapa.component.css',
})
export class MapaComponent implements AfterViewInit, OnDestroy {
  private readonly cdr = inject(ChangeDetectorRef);

  cities: MunicipioDto[] = [];
  selectedCity?: MunicipioDto;
  vehiculos: VehiculoDto[] = [];
  isLoading = false;
  errorMessage = '';
  successMessage = '';
  dataMessage = '';
  private map?: L.Map;
  private readonly markers = new Map<string, L.Marker>();
  private rutaSeleccionada?: L.Polyline;

  constructor(private readonly simuladorService: SimuladorService) {}

  ngAfterViewInit(): void {
    this.map = L.map('map').setView([38.984, -3.928], 7);
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; OpenStreetMap contributors',
    }).addTo(this.map);

    this.simuladorService.getCities().subscribe({
      next: (cities) => {
        this.cities = cities;
        if (cities.length > 0) {
          this.loadMap(cities[0]);
        } else {
          this.dataMessage = 'El simulador todavía no tiene municipios disponibles.';
        }
        this.cdr.markForCheck();
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = this.readError(error, 'No se pudieron cargar los municipios del simulador.');
        this.cdr.markForCheck();
      },
    });
  }

  loadMap(city: MunicipioDto): void {
    this.selectedCity = city;
    const hasCoordinates = Number.isFinite(city.latitude) && Number.isFinite(city.longitude);
    if (hasCoordinates) {
      this.map?.setView([city.latitude!, city.longitude!], 15);
      this.dataMessage = '';
    } else {
      this.map?.setView([38.984, -3.928], 7);
      this.dataMessage =
        `${city.name} todavía no tiene coordenadas ni direcciones cargadas. Puedes seleccionarlo, pero no se podrán mostrar ubicaciones ni generar rutas hasta cargar esos datos.`;
    }
    setTimeout(() => this.map?.invalidateSize());
    this.loadVehiculos();
  }

  mostrarRuta(vehiculo: VehiculoDto): void {
    if (!this.selectedCity || !this.map) {
      return;
    }

    this.errorMessage = '';
    this.simuladorService.getRuta(this.selectedCity.name, vehiculo.matricula).subscribe({
      next: (puntos) => {
        this.rutaSeleccionada?.remove();
        const coordenadas: L.LatLngExpression[] = puntos.map((punto) => [
          punto.latitude,
          punto.longitude,
        ]);
        this.rutaSeleccionada = L.polyline(coordenadas, { weight: 4, opacity: 0.8 }).addTo(this.map!);
        this.cdr.markForCheck();
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = this.readError(error, 'No se pudo cargar la ruta del vehículo.');
        this.cdr.markForCheck();
      },
    });
  }

  simulacionAleatoria(): void {
    if (!this.selectedCity || this.isLoading) {
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.simuladorService.simulacionAleatoria(this.selectedCity.name).subscribe({
      next: () => {
        this.successMessage = 'Simulación completada.';
        this.loadVehiculos();
        this.cdr.markForCheck();
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = this.readError(error, 'No se pudo ejecutar la simulación.');
        this.isLoading = false;
        this.cdr.markForCheck();
      },
    });
  }

  generarBicicleta(): void {
    if (!this.selectedCity || this.isLoading) {
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.simuladorService.generateBicycle(this.selectedCity.name).subscribe({
      next: () => {
        this.successMessage = 'Solicitud de generación enviada.';
        this.loadVehiculos();
        this.cdr.markForCheck();
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = this.readError(error, 'No se pudo generar una bicicleta.');
        this.isLoading = false;
        this.cdr.markForCheck();
      },
    });
  }

  loadVehiculos(): void {
    if (!this.selectedCity) {
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';
    this.simuladorService.getVehiculos(this.selectedCity.name).subscribe({
      next: (vehiculos) => {
        this.vehiculos = vehiculos;
        this.actualizarMarcadores();
        this.isLoading = false;
        this.cdr.markForCheck();
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = this.readError(error, 'No se pudieron cargar los vehículos del simulador.');
        this.isLoading = false;
        this.cdr.markForCheck();
      },
    });
  }

  ngOnDestroy(): void {
    this.map?.remove();
    this.markers.clear();
  }

  private actualizarMarcadores(): void {
    if (!this.map) {
      return;
    }

    const actuales = new Set<string>();
    for (const vehiculo of this.vehiculos) {
      actuales.add(vehiculo.matricula);
      const marker = this.markers.get(vehiculo.matricula);
      if (marker) {
        marker.setLatLng([vehiculo.latitudActual, vehiculo.longitudActual]);
      } else {
        const icon = L.divIcon({
          className: 'bicycle-marker',
          html: `<div class="bicycle-marker-content"><span>🚴</span><span class="bicycle-label">${vehiculo.matricula}</span></div>`,
          iconSize: [60, 48],
          iconAnchor: [30, 20],
        });
        const newMarker = L.marker(
          [vehiculo.latitudActual, vehiculo.longitudActual],
          { icon },
        ).bindTooltip(vehiculo.matricula).addTo(this.map);
        this.markers.set(vehiculo.matricula, newMarker);
      }
    }

    for (const [matricula, marker] of this.markers) {
      if (!actuales.has(matricula)) {
        marker.remove();
        this.markers.delete(matricula);
      }
    }
  }

  private readError(error: HttpErrorResponse, fallback: string): string {
    if (typeof error.error === 'string') {
      return error.error;
    }
    return error.error?.detail ?? error.error?.message ?? fallback;
  }
}
