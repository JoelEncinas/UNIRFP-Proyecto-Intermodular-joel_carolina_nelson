package com.unir.bikeshare.backend.payments.model;

public enum TransactionType {
	TOP_UP, //Se hace un pago para aumentar el saldo dentro de la app
    RENTAL_PAYMENT //Se hace un pago directo para el alquiler de una bici
}
