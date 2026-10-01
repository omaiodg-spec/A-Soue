import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { NotificationAdminService } from '../../../core/services/notification.service';
import { NotificationLog } from '../../../core/models/notification.model';

@Component({
  selector: 'app-notifications-admin',
  imports: [DatePipe],
  templateUrl: './notifications-admin.html',
})
export class NotificationsAdmin implements OnInit {
  private readonly notificationService = inject(NotificationAdminService);

  notifications = signal<NotificationLog[]>([]);
  chargement = signal(true);
  erreur = signal<string | null>(null);

  ngOnInit(): void {
    this.notificationService.lister().subscribe({
      next: (data) => {
        this.notifications.set(data.sort((a, b) => b.id - a.id));
        this.chargement.set(false);
      },
      error: () => {
        this.erreur.set('Impossible de charger l\'historique des notifications.');
        this.chargement.set(false);
      },
    });
  }
}
