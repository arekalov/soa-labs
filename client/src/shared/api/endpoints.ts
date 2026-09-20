/**
 * Высокие порты helios снаружи закрыты, поэтому на защите к сервисам ходят
 * через SSH-туннель на эти же порты localhost:
 *   ssh -N -L 24443:127.0.0.1:24443 -L 24543:127.0.0.1:24543 ifmo
 */
export const SPACE_MARINE_API = 'https://localhost:24443';
export const STARSHIP_API = 'https://localhost:24543/starship';
