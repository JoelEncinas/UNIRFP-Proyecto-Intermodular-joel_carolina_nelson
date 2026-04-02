import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { EditableField, ProfileStore, StripePaymentReturnState } from '../../state/profile.store';

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
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly isLoading = this.profileStore.isLoading;
  readonly isDeleting = this.profileStore.isDeleting;
  readonly isDeleteConfirmationVisible = this.profileStore.isDeleteConfirmationVisible;
  readonly updateInProgress = this.profileStore.updateInProgress;
  readonly topUpInProgress = this.profileStore.topUpInProgress;
  readonly errorMessage = this.profileStore.errorMessage;
  readonly successMessage = this.profileStore.successMessage;
  readonly profile = this.profileStore.profile;
  readonly isBusy = this.profileStore.isBusy;
  readonly profileForm = this.profileStore.profileForm;
  readonly topUpForm = this.profileStore.topUpForm;
  readonly topUpAmounts = this.profileStore.topUpAmounts;
  readonly paymentHistory = this.profileStore.paymentHistory;
  readonly userInitial = this.profileStore.userInitial;

  ngOnInit(): void {
    const paymentQuery = this.route.snapshot.queryParamMap.get('payment');
    const paymentState = this.toStripePaymentReturnState(paymentQuery);

    if (paymentState === 'success') {
      this.profileStore.handleStripeReturn(paymentState);
    } else {
      this.profileStore.loadInitialData();
      this.profileStore.handleStripeReturn(paymentState);
    }

    if (paymentQuery !== null) {
      void this.router.navigate([], {
        relativeTo: this.route,
        queryParams: { payment: null },
        queryParamsHandling: 'merge',
        replaceUrl: true,
      });
    }
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

  selectTopUpAmount(amount: number): void {
    this.profileStore.selectTopUpAmount(amount);
  }

  startStripeTopUp(): void {
    this.profileStore.startStripeTopUp();
  }

  private toStripePaymentReturnState(paymentQuery: string | null): StripePaymentReturnState {
    if (paymentQuery === 'success') {
      return 'success';
    }
    if (paymentQuery === 'cancel') {
      return 'cancel';
    }
    return null;
  }
}
