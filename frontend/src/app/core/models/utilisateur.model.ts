import { Role } from './enums';

export interface Utilisateur {
  id: number;
  nom: string;
  telephone: string;
  role: Role;
  telephoneVerifie: boolean;
}

export interface JwtResponse {
  token: string | null;
  type: string;
  userId: number;
  role: Role;
  otpRequis: boolean;
}
