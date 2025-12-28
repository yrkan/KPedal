import type { Reroute } from '@sveltejs/kit';

export const reroute: Reroute = ({ url }) => {
  // link.kpedal.com serves /link page at root
  if (url.hostname === 'link.kpedal.com' && url.pathname === '/') {
    return '/link';
  }
  // start.kpedal.com serves /start page at root
  if (url.hostname === 'start.kpedal.com' && url.pathname === '/') {
    return '/start';
  }
};
