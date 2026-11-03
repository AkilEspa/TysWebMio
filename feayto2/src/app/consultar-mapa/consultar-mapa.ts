import { CommonModule, DecimalPipe } from '@angular/common';
import {
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  ElementRef,
  EventEmitter,
  OnDestroy,
  Output,
  ViewChild,
  inject,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import * as L from 'leaflet';
import { MunicipioService } from '../services/municipio.service';
import { Usuario } from '../services/usuario';

export interface CoordenadasMapa {
  latitud: number;
  longitud: number;
}

@Component({
  imports: [CommonModule, DecimalPipe, FormsModule],
  selector: 'app-consultar-mapa',
  styleUrl: './consultar-mapa.css',
  templateUrl: './consultar-mapa.html',
  standalone: true,
})
export class ConsultarMapa implements AfterViewInit, OnDestroy {
  @ViewChild('mapContainer', { static: true })
  private readonly mapContainer!: ElementRef<HTMLDivElement>;

  @Output() readonly coordenadasSeleccionadas = new EventEmitter<CoordenadasMapa>();

  private readonly cdr = inject(ChangeDetectorRef);
  private readonly municipioService = inject(MunicipioService);
  private readonly usuario = inject(Usuario);
  private map?: L.Map;
  private marker?: L.CircleMarker;

  readonly session$ = this.usuario.session$;
  coordenadas: CoordenadasMapa | null = null;
  capacidad = 10;
  isSavingStation = false;
  errorMessage = '';
  successMessage = '';

  ngAfterViewInit(): void {
    this.map = L.map(this.mapContainer.nativeElement).setView([38.984, -3.928], 7);
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; OpenStreetMap contributors',
    }).addTo(this.map);

    this.map.on('click', (event: L.LeafletMouseEvent) => {
      const { lat, lng } = event.latlng;
      const coordenadas = { latitud: lat, longitud: lng };
      this.coordenadas = coordenadas;
      this.errorMessage = '';
      this.successMessage = '';
      this.marker?.remove();
      this.marker = L.circleMarker(event.latlng, {
        radius: 8,
        color: '#fff',
        weight: 2,
        fillColor: '#2867bd',
        fillOpacity: 1,
      }).addTo(this.map!);
      this.coordenadasSeleccionadas.emit(coordenadas);
      this.cdr.markForCheck();
    });

    setTimeout(() => this.map?.invalidateSize());
  }

  anadirEstacion(): void {
    if (!this.coordenadas || this.isSavingStation) {
      return;
    }
    if (!Number.isInteger(this.capacidad) || this.capacidad < 1) {
      this.errorMessage = 'Indica una capacidad de al menos una bicicleta.';
      this.successMessage = '';
      return;
    }

    this.isSavingStation = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.municipioService.setEstacionMapa({
      ...this.coordenadas,
      capacidad: this.capacidad,
    }).subscribe({
      next: (feature) => {
        this.successMessage = `Estación guardada en la base de datos (ID: ${feature.id}).`;
        if (this.marker) {
          this.marker.setStyle({ fillColor: '#198754' });
        }
        this.isSavingStation = false;
        this.cdr.markForCheck();
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = this.readError(error);
        this.isSavingStation = false;
        this.cdr.markForCheck();
      },
    });
  }

  ngOnDestroy(): void {
    this.map?.remove();
  }

  private readError(error: HttpErrorResponse): string {
    if (typeof error.error === 'string') {
      return error.error;
    }
    return error.error?.detail ?? error.error?.message
      ?? 'No se pudo añadir la estación. Comprueba que has iniciado sesión y que tu municipio está registrado.';
  }
}
