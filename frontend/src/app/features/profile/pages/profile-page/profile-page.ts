import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';

import { EditableField, ProfileStore } from '../../state/profile.store';

@Component({
  selector: 'app-profile-page',
  imports: [CommonModule, ReactiveFormsModule],
  providers: [ProfileStore],
  templateUrl: './profile-page.html',
  styleUrl: './profile-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProfilePage implements OnInit {
  private readonly profileStore = inject(ProfileStore);

  readonly isLoading = this.profileStore.isLoading;
  readonly isDeleting = this.profileStore.isDeleting;
  readonly isDeleteConfirmationVisible = this.profileStore.isDeleteConfirmationVisible;
  readonly updateInProgress = this.profileStore.updateInProgress;
  readonly errorMessage = this.profileStore.errorMessage;
  readonly successMessage = this.profileStore.successMessage;
  readonly profile = this.profileStore.profile;
  readonly isBusy = this.profileStore.isBusy;
  readonly profileForm = this.profileStore.profileForm;
  readonly userInitial = this.profileStore.userInitial;

  ngOnInit(): void {
    this.profileStore.loadInitialData();
  }

  updateField(field: EditableField): void {
    this.profileStore.updateField(field);
  }

  updateAll(): void {
    this.profileStore.updateAll();
  }

  showDeleteConfirmation(): void {
    this.profileStore.showDeleteConfirmation();
  }

  cancelDeleteConfirmation(): void {
    this.profileStore.cancelDeleteConfirmation();
  }

  confirmDeleteAccount(): void {
    this.profileStore.confirmDeleteAccount();
  }
}
