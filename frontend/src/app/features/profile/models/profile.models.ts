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
