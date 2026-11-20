"""Render the lab4 boarding sequence as SVG; convert with rsvg-convert."""
from html import escape
from pathlib import Path

out = Path(__file__).resolve().parents[1] / 'lab4-sequence.svg'
participants = [
    ('Клиент', 'Браузер · React', '#eaf3ff'),
    ('HAProxy', 'Балансировщик', '#eaf3ff'),
    ('starship/web', 'Controller + SOAP-клиент', '#eaf3ff'),
    ('starship/ejb', 'StarshipEndpoint', '#e8e3fa'),
    ('starship/ejb', 'StarshipBean', '#e1f3e9'),
    ('Mule ESB', 'space-marine-proxy', '#fff0d5'),
    ('SpaceMarine', 'web → ejb → repository', '#e1f3e9'),
    ('PostgreSQL', 'Таблицы сервисов', '#edf1f5'),
]
# Sender, receiver, label, response. Time flows downward.
messages = [
    (0, 1, '1. POST /starship/54/board/12', False),
    (1, 2, '2. HTTPS → выбранный узел', False),
    (2, 3, '3. SOAP board(54, 12)', False),
    (3, 4, '4. EJB: board(54, 12)', False),
    (4, 7, '5. Через StarshipRepository: найти корабль и проверить занятость десантника', False),
    (7, 4, '6. Корабль найден; десантник ещё не находится на борту', True),
    (4, 5, '7. SpaceMarineClient → HTTP GET /space-marines/12', False),
    (5, 6, '8. HTTPS GET /space-marines/12', False),
    (6, 7, '9. Найти десантника 12', False),
    (7, 6, '10. Данные десантника', True),
    (6, 5, '11. HTTP 200 + JSON', True),
    (5, 4, '12. HTTP 200 → SpaceMarineClient.exists(12) возвращает true', True),
    (4, 7, '13. Через StarshipRepository: сохранить связь корабль 54 → десантник 12', False),
    (7, 4, '14. Изменения сохранены в транзакции', True),
    (4, 3, '15. StarshipDto', True),
    (3, 2, '16. SOAP-ответ: Ship в XML', True),
    (2, 1, '17. HTTP 200 + JSON', True),
    (1, 0, '18. HTTP 200 + JSON', True),
]
width, height = 2640, 1770
xs = [165 + i*330 for i in range(8)]
svg = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}">',
'<defs><marker id="arrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="8" markerHeight="8" orient="auto-start-reverse"><path d="M 0 0 L 10 5 L 0 10 z" fill="#344d6b"/></marker></defs>',
'<rect width="100%" height="100%" fill="white"/>',
'<g font-family="Arial, sans-serif" fill="#213247">']
def text(x,y,s,size=20,anchor='middle',weight='normal'):
    svg.append(f'<text x="{x}" y="{y}" font-size="{size}" text-anchor="{anchor}" font-weight="{weight}">{escape(s)}</text>')
def box(i,y):
    title,sub,color=participants[i]; x=xs[i]
    svg.append(f'<rect x="{x-150}" y="{y}" width="300" height="82" rx="10" fill="{color}" stroke="#849bb4" stroke-width="1.5"/>')
    text(x,y+32,title,24,weight='bold');text(x,y+61,sub,18)
text(width/2,45,'ЛР4 · Диаграмма последовательности',32,weight='bold')
text(width/2,83,'Посадка десантника 12 на корабль 54 · успешный сценарий · время идёт сверху вниз',22)
for i in range(8):
    box(i,120)
    svg.append(f'<line x1="{xs[i]}" y1="202" x2="{xs[i]}" y2="1470" stroke="#a4b1c0" stroke-width="1.5" stroke-dasharray="7 7"/>')
    box(i,1470)
# Bars show processing / waiting during the nested synchronous call.
for i,start,end in [(1,260,1365),(2,325,1300),(3,390,1235),(4,455,1170),(5,650,975),(6,715,910)]:
    svg.append(f'<rect x="{xs[i]-7}" y="{start}" width="14" height="{end-start}" fill="#dae6f3" stroke="#849bb4"/>')
for n,(a,b,label,response) in enumerate(messages):
    y=260+n*65
    dash=' stroke-dasharray="9 6"' if response else ''
    svg.append(f'<line x1="{xs[a]}" y1="{y}" x2="{xs[b]}" y2="{y}" stroke="#344d6b" stroke-width="2"{dash} marker-end="url(#arrow)"/>')
    middle=(xs[a]+xs[b])/2
    # White backing keeps labels readable where they cross a lifeline.
    length=min(abs(xs[b]-xs[a])-12,len(label)*10.6)
    svg.append(f'<rect x="{middle-length/2}" y="{y-30}" width="{length}" height="25" fill="white"/>')
    text(middle,y-10,label,18 if abs(a-b)==1 else 20)
text(width/2,1603,'Сплошная стрелка — запрос / вызов. Пунктирная — ответ. Узкие полосы — обработка или ожидание ответа.',21)
text(width/2,1641,'REST → SOAP: SoapStarshipClient и CXF формируют XML; StarshipEndpoint передаёт операцию бизнес-компоненту.',21)
text(width/2,1679,'Mule связывает Starship и SpaceMarine. Проверка по HTTP и запись в БД — не распределённая транзакция.',21)
text(width/2,1717,'Показан один выбранный узел Starship. SSH-туннель и Consul опущены. Два блока starship/ejb — классы одного модуля.',21)
svg.append('</g></svg>')
out.write_text('\n'.join(svg))
print(out)
