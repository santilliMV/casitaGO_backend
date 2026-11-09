package co.edu.unbosque.casitago.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlantillaCorreoTest {

    @Test
    void htmlRecuperacion_incluyeCodigoNombreYMarca() {
        String html = PlantillaCorreo.htmlRecuperacion("Ana", "123456");

        assertThat(html).contains("123456", "Hola Ana,", "CasitaGo", "Recupera tu contraseña");
    }

    @Test
    void html_noDejaMarcadoresSinReemplazar() {
        assertThat(PlantillaCorreo.htmlRecuperacion("Ana", "123456")).doesNotContain("{{");
        assertThat(PlantillaCorreo.htmlVerificacion("Ana", "654321")).doesNotContain("{{");
    }

    @Test
    void htmlVerificacion_incluyeCodigoYTitulo() {
        String html = PlantillaCorreo.htmlVerificacion("Ana", "654321");

        assertThat(html).contains("654321", "Confirma que eres tú", "Verificación en dos pasos");
    }

    @Test
    void html_escapaCaracteresEspecialesDelNombre() {
        String html = PlantillaCorreo.htmlVerificacion("<script>alert(1)</script>", "654321");

        assertThat(html).doesNotContain("<script>");
        assertThat(html).contains("&lt;script&gt;alert(1)&lt;/script&gt;");
    }

    @Test
    void html_conNombreNulo_noFalla() {
        String html = PlantillaCorreo.htmlRecuperacion(null, "123456");

        assertThat(html).contains("Hola ,").contains("123456");
    }

    @Test
    void textoRecuperacion_incluyeCodigoYNombreSinHtml() {
        String texto = PlantillaCorreo.textoRecuperacion("Ana", "123456");

        assertThat(texto).contains("Hola Ana,", "Tu código: 123456");
        assertThat(texto).doesNotContain("<");
    }

    @Test
    void textoVerificacion_incluyeCodigoYAviso() {
        String texto = PlantillaCorreo.textoVerificacion("Ana", "654321");

        assertThat(texto).contains("Tu código: 654321", "cambia tu contraseña");
    }

    @Test
    void escapar_reemplazaLosCincoCaracteresEspeciales() {
        assertThat(PlantillaCorreo.escapar("&<>\"'")).isEqualTo("&amp;&lt;&gt;&quot;&#39;");
        assertThat(PlantillaCorreo.escapar(null)).isEmpty();
    }
}