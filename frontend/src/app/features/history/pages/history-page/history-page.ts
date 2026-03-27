import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';

import { BookingStatus, BookingSummary } from '../../../../shared/domain/booking.model';
import { HistoryApi } from '../../data-access/history-api';

const BOOKING_STATUS_ORDER: BookingStatus[] = ['PENDING', 'ACTIVE', 'COMPLETED', 'CANCELLED'];

@Component({
  selector: 'app-history-page',
  imports: [CommonModule],
  templateUrl: './history-page.html',
  styleUrl: './history-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HistoryPage implements OnInit {
  private readonly historyApi = inject(HistoryApi);

  readonly isLoading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly bookings = signal<BookingSummary[]>([]);

  readonly orderedBookings = computed(() =>
    [...this.bookings()].sort(
      (a, b) => this.toTimelinePoint(b) - this.toTimelinePoint(a),
    ),
  );

  readonly statusCounters = computed(() => {
    const entries = this.bookings();
    return BOOKING_STATUS_ORDER.map((status) => ({
      status,
      count: entries.filter((entry) => entry.status === status).length,
    }));
  });

  ngOnInit(): void {
    this.loadHistory();
  }

  displayPickupTime(booking: BookingSummary): string {
    return booking.activatedAt ?? booking.startTime;
  }

  statusLabel(status: BookingStatus): string {
    switch (status) {
      case 'PENDING':
        return 'Pendiente';
      case 'ACTIVE':
        return 'Activa';
      case 'COMPLETED':
        return 'Completada';
      case 'CANCELLED':
        return 'Cancelada';
    }
  }

  statusClass(status: BookingStatus): string {
    switch (status) {
      case 'PENDING':
        return 'border-amber-200 bg-amber-50 text-amber-700';
      case 'ACTIVE':
        return 'border-sky-200 bg-sky-50 text-sky-700';
      case 'COMPLETED':
        return 'border-emerald-200 bg-emerald-50 text-emerald-700';
      case 'CANCELLED':
        return 'border-rose-200 bg-rose-50 text-rose-700';
    }
  }

  private loadHistory(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.historyApi.getMyBookings().subscribe({
      next: (bookings) => {
        this.bookings.set(bookings);
        this.isLoading.set(false);
      },
      error: (error: unknown) => {
        this.errorMessage.set(this.toErrorMessage(error));
        this.isLoading.set(false);
      },
    });
  }

  private toTimelinePoint(booking: BookingSummary): number {
    const candidate = booking.returnedAt ?? booking.activatedAt ?? booking.startTime;
    return new Date(candidate).getTime();
  }

  private toErrorMessage(error: unknown): string {
    if (!(error instanceof HttpErrorResponse)) {
      return 'No se pudo cargar el historial.';
    }

    const apiMessage =
      typeof error.error === 'object' &&
      error.error !== null &&
      'message' in error.error &&
      typeof error.error.message === 'string'
        ? error.error.message
        : null;

    return apiMessage ?? 'No se pudo cargar el historial.';
  }
}
