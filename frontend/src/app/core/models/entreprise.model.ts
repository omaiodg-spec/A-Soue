import { TypeDechet } from './enums';

export interface Entreprise {
  id: number;
  raisonSociale: string;
  telephone: string;
  latitude: number | null;
  longitude: number | null;
  adresse: string | null;
  typesDechetGeres: TypeDechet[];
  estAssoue: boolean;
}

export interface EntrepriseCreate {
  raisonSociale: string;
  telephone: string;
  motDePasse: string;
  latitude?: number | null;
  longitude?: number | null;
  adresse?: string | null;
  typesDechetGeres: TypeDechet[];
  estAssoue: boolean;
}
