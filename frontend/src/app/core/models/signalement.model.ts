import { StatutSignalement, TypeDechet } from './enums';

export interface Signalement {
  id: number;
  utilisateurId: number;
  typeDechet: TypeDechet;
  photoUrl: string;
  latitude: number;
  longitude: number;
  adresse: string | null;
  numeroSuivi: string;
  statut: StatutSignalement;
  entrepriseId: number | null;
  dateCreation: string;
}

export interface SignalementMarketplace {
  id: number;
  typeDechet: TypeDechet;
  photoUrl: string;
  latitude: number;
  longitude: number;
  adresse: string | null;
  numeroSuivi: string;
  statut: StatutSignalement;
  distanceKm: number;
  dateCreation: string;
}

export interface SignalementCreate {
  typeDechet: TypeDechet;
  photoUrl: string;
  latitude: number;
  longitude: number;
}
