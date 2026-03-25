package com.apigatewaypagos.demo.domain.model;

public enum PaymentStatus {
    PENDING,     // El pago se creó pero aún no se envía al banco
    AUTHORIZED,  // El banco retuvo el dinero, pero aún no lo transfiere
    CAPTURED,    // El cobro fue exitoso y el dinero se movió
    DECLINED,    // El banco rechazó la tarjeta (sin fondos, robada, etc.)
    REFUNDED,    // El dinero fue devuelto al cliente
    FAILED       // Hubo un error técnico en el sistema o red
}
