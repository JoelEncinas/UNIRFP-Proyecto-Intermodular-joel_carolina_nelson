export interface ProfileUser {
  id: number;
  username: string;
  email: string;
  role: string;
  balance: number;
  createdAt: string;
}

export interface ProfileUpdateRequest {
  username?: string;
  email?: string;
  password?: string;
}

export interface StripeCheckoutSessionCreateRequest {
  userId: number;
  amount: number;
  transactionType: 'TOP_UP' | 'RENTAL_PAYMENT';
}

export interface StripeCheckoutSessionCancelRequest {
  sessionId: string;
}

export type ProfilePaymentStatus = 'PENDING' | 'SUCCESS' | 'FAILED' | 'CANCELLED';

export interface StripeCheckoutSessionResponse {
  paymentId: number;
  sessionId: string;
  checkoutUrl: string;
  paymentStatus: ProfilePaymentStatus;
}
