export interface Formation {
  id: number;
  titre: string;
  description: string | null;
  lieu: string;
  dateFormation: string;
  placesDisponibles: number | null;
  placesRestantes: number | null;
  dateCreation: string;
}

export interface FormationCreate {
  titre: string;
  description?: string;
  lieu: string;
  dateFormation: string;
  placesDisponibles?: number | null;
}

export interface InscriptionFormation {
  id: number;
  formationId: number;
  titreFormation: string;
  dateFormation: string;
  dateInscription: string;
}
