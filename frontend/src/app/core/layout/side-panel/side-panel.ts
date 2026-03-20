import { NgClass } from '@angular/common';
import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';

import { Auth } from '../../auth/auth';

interface SidePanelLink {
  readonly label: string;
  readonly path: string;
}

@Component({
  selector: 'app-side-panel',
  imports: [NgClass, RouterLink, RouterLinkActive],
  templateUrl: './side-panel.html',
  styleUrl: './side-panel.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SidePanel {
  private readonly auth = inject(Auth);
  private readonly router = inject(Router);

  @Input() mobileOpen = false;
  @Output() panelClose = new EventEmitter<void>();

  readonly links: SidePanelLink[] = [
    { label: 'Mapa', path: '/app/map' },
    { label: 'Historial', path: '/app/history' },
    { label: 'Perfil', path: '/app/profile' },
    { label: 'Terminos y Condiciones', path: '/app/terms' },
  ];

  onNavigate(): void {
    this.panelClose.emit();
  }

  logout(): void {
    this.auth.logout();
    this.panelClose.emit();
    void this.router.navigateByUrl('/auth/login');
  }
}
