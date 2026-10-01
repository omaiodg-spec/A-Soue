import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../api-config';
import { NotificationLog } from '../models/notification.model';

@Injectable({ providedIn: 'root' })
export class NotificationAdminService {
  private http = inject(HttpClient);
  private apiUrl = `${API_BASE_URL}/notifications`;

  lister(): Observable<NotificationLog[]> {
    return this.http.get<NotificationLog[]>(this.apiUrl);
  }
}
