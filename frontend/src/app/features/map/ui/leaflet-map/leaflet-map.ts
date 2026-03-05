import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-leaflet-map',
  imports: [],
  templateUrl: './leaflet-map.html',
  styleUrl: './leaflet-map.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LeafletMap {}
