/**
 * Capa Controller (MVC): todos los @RestController del backend, sin importar
 * a qué área del dominio pertenezcan. Reciben la petición HTTP, validan el
 * DTO de entrada (@Valid) y delegan toda lógica de negocio a la capa service.
 * No contienen lógica de negocio.
 *
 * Convención de nombres (para mantener trazabilidad con el diseño orientado a
 * servicios documentado en Confluence, aunque el código no esté separado en
 * carpetas por servicio):
 *   AuthController          -> RF-01 a RF-07, RF-25
 *   ListingController       -> RF-08 a RF-12, RF-24
 *   AvailabilityController  -> RF-13, RF-14
 *   SearchController        -> RF-15, RF-16
 *   BookingController       -> RF-17 a RF-20
 *   PaymentController       -> RF-35
 *   NotificationController  -> RF-21, RF-34
 *   ReviewController        -> RF-22, RF-23
 *   AdminController         -> RF-24 a RF-26
 *   ReportingController     -> RF-27, RF-28
 */
package co.edu.unbosque.casitago.controller;
