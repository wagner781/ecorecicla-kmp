"""Genera el informe desde capturas, resultados XML, Git y una respuesta HTTP real.
Requiere reportlab y Pillow. Ejecutar desde la raiz del repositorio con el API activo.
No cambia datos del servidor: la escritura intentada con AUDITOR debe devolver 403.
"""
from pathlib import Path
from datetime import datetime
import json, subprocess, urllib.request, urllib.error, xml.etree.ElementTree as ET
from html import escape
from PIL import Image as PILImage, ImageDraw, ImageFont
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, Image, PageBreak
from reportlab.lib.styles import getSampleStyleSheet
from reportlab.lib import colors
from reportlab.lib.pagesizes import A4

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'docs/evidencias/parte2'
BACK=ROOT.parent/'ecoapi-backend'
OUT.mkdir(parents=True,exist_ok=True)
styles=getSampleStyleSheet()
styles['Normal'].fontSize=10
styles['Normal'].leading=14
styles['Title'].textColor=colors.HexColor('#176348')
story=[]

def p(text,style='Normal'): return Paragraph(text,styles[style])
def image(path,w=245,h=570):
    with PILImage.open(path) as im: iw,ih=im.size
    factor=min(w/iw,h/ih)
    return Image(str(path),iw*factor,ih*factor)
def title(text): story.extend([p(text,'Heading1'),Spacer(1,10)])
def page(text,items):
    story.append(PageBreak()); title(text)
    cells=[]
    for path,caption in items: cells.append([image(ROOT/path),Spacer(1,10),p(caption)])
    t=Table([cells],colWidths=[259]*len(cells)); t.setStyle(TableStyle([('VALIGN',(0,0),(-1,-1),'TOP')]))
    story.append(t)
def git(repo,*args): return subprocess.check_output(['git',*args],cwd=repo,text=True,encoding='utf-8').strip()
def panel(name,lines,width=1400,font_size=21):
    font=ImageFont.truetype('C:/Windows/Fonts/consola.ttf',font_size)
    height=48+(font_size+9)*len(lines)
    im=PILImage.new('RGB',(width,height),'#101b25'); draw=ImageDraw.Draw(im)
    for i,line in enumerate(lines): draw.text((20,20+i*(font_size+9)),line,font=font,fill='#e3efe9')
    im.save(OUT/name)

def tests(path):
    return [{'clase':r.attrib['name'],'pruebas':int(r.attrib['tests']),'fallos':int(r.attrib['failures']),'errores':int(r.attrib['errors'])}
            for f in sorted(path.glob('TEST-*.xml')) for r in [ET.parse(f).getroot()]]
backend=tests(BACK/'build/test-results/test')
jvm=tests(ROOT/'shared/build/test-results/jvmTest')
android=tests(ROOT/'shared/build/test-results/testDebugUnitTest')
assert sum(t['pruebas'] for t in backend)==35
assert sum(t['pruebas'] for t in jvm)==33
assert sum(t['pruebas'] for t in android)==31
assert all(t['fallos']==t['errores']==0 for t in backend+jvm+android)

base='http://localhost:8080/api/v1'
def request(method,path,body=None,token=None):
    req=urllib.request.Request(base+path,method=method,data=None if body is None else json.dumps(body).encode(),headers={'Content-Type':'application/json',**({'Authorization':'Bearer '+token} if token else {})})
    try:
        with urllib.request.urlopen(req) as r: return r.status,json.loads(r.read())
    except urllib.error.HTTPError as e: return e.code,json.loads(e.read())
code,login=request('POST','/auth/login',{'usuario':'auditor1','password':'Eco2026*'})
assert code==200
code,rechazo=request('DELETE','/pesajes/1',{'motivo':'Verificar prohibicion del auditor'},login['token'])
assert code==403
(OUT/'auditor-403.json').write_text(json.dumps(rechazo,indent=2,ensure_ascii=False),encoding='utf-8')
panel('auditor-403.png',['RESPUESTA HTTP REAL | AUDITOR','DELETE /api/v1/pesajes/1','HTTP 403',*json.dumps(rechazo,indent=2,ensure_ascii=False).splitlines()],width=1400)
for repo,name in [(BACK,'backend'),(ROOT,'android')]:
    lines=git(repo,'log','--graph','--oneline','--all','--decorate','--max-count=28').splitlines()
    # Limita ancho visual; la salida completa queda en el TXT contiguo.
    (OUT/f'git-{name}.txt').write_text('\n'.join(lines),encoding='utf-8')
    panel(f'git-{name}.png',[f'GIT REAL: {name} | log --graph --oneline --all',*[l[:104] for l in lines]],width=1450,font_size=21)
metadata={'fechaGeneracion':datetime.now().astimezone().isoformat(),'backend':git(BACK,'rev-parse','HEAD'),'android':git(ROOT,'rev-parse','HEAD'),'pruebasBackend':backend,'pruebasJvm':jvm,'pruebasAndroidDebug':android,'auditorEscrituraHTTP':code,'observacion':'Capturas reales del emulador. Historial de la fecha real, sin acreditar sesiones docentes ni autorias pasadas.'}
(OUT/'verificacion.json').write_text(json.dumps(metadata,ensure_ascii=False,indent=2),encoding='utf-8')

title('EcoRecicla | Parte I corregida y cuatro SC')
story.extend([p('Evidencias de implementacion y verificacion','Title'),Spacer(1,18),p('Generado: '+escape(metadata['fechaGeneracion'])),Spacer(1,12)])
story.append(p('Fuente: Examen Parcial U2, Practica Version B, caso Eco, 19 paginas. Las cuatro SC se implementaron por solicitud del usuario para practicar; no se afirma asignacion docente ni ejecucion dentro de los ochenta minutos de examen.'))
story.append(Spacer(1,12))
rows=[['Cambio','Resultado comprobado'],['SC-A','Buscador + pendientes en caso de uso, desde cache offline.'],['SC-B','AUDITOR con PUNTO_LEER y PESAJE_LEER; escrituras 403.'],['SC-C','Humedad opcional en servidor, SQLite, formulario y detalle.'],['SC-D','COUNT observable; 0 -> 1 offline, reinicio 1, sincronizar 0.']]
t=Table([[p(v) for v in r] for r in rows],colWidths=[65,450]); t.setStyle(TableStyle([('BACKGROUND',(0,0),(-1,0),colors.HexColor('#e2efe9')),('GRID',(0,0),(-1,-1),.4,colors.lightgrey),('VALIGN',(0,0),(-1,-1),'TOP'),('TOPPADDING',(0,0),(-1,-1),8),('BOTTOMPADDING',(0,0),(-1,-1),8)])); story.append(t)
story.extend([Spacer(1,16),p('Verificacion: 35 pruebas Spring; 35 peticiones HTTP reales exportadas. Android: 31 casos comunes y dos migraciones JVM; APK debug compilado y ejecutado en emulador. Las variantes Android ejecutan los mismos casos comunes, no se cuentan como pruebas diferentes.'),Spacer(1,12),p('La humedad usa porcentaje 0..100, hasta dos decimales; vacio significa sin estimar. No altera bruto menos tara. El filtro de pendientes excluye omisiones justificadas, coherente con el cierre de ruta.'),Spacer(1,12),p('Ramas SC desde develop, al menos tres commits reales por rama afectada y PR hacia develop. Parte I marcada v1.1-parte1; entrega actual v1.2.1-unidad2. Se conserva la etiqueta anterior. El historial no acredita trabajo distribuido durante varias fechas de la unidad.')])
page('Parte I | gestion y supervision comprobadas',[(Path('docs/evidencias/parte1/admin-padron.png'),'Administrador: consulta el padron sin ruta propia. Se probaron altas, cambios y bajas administrativas.'),(Path('docs/evidencias/parte1/supervisor-cierre.png'),'Supervisor: ruta QA #4 cerrada despues de dos omisiones. La ruta original #1 permanece abierta.')])
page('SC-A y SC-B | filtro offline y auditor',[(Path('docs/evidencias/parte2/sc-a-filtro-offline.png'),'SC-A: modo avion, buscador Empresa 6 y solo pendientes combinados. La logica vive en FiltrarRutaUseCase.'),(Path('docs/evidencias/parte2/sc-b-auditor.png'),'SC-B: auditor1 entra al padron con acceso de consulta, sin acciones de escritura ni administracion.')])
page('SC-C | validacion y persistencia offline',[(Path('docs/evidencias/parte2/sc-c-validacion.png'),'101 % es rechazado con error de campo. El formulario conserva los datos para corregirlos.'),(Path('docs/evidencias/parte2/sc-c-humedad-offline.png'),'18.25 % se guarda sin conexion con ID provisional -1 y pendiente de sincronizar.')])
page('SC-C y SC-D | envio y conteo local',[(Path('docs/evidencias/parte2/sc-c-humedad-sincronizada.png'),'Tras sincronizar, humedad 18.25 % con ID definitivo. PostgreSQL verifico el registro #25.'),(Path('docs/evidencias/parte2/sc-d-pendiente-1.png'),'SC-D: registrar sin red actualiza la consulta SQL observable y el distintivo indica 1.')])
page('SC-D | restauracion y sincronizacion',[(Path('docs/evidencias/parte2/sc-d-restaurado-1.png'),'Reabrir la app sin red conserva el pendiente y muestra 1 desde SQLite.'),(Path('docs/evidencias/parte2/sc-d-sincronizado-0.png'),'Recuperar red y sincronizar deja 0; PostgreSQL verifico el registro #26 con humedad 7.50 %.')])
story.append(PageBreak()); title('Seguridad | rechazo real y pruebas')
story.extend([p('Esta respuesta se obtuvo con el token valido de auditor1. No se guardan tokens en las evidencias.'),Spacer(1,12),image(OUT/'auditor-403.png',515,260),Spacer(1,16)])
for titulo,datos in [('Backend',backend),('JVM compartido y migraciones',jvm)]:
    story.append(p(titulo,'Heading2'))
    for t in datos: story.append(p(f"{escape(t['clase'].split('.')[-1])}: {t['pruebas']} pruebas, 0 fallos, 0 errores."))
for nombre in ['backend','android']:
    story.append(PageBreak()); title('Historial real | '+nombre)
    story.extend([p('Salida de git log --graph --oneline --all --decorate. El TXT contiguo conserva las lineas completas. Esta imagen representa la salida del comando, no una vista de GitHub.'),Spacer(1,10),image(OUT/f'git-{nombre}.png',520,650)])
story.append(PageBreak()); title('Defensa y acceso de demostracion')
for text in ['SC-A: la UI emite eventos de filtro; el ViewModel usa FiltrarRutaUseCase sobre el Flow de puntos. La ausencia de red no modifica el resultado.', 'SC-B: Rol configura privilegios y los controladores verifican authorities. La app oculta acciones por privilegio y el servidor rechaza llamadas directas. Un 403 conserva la sesion.', 'SC-C: formulario -> FormularioPesaje -> repositorio -> SQLDelight -> cola -> DTO -> servicio -> PostgreSQL. La respuesta vuelve por DTO.dominio() a la base observable. Migracion 2->3 preserva el pendiente anterior.', 'SC-D: cantidadPendientes COUNT(*) -> asFlow().mapToOne() -> caso de uso -> StateFlow -> distintivo. Nunca se incrementa el contador a mano.', 'Usuarios de demostracion: operario1 y operario2 (OPERARIO), supervisor1 (SUPERVISOR), admin (ADMINISTRADOR), auditor1 (AUDITOR). Clave comun: Eco2026*. Solo para entorno de examen local.', 'El administrador gestiona puntos, usuarios, roles y asignaciones; no escribe pesajes. Operario corrige solo pesajes propios del dia. Supervisor puede corregir globalmente, anular y cerrar. AUDITOR solo consulta.', 'Codigo y PR: github.com/wagner781/ecoapi-backend y github.com/wagner781/ecorecicla-kmp. Guias: docs/DEFENSA.md de cada repositorio. Capturas originales y XML de pruebas sustentan este documento.', 'Revision backend: '+metadata['backend'], 'Revision Android antes del commit de estas evidencias: '+metadata['android']]: story.extend([p(escape(text)),Spacer(1,12)])

def pie(c,doc):
    c.setFont('Helvetica',8); c.setFillColor(colors.grey)
    c.drawString(36,20,'EcoRecicla | Evidencias reales de la sesion | 2026-10-28')
    c.drawRightString(A4[0]-36,20,str(doc.page))
archivo=ROOT/'docs/evidencias/Evidencias-EcoRecicla-Parte-I-y-II.pdf'
SimpleDocTemplate(str(archivo),pagesize=A4,rightMargin=36,leftMargin=36,topMargin=30,bottomMargin=36).build(story,onFirstPage=pie,onLaterPages=pie)
print(archivo)
