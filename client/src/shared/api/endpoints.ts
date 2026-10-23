/**
 * Высокие порты helios снаружи закрыты, поэтому на защите к сервисам ходят
 * через SSH-туннель на эти же порты localhost:
 *   ssh -N -L 27443:127.0.0.1:27443 -L 27343:127.0.0.1:27343 \
 *          -L 27500:127.0.0.1:27500 -L 27344:127.0.0.1:27344 ifmo
 *
 * Starship отдаётся через балансировщик: узлов два, и какой из них ответит — не важно.
 */
export const SPACE_MARINE_API = 'https://localhost:27443';
export const STARSHIP_API = 'https://localhost:27343/starship';

export const CONSUL_UI = 'http://localhost:27500/ui/';
export const HAPROXY_STATS = 'http://localhost:27344/';
