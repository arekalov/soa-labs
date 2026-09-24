/**
 * Высокие порты helios снаружи закрыты, поэтому на защите к сервисам ходят
 * через SSH-туннель на эти же порты localhost:
 *   ssh -N -L 27443:127.0.0.1:27443 -L 27543:127.0.0.1:27543 ifmo
 */
export const SPACE_MARINE_API = 'https://localhost:27443';
export const STARSHIP_API = 'https://localhost:27543/starship';
