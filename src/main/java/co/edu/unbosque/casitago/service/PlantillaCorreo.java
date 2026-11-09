package co.edu.unbosque.casitago.service;

/**
 * Plantillas de los correos de CasitaGO (RF-03, RF-33): una versión HTML con la marca de la
 * aplicación y una versión en texto plano para los clientes de correo que no muestran HTML.
 * Usa solo estilos en línea y tablas, que es lo que entienden todos los clientes de correo.
 */
public final class PlantillaCorreo {

    private static final String RECUPERACION_ETIQUETA = "Recuperación de contraseña";
    private static final String RECUPERACION_TITULO = "Recupera tu contraseña";
    private static final String RECUPERACION_INTRODUCCION =
            "Recibimos una solicitud para cambiar la contraseña de tu cuenta. "
                    + "Escribe este código en CasitaGo para continuar.";
    private static final String RECUPERACION_AVISO =
            "Si no fuiste tú quien lo solicitó, ignora este mensaje: tu contraseña no cambiará.";

    private static final String VERIFICACION_ETIQUETA = "Verificación en dos pasos";
    private static final String VERIFICACION_TITULO = "Confirma que eres tú";
    private static final String VERIFICACION_INTRODUCCION =
            "Alguien está iniciando sesión en tu cuenta de CasitaGo. "
                    + "Escribe este código para completar el ingreso.";
    private static final String VERIFICACION_AVISO =
            "Si no fuiste tú quien intentó iniciar sesión, cambia tu contraseña cuanto antes.";

    private static final String HTML = """
            <!DOCTYPE html>
            <html lang="es">
            <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>{{titulo}}</title>
            </head>
            <body style="margin:0;padding:0;background-color:#f2ebf8;">
            <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color:#f2ebf8;padding:24px 12px;">
            <tr><td align="center">
            <table role="presentation" width="560" cellpadding="0" cellspacing="0" style="width:100%;max-width:560px;background-color:#ffffff;border-radius:16px;font-family:Inter,Arial,Helvetica,sans-serif;">
            <tr><td style="background-color:#452166;padding:26px 32px;border-radius:16px 16px 0 0;">
            <span style="display:inline-block;background-color:#6d3aa2;color:#ffffff;font-size:15px;font-weight:800;padding:9px 11px;border-radius:12px 5px 12px 5px;">CG</span>
            <span style="color:#ffffff;font-size:22px;font-weight:800;padding-left:10px;vertical-align:middle;">CasitaGo</span>
            </td></tr>
            <tr><td style="padding:32px;">
            <p style="margin:0 0 8px;color:#6d3aa2;font-size:12px;font-weight:800;letter-spacing:1.4px;text-transform:uppercase;">{{etiqueta}}</p>
            <h1 style="margin:0 0 18px;color:#261333;font-size:24px;line-height:1.3;">{{titulo}}</h1>
            <p style="margin:0 0 12px;color:#261333;font-size:15px;line-height:1.6;">Hola {{nombre}},</p>
            <p style="margin:0 0 24px;color:#261333;font-size:15px;line-height:1.6;">{{introduccion}}</p>
            <table role="presentation" align="center" cellpadding="0" cellspacing="0" style="margin:0 auto 24px;">
            <tr><td style="background-color:#f2ebf8;border:2px dashed #9b72bf;border-radius:14px;padding:18px 30px;color:#452166;font-family:'Courier New',monospace;font-size:34px;font-weight:800;letter-spacing:10px;text-align:center;">{{codigo}}</td></tr>
            </table>
            <p style="margin:0 0 12px;color:#261333;font-size:14px;line-height:1.6;text-align:center;">Este código vence en unos minutos. No lo compartas con nadie.</p>
            <p style="margin:0;padding-top:16px;border-top:1px solid #e6dcef;color:#6b6478;font-size:13px;line-height:1.6;">{{aviso}}</p>
            </td></tr>
            <tr><td style="background-color:#faf8fb;padding:18px 32px;border-radius:0 0 16px 16px;color:#6b6478;font-size:12px;line-height:1.5;text-align:center;">
            &copy; CasitaGo &middot; Este es un mensaje automático, no respondas a este correo.
            </td></tr>
            </table>
            </td></tr>
            </table>
            </body>
            </html>
            """;

    private PlantillaCorreo() {
    }

    public static String htmlRecuperacion(String nombre, String codigo) {
        return construirHtml(RECUPERACION_ETIQUETA, RECUPERACION_TITULO, RECUPERACION_INTRODUCCION,
                RECUPERACION_AVISO, nombre, codigo);
    }

    public static String textoRecuperacion(String nombre, String codigo) {
        return construirTexto(RECUPERACION_INTRODUCCION, RECUPERACION_AVISO, nombre, codigo);
    }

    public static String htmlVerificacion(String nombre, String codigo) {
        return construirHtml(VERIFICACION_ETIQUETA, VERIFICACION_TITULO, VERIFICACION_INTRODUCCION,
                VERIFICACION_AVISO, nombre, codigo);
    }

    public static String textoVerificacion(String nombre, String codigo) {
        return construirTexto(VERIFICACION_INTRODUCCION, VERIFICACION_AVISO, nombre, codigo);
    }

    // El nombre lo escribe el usuario al registrarse: se escapa para que no rompa el HTML del correo.
    static String escapar(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static String construirHtml(String etiqueta, String titulo, String introduccion, String aviso,
                                        String nombre, String codigo) {
        return HTML
                .replace("{{etiqueta}}", etiqueta)
                .replace("{{titulo}}", titulo)
                .replace("{{introduccion}}", introduccion)
                .replace("{{aviso}}", aviso)
                .replace("{{nombre}}", escapar(nombre))
                .replace("{{codigo}}", escapar(codigo));
    }

    private static String construirTexto(String introduccion, String aviso, String nombre, String codigo) {
        return "Hola " + nombre + ",\n\n"
                + introduccion + "\n\n"
                + "Tu código: " + codigo + "\n\n"
                + "Este código vence en unos minutos. No lo compartas con nadie.\n\n"
                + aviso;
    }
}