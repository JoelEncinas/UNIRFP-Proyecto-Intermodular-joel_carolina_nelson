import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { SidePanel } from '../side-panel/side-panel';

@Component({
  selector: 'app-app-shell',
  imports: [RouterOutlet, SidePanel],
  templateUrl: './app-shell.html',
  styleUrl: './app-shell.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AppShell {
  readonly isMobileMenuOpen = signal(false);

  toggleMobileMenu(): void {
    this.isMobileMenuOpen.update((current) => !current);
  }

  closeMobileMenu(): void {
    this.isMobileMenuOpen.set(false);
  }
}
