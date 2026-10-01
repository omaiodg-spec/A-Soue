/**
 * Base de l'API Spring Boot.
 *
 * Toutes les requetes passent par le prefixe /api sur la MEME origine que le
 * frontend -- pas d'URL absolue codee en dur, donc :
 *   - aucun probleme de CORS (meme origine pour le navigateur) ;
 *   - l'application marche telle quelle sur localhost, sur une IP LAN ou
 *     derriere un nom de domaine, sans recompiler.
 *
 * Qui fait la traduction /api/... -> backend:8080/... ?
 *   - en Docker      : Nginx (frontend/nginx.conf) ;
 *   - en dev local   : le dev-server Angular (frontend/proxy.conf.json),
 *                      actif automatiquement avec "npm start".
 */
export const API_BASE_URL = '/api';
