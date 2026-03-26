export function buildApiUrl(apiBaseUrl: string, path: string): string {
  if (!apiBaseUrl) {
    return path;
  }

  return `${apiBaseUrl.replace(/\/+$/, '')}${path}`;
}
