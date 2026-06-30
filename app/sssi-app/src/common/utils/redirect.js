export function resolveRedirectTarget(redirect) {
    if (!redirect) return '/home';
    if (!redirect.startsWith('/') || redirect.startsWith('//')) return '/home';
    return redirect;
}
