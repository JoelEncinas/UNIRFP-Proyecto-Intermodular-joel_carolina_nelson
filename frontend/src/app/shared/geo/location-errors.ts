export type LocationErrorContext = 'map' | 'stations';

export const GEOLOCATION_REQUEST_OPTIONS: PositionOptions = {
  enableHighAccuracy: true,
  timeout: 10000,
  maximumAge: 0,
};

export function getLocationErrorMessage(
  errorCode: number,
  context: LocationErrorContext,
): string {
  switch (errorCode) {
    case 1:
      return context === 'map'
        ? 'Permiso de ubicacion denegado. Activalo para desbloquear cerca de estaciones.'
        : 'Permiso de ubicacion denegado.';
    case 2:
      return context === 'map'
        ? 'No se pudo obtener tu ubicacion actual. Intentalo de nuevo.'
        : 'No se pudo obtener la ubicacion actual.';
    case 3:
      return 'La solicitud de ubicacion tardo demasiado.';
    default:
      return 'No se pudo obtener tu ubicacion.';
  }
}
