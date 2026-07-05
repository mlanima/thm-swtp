import { isPlatformBrowser } from '@angular/common';
import { Component, inject, input, output, ElementRef, ViewChild, PLATFORM_ID, AfterViewInit, OnDestroy } from '@angular/core';
import { environment } from '../../enviroments/enviroment.dev';

let scriptLoadPromise: Promise<void> | null = null;

function loadGoogleMapsScript(apiKey: string): Promise<void> {
  if (scriptLoadPromise) {
    return scriptLoadPromise;
  }
  if (typeof google !== 'undefined' && google.maps?.places) {
    scriptLoadPromise = Promise.resolve();
    return scriptLoadPromise;
  }
  scriptLoadPromise = new Promise((resolve, reject) => {
    const script = document.createElement('script');
    script.src = `https://maps.googleapis.com/maps/api/js?key=${apiKey}&libraries=places`;
    script.async = true;
    script.onload = () => resolve();
    script.onerror = () => {
      scriptLoadPromise = null;
      reject(new Error('Failed to load Google Maps script'));
    };
    document.head.appendChild(script);
  });
  return scriptLoadPromise;
}

@Component({
  selector: 'app-place-autocomplete',
  standalone: true,
  imports: [],
  templateUrl: './place-autocomplete.html',
})
export class PlaceAutocomplete implements AfterViewInit, OnDestroy {
  private readonly platformId = inject(PLATFORM_ID);

  readonly placeholder = input('');

  readonly value = input('');

  readonly placeChange = output<{ placeId: string; location: string }>();

  readonly enterKey = output<void>();

  @ViewChild('inputRef', { static: true }) inputElement!: ElementRef<HTMLInputElement>;

  private autocomplete: google.maps.places.Autocomplete | null = null;

  private currentPlaceId = '';

  private readonly listener: google.maps.MapsEventListener[] = [];

  ngAfterViewInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      this.inputElement.nativeElement.value = this.value();
      this.initAutocomplete();
    }
  }

  onInput(): void {
    this.currentPlaceId = '';
    this.placeChange.emit({
      placeId: '',
      location: this.inputElement.nativeElement.value,
    });
  }

  private async initAutocomplete(): Promise<void> {
    try {
      await loadGoogleMapsScript(environment.googleMapsApiKey);
      this.autocomplete = new google.maps.places.Autocomplete(this.inputElement.nativeElement, {
        types: ['(cities)'],
      });
      const evt = this.autocomplete.addListener('place_changed', () => {
        const place = this.autocomplete!.getPlace();
        if (place.place_id && place.formatted_address) {
          this.currentPlaceId = place.place_id;
          this.inputElement.nativeElement.value = place.formatted_address;
          this.placeChange.emit({
            placeId: place.place_id,
            location: place.formatted_address,
          });
        }
      });
      this.listener.push(evt);
    } catch {
      // script failed to load — autocomplete unavailable, input works as plain text
    }
  }

  ngOnDestroy(): void {
    this.listener.forEach((l) => l.remove());
    if (this.autocomplete) {
      google.maps.event.clearInstanceListeners(this.autocomplete);
    }
    document.querySelectorAll('.pac-container').forEach(el => el.remove());
  }
}
