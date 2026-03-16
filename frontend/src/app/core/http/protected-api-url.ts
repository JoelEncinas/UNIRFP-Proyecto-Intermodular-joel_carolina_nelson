export function isProtectedApiRequest(url: string): boolean {
  // Relative API calls from app code.
  if (url.startsWith('/api/')) {
    return true;
  }

  // Absolute API calls (e.g. http://localhost:8080/api/...)
  try {
    const parsed = new URL(url);
    return parsed.pathname.startsWith('/api/');
  } catch {
    return false;
  }
}
