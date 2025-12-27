import type { Handle } from '@sveltejs/kit';

export const handle: Handle = async ({ event, resolve }) => {
	const response = await resolve(event);

	// Security headers
	response.headers.set('X-Content-Type-Options', 'nosniff');
	response.headers.set('X-Frame-Options', 'DENY');
	response.headers.set('Referrer-Policy', 'strict-origin-when-cross-origin');

	// Cache control for HTML pages (force revalidation after deploy)
	if (response.headers.get('content-type')?.includes('text/html')) {
		response.headers.set('Cache-Control', 'public, max-age=0, must-revalidate');
	}

	return response;
};
