"""Genera el informe con capturas reales ya existentes; no fabrica pantallas."""
from pathlib import Path
from datetime import datetime
from zoneinfo import ZoneInfo
from xml.etree import ElementTree
import json
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Image, Table, TableStyle, PageBreak, KeepTogether
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER
from reportlab.lib.utils import ImageReader

root = Path(__file__).resolve().parents[1]
out = root / "docs/evidencias"
styles = getSampleStyleSheet()
styles.add(ParagraphStyle(name="TitleEco", fontName="Helvetica-Bold", fontSize=26, leading=31, textColor=colors.HexColor("#146B45"), spaceAfter=15))
styles.add(ParagraphStyle(name="CaptionEco", fontSize=10, leading=14, spaceAfter=12))
styles["BodyText"].leading = 16
styles["BodyText"].spaceAfter = 10

story = []
def text(value, style="BodyText"):
    story.append(Paragraph(value, styles[style]))

def image_fit(filename, width=480, height=600):
    file = out / filename
    w,h = ImageReader(str(file)).getSize()
    scale = min(width/w, height/h)
    return Image(str(file), width=w*scale, height=h*scale)

def pair(title, first, second, caption):
    story.append(PageBreak()); text(title, "TitleEco")
    table = Table([[image_fit(first,228,520), image_fit(second,228,520)]], colWidths=[243,243])
    table.setStyle(TableStyle([("VALIGN",(0,0),(-1,-1),"TOP")]))
    story.append(table); story.append(Spacer(1,12)); text(caption,"CaptionEco")

now = datetime.now(ZoneInfo("America/Lima"))
text("EcoRecicla", "TitleEco")
text("Evidencias de implementacion y verificacion", "Heading1")
text("Generado: " + now.strftime("%d/%m/%Y %H:%M") + " - America/Lima")
text("Backend Spring Boot/Kotlin con PostgreSQL; aplicacion KMP para Android con Compose, Ktor, Koin y SQLite/SQLDelight. Capturas y salidas obtenidas durante esta sesion real de trabajo.")
text("Repositorios privados", "Heading2")
text('<link href="https://github.com/wagner781/ecoapi-backend">github.com/wagner781/ecoapi-backend</link><br/><link href="https://github.com/wagner781/ecorecicla-kmp">github.com/wagner781/ecorecicla-kmp</link>')
data = [["Comprobacion", "Resultado observado"],
        ["Compilacion inicial y final", "JAR Spring Boot y APK Android generados"],
        ["Pruebas Spring/MockMvc", "13 casos, cero fallos"],
        ["Pruebas commonTest", "8 casos en JVM y unitarias Android debug/release"],
        ["API real con PostgreSQL", "20 peticiones HTTP aprobadas"],
        ["Modo avion", "Registro local #-1, pendiente de envio"],
        ["Reinicio y reconexion", "Sustitucion por #17; una sola fila PostgreSQL"],
        ["Sesion compartida", "Token cifrado; logout borra token y cache"]]
table = Table([[Paragraph(cell,styles["CaptionEco"]) for cell in row] for row in data],colWidths=[170,316],hAlign="LEFT")
table.setStyle(TableStyle([("BACKGROUND",(0,0),(-1,0),colors.HexColor("#D9EDDB")),("GRID",(0,0),(-1,-1),0.4,colors.HexColor("#B4C7BB")),("VALIGN",(0,0),(-1,-1),"TOP"),("LEFTPADDING",(0,0),(-1,-1),10),("TOPPADDING",(0,0),(-1,-1),7)]))
story.append(table); story.append(Spacer(1,14))
text("Alcance de las evidencias", "Heading2")
text("Los graficos Git se exportaron antes del commit de evidencias y de la fusion final. El historial empieza en la fecha real de creacion: no acredita trabajo en sesiones anteriores de la unidad. No se ha recibido una solicitud de cambio SC ni se simula su realizacion. Las guias DEFENSA.md explican cada capa y los pasos para repetir la demostracion.")

pair("Inicio de sesion y ruta", "login.png", "ruta.png", "Izquierda: formulario real de autenticacion. Derecha: ruta descargada del servicio y observada desde SQLite; se muestra la fecha y la ultima sincronizacion.")
pair("Modo avion y sincronizacion", "offline.png", "sincronizado.png", "Izquierda: modo avion, sin conexion, registro #-1 pendiente. Derecha: el mismo registro tras reinicio y reconexion automatica, ID #17 y neto 30 kg calculado por el servicio. Se verifico una unica fila tanto en SQLite como en PostgreSQL.")
pair("Validacion y privilegios", "validacion.png", "perfil.png", "Izquierda: tara mayor al bruto, error bajo el campo y sin escritura. Derecha: perfil real del operario con sus privilegios; no posee PESAJE_ANULAR ni RUTA_CERRAR. El servidor tambien los verifica, como muestra la siguiente pagina.")
story.append(PageBreak()); text("Rechazo real del servicio", "TitleEco")
story.append(image_fit("rechazo-403.png",486,440)); story.append(Spacer(1,14))
text("Captura del cliente de verificacion mostrando la respuesta HTTP real a DELETE /api/v1/pesajes/17 con token valido de operario1. Se oculta el token. El resultado 403 prueba que la proteccion esta en el servidor, aunque la peticion se realice fuera de la aplicacion. La fila no se anulo.")
response = json.loads((out/"respuesta-403.json").read_text(encoding="utf-8"))
text("Timestamp devuelto por el servicio: " + response["timestamp"])
text("Las pruebas automatizadas cubren ademas 401 por token ausente, alterado y vencido, 400 por neto invalido, 404 por recurso inexistente y 409 por reglas de ruta. Los errores siguen el contrato comun del enunciado.")

for name,label in [("backend","Backend"),("android","Android/KMP")]:
    story.append(PageBreak()); text("Trazabilidad - " + label,"TitleEco")
    story.append(image_fit("git-"+name+".png",486,550)); story.append(Spacer(1,14))
    text("Exportacion visual del resultado real de git log --graph --oneline --all. Las ramas de funcionalidad parten de develop y regresan mediante solicitudes de incorporacion con merge commit. Los commits usan las fechas reales; no se inventan jornadas, conflictos ni autoria.")

def footer(canvas, doc):
    canvas.setStrokeColor(colors.HexColor("#B4C7BB")); canvas.line(42,37,553,37)
    canvas.setFont("Helvetica",8); canvas.setFillColor(colors.HexColor("#516A5B"))
    canvas.drawString(42,23,"EcoRecicla - Evidencias reales de la sesion")
    canvas.drawRightString(553,23,str(doc.page))

pdf = out / "Evidencias-EcoRecicla.pdf"
SimpleDocTemplate(str(pdf),pagesize=(595.28,841.89),rightMargin=54,leftMargin=54,topMargin=48,bottomMargin=50).build(story,onFirstPage=footer,onLaterPages=footer)
print(pdf)
